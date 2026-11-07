package br.karina.pet;

import android.content.SharedPreferences;
import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Busca conhecimento apenas em fontes públicas explicitamente autorizadas. */
public class Conhecimento {
    public static final String[] FEEDS = {
            "https://feeds.bbci.co.uk/portuguese/rss.xml",
            "https://g1.globo.com/rss/g1/"
    };

    private static final int MAX_RESPOSTA_BYTES = 1024 * 1024;
    private static final Set<String> HOSTS_PERMITIDOS = new HashSet<>(Arrays.asList(
            "feeds.bbci.co.uk",
            "g1.globo.com",
            "geocoding-api.open-meteo.com",
            "api.open-meteo.com",
            "pt.wikipedia.org"
    ));

    /** Impede HTTP, hosts arbitrários, portas alternativas e redirecionamentos. */
    private static InputStream abrir(String endereco) throws Exception {
        if (endereco == null || endereco.length() > 2048) {
            throw new SecurityException("Endereço inválido");
        }
        URL url = new URL(endereco);
        String host = url.getHost() == null ? "" : url.getHost().toLowerCase(java.util.Locale.ROOT);
        if (!"https".equalsIgnoreCase(url.getProtocol())
                || !HOSTS_PERMITIDOS.contains(host)
                || url.getUserInfo() != null
                || (url.getPort() != -1 && url.getPort() != 443)) {
            throw new SecurityException("Destino de rede não permitido");
        }

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setInstanceFollowRedirects(false);
        con.setConnectTimeout(5000);
        con.setReadTimeout(8000);
        con.setUseCaches(false);
        con.setRequestProperty("User-Agent", "Puppy/0.2 (Android; consultas públicas)");
        con.setRequestProperty("Accept", "application/json, application/rss+xml, application/xml, text/xml");
        int status = con.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            con.disconnect();
            throw new IOException("Consulta recusada");
        }
        long tamanho = con.getContentLengthLong();
        if (tamanho > MAX_RESPOSTA_BYTES) {
            con.disconnect();
            throw new IOException("Resposta muito grande");
        }
        return new LimiteInputStream(con.getInputStream(), MAX_RESPOSTA_BYTES);
    }

    /** Limita também respostas sem Content-Length (transferência em blocos). */
    private static class LimiteInputStream extends FilterInputStream {
        private long restante;
        LimiteInputStream(InputStream in, long limite) {
            super(in);
            restante = limite;
        }
        @Override public int read() throws IOException {
            if (restante <= 0) throw new IOException("Limite de resposta excedido");
            int valor = super.read();
            if (valor != -1) restante--;
            return valor;
        }
        @Override public int read(byte[] b, int off, int len) throws IOException {
            if (restante <= 0) throw new IOException("Limite de resposta excedido");
            int lido = super.read(b, off, (int) Math.min(len, restante));
            if (lido > 0) restante -= lido;
            return lido;
        }
    }

    static String texto(String u) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(abrir(u), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = br.readLine()) != null) sb.append(l).append('\n');
            return sb.toString();
        }
    }

    /** Pesquisa um assunto na Wikipédia (CC BY-SA). */
    public static String[] wikipedia(String termo) throws Exception {
        String q = URLEncoder.encode(termo, "UTF-8");
        JSONArray busca = new JSONArray(texto(
                "https://pt.wikipedia.org/w/api.php?action=opensearch&limit=1&format=json&search=" + q));
        JSONArray titulos = busca.getJSONArray(1);
        if (titulos.length() == 0) return null;
        String titulo = titulos.getString(0);
        String t = URLEncoder.encode(titulo.replace(' ', '_'), "UTF-8");
        JSONObject r = new JSONObject(texto("https://pt.wikipedia.org/api/rest_v1/page/summary/" + t));
        String url = "";
        JSONObject cu = r.optJSONObject("content_urls");
        if (cu != null && cu.optJSONObject("desktop") != null) {
            url = cu.getJSONObject("desktop").optString("page", "");
        }
        return new String[]{r.optString("title", titulo), r.optString("extract", ""), url};
    }

    /** Lê até n itens de um feed previamente autorizado. */
    public static List<String[]> manchetes(String feed, int n) throws Exception {
        if (n < 1 || n > 10) throw new IllegalArgumentException("Quantidade inválida");
        List<String[]> out = new ArrayList<>();
        try (InputStream in = abrir(feed)) {
            XmlPullParser xp = Xml.newPullParser();
            xp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            xp.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
            xp.setInput(in, null);
            boolean dentro = false;
            String tag = "", titulo = "", link = "";
            int ev = xp.getEventType();
            while (ev != XmlPullParser.END_DOCUMENT && out.size() < n) {
                if (ev == XmlPullParser.START_TAG) {
                    tag = xp.getName();
                    if ("item".equals(tag)) {
                        dentro = true;
                        titulo = "";
                        link = "";
                    }
                } else if (ev == XmlPullParser.TEXT && dentro) {
                    if ("title".equals(tag) && titulo.length() < 500) titulo += xp.getText();
                    else if ("link".equals(tag) && link.length() < 1000) link += xp.getText();
                } else if (ev == XmlPullParser.END_TAG) {
                    if ("item".equals(xp.getName())) {
                        dentro = false;
                        if (!titulo.trim().isEmpty()) out.add(new String[]{titulo.trim(), link.trim()});
                    }
                    tag = "";
                }
                ev = xp.next();
            }
        }
        return out;
    }

    /** Guarda um item no celular (sem repetir) e mantém no máximo 300. */
    public static void guardar(SharedPreferences sp, String fonte, String titulo, String texto, String url) {
        try {
            JSONArray arr = new JSONArray(sp.getString("itens", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (titulo.equals(o.optString("titulo")) && fonte.equals(o.optString("fonte"))) return;
            }
            JSONObject o = new JSONObject();
            o.put("fonte", fonte);
            o.put("titulo", titulo);
            o.put("texto", texto);
            o.put("url", url);
            o.put("quando", System.currentTimeMillis());
            JSONArray novo = new JSONArray();
            novo.put(o);
            for (int i = 0; i < arr.length() && novo.length() < 300; i++) novo.put(arr.get(i));
            sp.edit().putString("itens", novo.toString()).apply();
        } catch (Exception ignored) {
            // Se o conteúdo recebido estiver inválido, não interrompe o app.
        }
    }
}

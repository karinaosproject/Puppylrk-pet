package br.karina.pet;

import android.content.SharedPreferences;
import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/** Busca conhecimento em fontes abertas e guarda no celular. */
public class Conhecimento {
    // Troque ou acrescente feeds aqui. Se algum parar de funcionar, o app avisa na tela.
    public static final String[] FEEDS = {
            "https://feeds.bbci.co.uk/portuguese/rss.xml",
            "https://g1.globo.com/rss/g1/"
    };

    private static InputStream abrir(String u) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(u).openConnection();
        con.setConnectTimeout(10000);
        con.setReadTimeout(15000);
        con.setRequestProperty("User-Agent", "Puppy/0.1 (app pessoal Android)");
        if (con.getResponseCode() != 200) throw new Exception("HTTP " + con.getResponseCode());
        return con.getInputStream();
    }

     static String texto(String u) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(abrir(u), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = br.readLine()) != null) sb.append(l).append('\n');
            return sb.toString();
        }
    }

    /** Pesquisa um assunto na Wikipédia (CC BY-SA). Devolve {titulo, texto, url} ou null. */
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

    /** Lê um feed RSS e devolve até n itens {titulo, link}. */
    public static List<String[]> manchetes(String feed, int n) throws Exception {
        List<String[]> out = new ArrayList<>();
        try (InputStream in = abrir(feed)) {
            XmlPullParser xp = Xml.newPullParser();
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
                    if ("title".equals(tag)) titulo += xp.getText();
                    else if ("link".equals(tag)) link += xp.getText();
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
            // se der erro ao guardar, só não guarda
        }
    }
}

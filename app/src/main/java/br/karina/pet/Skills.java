package br.karina.pet;

import android.content.SharedPreferences;

import java.net.URL;
import java.text.Normalizer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Habilidades do Puppy: entende frases simples por regras (sem IA pesada). */
public class Skills {
    public static final String CIDADE_PADRAO = "Taubaté";

    public static class Resposta {
        public String texto = "";       // o que aparece na tela
        public String fala = null;      // frase curta do Puppy (balão)
        public String aprendeu = null;  // assunto aprendido
        public String diario = null;    // linha para o diário
    }

    static String norm(String s) {
        String n = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}+", "")
                .replaceAll("[?!.,;:]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static Resposta executar(String cmd, SharedPreferences sk) {
        Resposta r = new Resposta();
        String n = norm(cmd);
        try {
            if (n.isEmpty()) {
                r.texto = "Diga alguma coisa! Tente: \"ajuda\".";
            } else if (temAlgum(n, "ajuda", "o que voce faz", "comandos")) {
                r.texto = "Eu entendo:\n• clima  /  clima em Santos\n• notícias\n"
                        + "• aprender sobre <assunto>\n• o que é <assunto>\n• diário";
            } else if (clima(n)) {
                String cidade = cidadeDe(n, sk.getString("cidade", CIDADE_PADRAO));
                String[] c = Clima.consultar(cidade);
                if (c == null) {
                    r.texto = "Não achei a cidade \"" + cidade + "\".";
                    r.fala = "Cidade não achada";
                } else {
                    sk.edit().putString("cidade", c[2]).apply();
                    r.texto = c[0];
                    r.fala = c[1];
                    r.diario = "Consultei o clima: " + c[1];
                }
            } else if (temAlgum(n, "noticia", "manchete")) {
                StringBuilder sb = new StringBuilder();
                int novos = 0;
                for (String feed : Conhecimento.FEEDS) {
                    try {
                        String fonte = new URL(feed).getHost();
                        List<String[]> itens = Conhecimento.manchetes(feed, 5);
                        for (String[] it : itens) {
                            Conhecimento.guardar(sk, fonte, it[0], "", it[1]);
                            sb.append("• ").append(it[0]).append("\n");
                            novos++;
                        }
                    } catch (Exception e) {
                        sb.append("(não consegui ler: ").append(feed).append(")\n");
                    }
                }
                r.texto = sb.length() == 0 ? "Nada encontrado." : sb.toString();
                if (novos > 0) {
                    r.aprendeu = "as notícias de hoje";
                    r.diario = "Li " + novos + " manchetes";
                }
            } else {
                String termo = assunto(n);
                if (termo == null) {
                    r.texto = "Ainda não sei fazer isso. Digite \"ajuda\" para ver o que eu entendo.";
                    r.fala = "Não entendi...";
                    r.diario = "Não entendi o comando: " + cmd;
                } else {
                    String[] w = Conhecimento.wikipedia(termo);
                    if (w == null || w[1].isEmpty()) {
                        r.texto = "Não encontrei nada sobre \"" + termo + "\".";
                        r.fala = "Hmm, não achei";
                        r.diario = "Procurei \"" + termo + "\" e não achei";
                    } else {
                        Conhecimento.guardar(sk, "Wikipédia", w[0], w[1], w[2]);
                        r.texto = w[0] + ": " + w[1] + "\n(Fonte: Wikipédia, CC BY-SA)";
                        r.aprendeu = w[0];
                        r.diario = "Aprendi sobre " + w[0];
                    }
                }
            }
        } catch (Exception e) {
            r.texto = "Sem conexão ou erro: " + e.getMessage();
            r.fala = "Sem conexão...";
        }
        return r;
    }

    private static boolean temAlgum(String n, String... termos) {
        for (String t : termos) if (n.contains(t)) return true;
        return false;
    }

    private static boolean clima(String n) {
        return temAlgum(n, "clima", "previsao", "temperatura", "chover", "chuva")
                || n.contains("como esta o tempo") || n.startsWith("tempo ") || n.equals("tempo");
    }

    private static String cidadeDe(String n, String padrao) {
        Matcher m = Pattern.compile("(?:^| )(?:em|para|de|no|na) (.+)$").matcher(n);
        if (m.find()) {
            String c = m.group(1).replaceAll("( hoje| agora| amanha)$", "").trim();
            if (!c.isEmpty() && !c.equals("hoje") && !c.equals("agora") && !c.equals("amanha")) return c;
        }
        return padrao;
    }

    private static String assunto(String n) {
        String[] prefixos = {"aprender sobre ", "aprenda sobre ", "me fale sobre ", "fale sobre ",
                "pesquisar ", "pesquise ", "pesquisa ", "o que e ", "o que sao ", "quem e ", "quem foi "};
        for (String p : prefixos) {
            if (n.startsWith(p) && n.length() > p.length()) return n.substring(p.length()).trim();
        }
        return null;
    }
}
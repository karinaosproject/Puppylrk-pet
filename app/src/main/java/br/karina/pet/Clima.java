package br.karina.pet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;

/** Clima via Open-Meteo (gratuito, sem chave). */
public class Clima {
    static String descricao(int c) {
        if (c == 0) return "céu limpo";
        if (c == 1) return "quase sem nuvens";
        if (c == 2) return "parcialmente nublado";
        if (c == 3) return "nublado";
        if (c == 45 || c == 48) return "neblina";
        if (c >= 51 && c <= 57) return "garoa";
        if (c >= 61 && c <= 67) return "chuva";
        if (c >= 71 && c <= 77) return "neve";
        if (c >= 80 && c <= 82) return "pancadas de chuva";
        if (c >= 95) return "trovoada";
        return "tempo instável";
    }

    /** Devolve {texto, resumo curto, nome da cidade} ou null se a cidade não for encontrada. */
    public static String[] consultar(String cidade) throws Exception {
        JSONObject g = new JSONObject(Conhecimento.texto(
                "https://geocoding-api.open-meteo.com/v1/search?count=1&language=pt&format=json&name="
                        + URLEncoder.encode(cidade, "UTF-8")));
        JSONArray rs = g.optJSONArray("results");
        if (rs == null || rs.length() == 0) return null;
        JSONObject loc = rs.getJSONObject(0);
        String nome = loc.optString("name", cidade);
        double lat = loc.getDouble("latitude");
        double lon = loc.getDouble("longitude");

        JSONObject f = new JSONObject(Conhecimento.texto(
                "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                        + "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m"
                        + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                        + "&forecast_days=1&timezone=auto"));
        JSONObject cur = f.getJSONObject("current");
        JSONObject dia = f.getJSONObject("daily");
        long temp = Math.round(cur.getDouble("temperature_2m"));
        long sens = Math.round(cur.getDouble("apparent_temperature"));
        long vento = Math.round(cur.getDouble("wind_speed_10m"));
        String desc = descricao(cur.getInt("weather_code"));
        long max = Math.round(dia.getJSONArray("temperature_2m_max").getDouble(0));
        long min = Math.round(dia.getJSONArray("temperature_2m_min").getDouble(0));
        int chuva = dia.getJSONArray("precipitation_probability_max").optInt(0, 0);

        String texto = nome + ": " + temp + "°C (sensação " + sens + "°C), " + desc + ".\n"
                + "Hoje: mín " + min + "°, máx " + max + "°, chance de chuva " + chuva
                + "%. Vento " + vento + " km/h.";
        return new String[]{texto, nome + " " + temp + "°C, " + desc, nome};
    }
}
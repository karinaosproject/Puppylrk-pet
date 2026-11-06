package br.karina.pet;

import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Diário do Puppy: registra o que ele faz e aprende. */
public class Diario {
    private static final int MAX = 200;

    public static void registrar(SharedPreferences sp, String texto) {
        if (texto == null || texto.trim().isEmpty()) return;
        String hora = new SimpleDateFormat("dd/MM HH:mm", new Locale("pt", "BR")).format(new Date());
        String linha = hora + " — " + texto.replace('\n', ' ');
        String atual = sp.getString("linhas", "");
        String[] a = atual.isEmpty() ? new String[0] : atual.split("\n");
        StringBuilder sb = new StringBuilder();
        int ini = Math.max(0, a.length - (MAX - 1));
        for (int i = ini; i < a.length; i++) sb.append(a[i]).append('\n');
        sb.append(linha);
        sp.edit().putString("linhas", sb.toString()).apply();
    }

    /** Texto do diário, do mais novo para o mais antigo. */
    public static String ler(SharedPreferences sp) {
        String atual = sp.getString("linhas", "");
        if (atual.isEmpty()) return "O diário ainda está vazio.";
        String[] a = atual.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = a.length - 1; i >= 0; i--) sb.append("• ").append(a[i]).append('\n');
        return sb.toString();
    }
}
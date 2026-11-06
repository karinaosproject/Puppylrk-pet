package br.karina.pet;

import android.content.SharedPreferences;
import android.graphics.Color;

/** Estado do pet: necessidades, emoções, decisões e persistência. */
public class Pet {
    public static final int PASSEAR = 0, BRINCAR = 1, TRAVESSURA = 2, DORMIR = 3, PEDIR = 4;

    public final long seed;
    public final Brain brain;
    public float fome = 0.3f, energia = 0.8f, alegria = 0.5f, raiva = 0f, vinculo = 0.1f;
    public int action = PASSEAR;
    public String fala = "Oi, eu sou o Puppy!";

    private final float[] stim = new float[Brain.IN];
    private int ticksSinceTravessura = 999;
    private int actionTicks = 0;

    public Pet(long seed) {
        this.seed = seed;
        this.brain = new Brain(seed);
    }

    static float c(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    public int cor() {
        float hue = (float) (((seed % 360) + 360) % 360);
        return Color.HSVToColor(new float[]{hue, 0.55f, 0.95f});
    }

    /** Um passo de tempo (~10x por segundo). */
    public void tick() {
        fome = c(fome + 0.0006f);
        if (action == DORMIR) energia = c(energia + 0.004f);
        else energia = c(energia - 0.0004f);
        alegria = c(alegria + (0.5f - alegria) * 0.002f - (fome > 0.7f ? 0.0008f : 0f));
        raiva *= 0.995f;
        for (int i = 0; i < 4; i++) stim[i] *= 0.85f;
        stim[4] = fome;
        stim[5] = 1f - energia;
        brain.step(stim);
        ticksSinceTravessura++;
        decide();
    }

    private void decide() {
        if (actionTicks > 0) {
            actionTicks--;
            return;
        }
        float[] s = new float[5];
        s[PEDIR] = fome > 0.65f ? fome : 0f;
        s[DORMIR] = energia < 0.25f ? 1f - energia : 0f;
        s[TRAVESSURA] = brain.rate[2] * 3f * (0.3f + brain.rebeldia) * energia;
        s[BRINCAR] = (alegria * 0.6f + brain.rate[0] * 2f) * energia;
        s[PASSEAR] = 0.3f;
        int best = PASSEAR;
        float bv = -1f;
        for (int i = 0; i < s.length; i++) {
            float v = s[i] + (float) Math.random() * 0.15f;
            if (v > bv) {
                bv = v;
                best = i;
            }
        }
        action = best;
        actionTicks = 20 + (int) (Math.random() * 30);
        switch (best) {
            case TRAVESSURA: ticksSinceTravessura = 0; fala = "He he he!"; break;
            case BRINCAR: fala = "Vamos brincar!"; break;
            case DORMIR: fala = "Zzz..."; break;
            case PEDIR: fala = "Tô com fome!"; break;
            default: fala = "..."; break;
        }
    }

    // ----- eventos do usuário -----

    public void carinho() {
        stim[0] = 1f;
        alegria = c(alegria + 0.25f);
        vinculo = c(vinculo + 0.02f);
        raiva *= 0.5f;
        fala = "Gostei!";
        consequencia(+1f);
    }

    public void comer() {
        stim[1] = 1f;
        fome = c(fome - 0.5f);
        alegria = c(alegria + 0.1f);
        fala = "Nham!";
        consequencia(+0.3f);
    }

    public void brincar() {
        stim[2] = 1f;
        alegria = c(alegria + 0.2f);
        energia = c(energia - 0.1f);
        vinculo = c(vinculo + 0.01f);
        action = BRINCAR;
        actionTicks = 40;
        fala = "Eba!";
        consequencia(+0.7f);
    }

    public void bronca() {
        stim[3] = 1f;
        raiva = c(raiva + 0.4f);
        alegria = c(alegria - 0.3f);
        vinculo = c(vinculo - 0.03f);
        fala = "Buá...";
        consequencia(-1f);
    }

    /** Só ensina se a reação veio logo depois de uma travessura (cerca de 5 segundos). */
    private void consequencia(float r) {
        if (ticksSinceTravessura < 50) {
            brain.reward(r);
            brain.rebeldia = c(brain.rebeldia + (r < 0 ? -0.05f : 0.02f));
        }
    }

    /** Aplica o tempo que passou com o app fechado (bem mais devagar do que com o app aberto). */
    public void passarTempo(long ms) {
        if (ms <= 0L) return;
        float horas = Math.min(ms, 48L * 3600000L) / 3600000f;
        fome = c(fome + 0.08f * horas);
        energia = c(energia + 0.15f * horas);
        raiva = c(raiva - 1.0f * horas);
        if (fome > 0.7f) alegria = c(alegria - 0.05f * horas);
        alegria = c(alegria + (0.5f - alegria) * Math.min(1f, 0.2f * horas));
        vinculo = c(vinculo - 0.003f * horas);
        if (fome > 0.7f) fala = "Tô com fome!";
        else if (horas >= 1f) fala = "Que saudade!";
    }

    // ----- persistência -----

    public void save(SharedPreferences sp) {
        SharedPreferences.Editor e = sp.edit();
        e.putLong("seed", seed);
        e.putLong("ultimaVez", System.currentTimeMillis());
        e.putString("estado", fome + "," + energia + "," + alegria + "," + raiva + "," + vinculo
                + "," + brain.curiosidade + "," + brain.apego + "," + brain.rebeldia);
        e.putString("w1", join(brain.w1));
        e.putString("w2", join(brain.w2));
        e.apply();
    }

    public static Pet load(SharedPreferences sp) {
        long seed = sp.getLong("seed", System.currentTimeMillis() % 100000L);
        Pet p = new Pet(seed);
        try {
            String es = sp.getString("estado", null);
            if (es != null) {
                String[] a = es.split(",");
                p.fome = Float.parseFloat(a[0]);
                p.energia = Float.parseFloat(a[1]);
                p.alegria = Float.parseFloat(a[2]);
                p.raiva = Float.parseFloat(a[3]);
                p.vinculo = Float.parseFloat(a[4]);
                p.brain.curiosidade = Float.parseFloat(a[5]);
                p.brain.apego = Float.parseFloat(a[6]);
                p.brain.rebeldia = Float.parseFloat(a[7]);
            }
            fill(p.brain.w1, sp.getString("w1", null));
            fill(p.brain.w2, sp.getString("w2", null));
            long antes = sp.getLong("ultimaVez", 0L);
            if (antes > 0L) p.passarTempo(System.currentTimeMillis() - antes);
        } catch (Exception ignored) {
            // se algo estiver corrompido, o pet começa do zero
        }
        return p;
    }

    private static String join(float[][] m) {
        StringBuilder sb = new StringBuilder();
        for (float[] row : m)
            for (float v : row) {
                if (sb.length() > 0) sb.append(',');
                sb.append(v);
            }
        return sb.toString();
    }

    private static void fill(float[][] m, String s) {
        if (s == null) return;
        String[] a = s.split(",");
        if (a.length != m.length * m[0].length) return;
        int k = 0;
        for (float[] row : m)
            for (int j = 0; j < row.length; j++) row[j] = Float.parseFloat(a[k++]);
    }
}

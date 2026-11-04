package br.karina.pet;

import java.util.Random;

/** Rede neural pulsada simples (neurônios "integra e dispara") com aprendizado por recompensa. */
public class Brain {
    public static final int IN = 6, HID = 12, OUT = 4;
    // Entradas: 0 carinho, 1 comida, 2 brincadeira, 3 bronca, 4 fome, 5 cansaço
    // Saídas:   0 alegria, 1 tristeza, 2 travessura, 3 sono

    public final float[][] w1 = new float[IN][HID];
    public final float[][] w2 = new float[HID][OUT];
    private final float[][] e1 = new float[IN][HID];   // rastros de elegibilidade
    private final float[][] e2 = new float[HID][OUT];
    private final float[] vH = new float[HID];
    private final float[] vO = new float[OUT];
    private final float[] preIn = new float[IN];
    private final float[] hTrace = new float[HID];
    private final boolean[] sH = new boolean[HID];
    private final boolean[] sO = new boolean[OUT];
    public final float[] rate = new float[OUT];        // taxa de disparo de cada saída

    // Personalidade (muda com as experiências)
    public float curiosidade, apego, rebeldia;

    private final Random rnd;

    public Brain(long seed) {
        rnd = new Random(seed);
        for (int i = 0; i < IN; i++)
            for (int j = 0; j < HID; j++) w1[i][j] = 0.2f + rnd.nextFloat() * 0.6f;
        for (int j = 0; j < HID; j++)
            for (int k = 0; k < OUT; k++) w2[j][k] = 0.2f + rnd.nextFloat() * 0.6f;
        curiosidade = rnd.nextFloat();
        apego = rnd.nextFloat();
        rebeldia = rnd.nextFloat();
    }

    /** Um passo de tempo (chamado ~10x por segundo). */
    public void step(float[] in) {
        for (int i = 0; i < IN; i++) preIn[i] = preIn[i] * 0.9f + in[i];

        for (int j = 0; j < HID; j++) {
            float cur = 0f;
            for (int i = 0; i < IN; i++) cur += in[i] * w1[i][j];
            cur += (rnd.nextFloat() - 0.5f) * 0.1f * (0.5f + curiosidade); // ruído espontâneo
            vH[j] = vH[j] * 0.85f + cur;
            sH[j] = vH[j] > 1f;
            if (sH[j]) vH[j] = 0f;
            hTrace[j] = hTrace[j] * 0.9f + (sH[j] ? 1f : 0f);
        }

        for (int k = 0; k < OUT; k++) {
            float cur = 0f;
            for (int j = 0; j < HID; j++) if (sH[j]) cur += w2[j][k];
            vO[k] = vO[k] * 0.85f + cur * 0.5f;
            sO[k] = vO[k] > 1f;
            if (sO[k]) vO[k] = 0f;
            rate[k] = rate[k] * 0.97f + (sO[k] ? 0.03f : 0f);
        }

        for (int i = 0; i < IN; i++)
            for (int j = 0; j < HID; j++)
                e1[i][j] = e1[i][j] * 0.95f + preIn[i] * (sH[j] ? 1f : 0f) * 0.1f;
        for (int j = 0; j < HID; j++)
            for (int k = 0; k < OUT; k++)
                e2[j][k] = e2[j][k] * 0.95f + hTrace[j] * (sO[k] ? 1f : 0f) * 0.1f;
    }

    /** Recompensa (>0, carinho) ou punição (<0, bronca) sobre as conexões que estavam ativas. */
    public void reward(float r) {
        for (int i = 0; i < IN; i++)
            for (int j = 0; j < HID; j++) w1[i][j] = clamp(w1[i][j] + 0.1f * r * e1[i][j]);
        for (int j = 0; j < HID; j++)
            for (int k = 0; k < OUT; k++) w2[j][k] = clamp(w2[j][k] + 0.1f * r * e2[j][k]);
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1.5f, v));
    }
}

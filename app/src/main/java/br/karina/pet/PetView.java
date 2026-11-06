package br.karina.pet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Desenha o Puppy (mascote chama) e as barras de estado; toque no pet = carinho. */
public class PetView extends View {
    private static final String NOME = "Puppy";
    private final Pet pet;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private final Path path = new Path();
    private float x = 0.5f, y = 0.5f, tx = 0.5f, ty = 0.5f;
    private float cr = 50f, cg = 170f, cb = 255f; // cor atual (muda suavemente)

    private final Runnable loop = new Runnable() {
        @Override
        public void run() {
            pet.tick();
            move();
            invalidate();
            postDelayed(this, 100);
        }
    };

    public PetView(Context ctx, Pet pet) {
        super(ctx);
        this.pet = pet;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        retomar();
    }

    public void retomar() {
        removeCallbacks(loop);
        post(loop);
    }

    public void pausar() {
        removeCallbacks(loop);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(loop);
        super.onDetachedFromWindow();
    }

    private void move() {
        boolean chegou = Math.abs(x - tx) < 0.02f && Math.abs(y - ty) < 0.02f;
        if (chegou || Math.random() < 0.03) {
            tx = 0.3f + (float) Math.random() * 0.4f;
            ty = 0.42f + (float) Math.random() * 0.2f;
        }
        float v = pet.action == Pet.DORMIR ? 0f : (pet.action == Pet.TRAVESSURA ? 0.08f : 0.025f);
        x += (tx - x) * v;
        y += (ty - y) * v;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            float rr = Math.min(getWidth(), getHeight()) * 0.15f;
            float dx = e.getX() - x * getWidth();
            float dy = e.getY() - (y * getHeight() - rr * 0.2f);
            float hit = rr * 1.6f;
            if (dx * dx + dy * dy < hit * hit) pet.carinho();
        }
        return true;
    }

    // ----- cores -----

    private int alvo() {
        if (pet.action == Pet.DORMIR) return Color.rgb(110, 100, 255);
        if (pet.raiva > 0.4f) return Color.rgb(255, 60, 50);
        if (pet.action == Pet.TRAVESSURA) return Color.rgb(255, 150, 30);
        if (pet.action == Pet.PEDIR) return Color.rgb(255, 200, 50);
        if (pet.alegria < 0.3f) return Color.rgb(70, 110, 210);
        if (pet.action == Pet.BRINCAR || pet.alegria > 0.85f) return Color.rgb(90, 255, 100);
        return Color.rgb(50, 170, 255);
    }

    private int cor(int a) {
        return Color.argb(a, (int) cr, (int) cg, (int) cb);
    }

    private int claro(int a) {
        return Color.argb(a,
                (int) (cr + (255f - cr) * 0.55f),
                (int) (cg + (255f - cg) * 0.55f),
                (int) (cb + (255f - cb) * 0.55f));
    }

    // ----- peças -----

    private void chama(Canvas c, float cx, float cy, float r, float sway, float flick) {
        path.reset();
        path.moveTo(cx - r, cy + r * 0.2f);
        path.cubicTo(cx - r * 1.1f, cy - r * 0.7f, cx - r * 0.35f, cy - r * 0.9f,
                cx - r * 0.05f + sway, cy - r * 2.1f + flick);
        path.cubicTo(cx + r * 0.2f, cy - r * 1.0f, cx + r * 1.1f, cy - r * 0.8f, cx + r, cy + r * 0.2f);
        path.cubicTo(cx + r, cy + r * 1.0f, cx - r, cy + r * 1.0f, cx - r, cy + r * 0.2f);
        path.close();
        p.setStyle(Paint.Style.FILL);
        for (int i = 3; i >= 1; i--) { // brilho em camadas
            float s = 1f + i * 0.09f;
            c.save();
            c.scale(s, s, cx, cy + r * 0.2f);
            p.setColor(cor(28));
            c.drawPath(path, p);
            c.restore();
        }
        p.setColor(cor(235));
        c.drawPath(path, p);
        c.save();
        c.scale(0.72f, 0.72f, cx, cy + r * 0.45f);
        p.setColor(claro(200));
        c.drawPath(path, p);
        c.restore();
    }

    private void lingua(Canvas c, float x0, float y0, float s, float lean) {
        path.reset();
        path.moveTo(x0 - s * 0.5f, y0);
        path.quadTo(x0 - s * 0.5f, y0 - s, x0 + lean, y0 - s * 2f);
        path.quadTo(x0 + s * 0.6f, y0 - s * 0.9f, x0 + s * 0.5f, y0);
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(cor(220));
        c.drawPath(path, p);
    }

    private void orbita(Canvas c, float cx, float cy, float r, long t, boolean frente) {
        c.save();
        c.rotate(-12f, cx, cy);
        rf.set(cx - r * 1.9f, cy - r * 0.4f, cx + r * 1.9f, cy + r * 0.4f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        if (frente) {
            p.setColor(cor(210));
            c.drawArc(rf, 0, 180, false, p);
        } else {
            p.setColor(cor(90));
            c.drawOval(rf, p);
            p.setStrokeWidth(6f);
            p.setColor(claro(255));
            c.drawArc(rf, (t / 8) % 360, 50, false, p);
        }
        c.restore();
    }

    private void triangulo(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3) {
        path.reset();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(8, 8, 14));
        c.drawPath(path, p);
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        c.drawColor(Color.rgb(6, 8, 18));
        float r = Math.min(w, h) * 0.15f;
        long t = System.currentTimeMillis();
        boolean dorme = pet.action == Pet.DORMIR;
        float pulo = dorme ? 0f : (float) Math.sin(t / 220.0) * (pet.action == Pet.BRINCAR ? 14f : 4f);
        float baseY = y * h;
        float cx = x * w, cy = baseY + pulo;

        int a = alvo();
        cr += (Color.red(a) - cr) * 0.12f;
        cg += (Color.green(a) - cg) * 0.12f;
        cb += (Color.blue(a) - cb) * 0.12f;

        float energico = (pet.action == Pet.BRINCAR || pet.action == Pet.TRAVESSURA) ? 1.8f : 1f;
        float flick = dorme ? 0f : (float) Math.sin(t / 130.0) * r * 0.12f;
        float sway = dorme ? 0f : (float) Math.sin(t / 350.0) * r * 0.1f * energico;

        // anéis no chão
        float fy0 = baseY + r * 1.15f;
        float pulso = 1f + (float) Math.sin(t / 400.0) * 0.04f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(cor(35));
        rf.set(cx - r * 1.9f * pulso, fy0 - r * 0.42f, cx + r * 1.9f * pulso, fy0 + r * 0.42f);
        c.drawOval(rf, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4f);
        p.setColor(cor(200));
        rf.set(cx - r * 1.55f * pulso, fy0 - r * 0.34f, cx + r * 1.55f * pulso, fy0 + r * 0.34f);
        c.drawOval(rf, p);
        p.setStrokeWidth(3f);
        p.setColor(cor(110));
        rf.set(cx - r * 1.0f * pulso, fy0 - r * 0.22f, cx + r * 1.0f * pulso, fy0 + r * 0.22f);
        c.drawOval(rf, p);

        // órbita (parte de trás)
        orbita(c, cx, cy, r, t, false);

        // personagem (achata um pouco quando dorme)
        c.save();
        if (dorme) c.scale(1.05f, 0.88f, cx, cy + r * 1.05f);

        lingua(c, cx - r * 0.95f, cy - r * 0.1f, r * 0.35f, sway - r * 0.1f);
        lingua(c, cx + r * 0.9f, cy - r * 0.25f, r * 0.3f, sway + r * 0.1f);
        chama(c, cx, cy, r, sway, flick);

        // rosto preto
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(8, 8, 14));
        rf.set(cx - r * 0.88f, cy - r * 0.3f, cx + r * 0.88f, cy + r * 0.78f);
        c.drawOval(rf, p);

        // olhos
        float ey = cy + r * 0.22f;
        float ex = r * 0.38f;
        boolean pisca = (t % 3500) < 130;
        for (int s = -1; s <= 1; s += 2) {
            float xc = cx + s * ex;
            if (dorme || pisca) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(6f);
                p.setColor(claro(255));
                rf.set(xc - r * 0.2f, ey - r * 0.1f, xc + r * 0.2f, ey + r * 0.1f);
                c.drawArc(rf, 20, 140, false, p);
            } else if (pet.raiva > 0.4f) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(Color.rgb(245, 250, 255));
                rf.set(xc - r * 0.17f, ey - r * 0.2f, xc + r * 0.17f, ey + r * 0.2f);
                c.drawOval(rf, p);
                triangulo(c, xc + s * r * 0.25f, ey - r * 0.3f, xc - s * r * 0.25f, ey - r * 0.3f,
                        xc - s * r * 0.25f, ey + r * 0.02f);
            } else if (pet.alegria > 0.55f) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(8f);
                p.setColor(Color.rgb(245, 250, 255));
                rf.set(xc - r * 0.2f, ey - r * 0.12f, xc + r * 0.2f, ey + r * 0.2f);
                c.drawArc(rf, 200, 140, false, p);
            } else if (pet.alegria < 0.3f) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(Color.rgb(245, 250, 255));
                rf.set(xc - r * 0.15f, ey - r * 0.17f, xc + r * 0.15f, ey + r * 0.2f);
                c.drawOval(rf, p);
                triangulo(c, xc - s * r * 0.25f, ey - r * 0.3f, xc + s * r * 0.25f, ey - r * 0.3f,
                        xc + s * r * 0.25f, ey + r * 0.02f);
            } else {
                p.setStyle(Paint.Style.FILL);
                p.setColor(Color.rgb(245, 250, 255));
                rf.set(xc - r * 0.17f, ey - r * 0.2f, xc + r * 0.17f, ey + r * 0.2f);
                c.drawOval(rf, p);
            }
        }

        // pezinhos
        float fyL = pet.action == Pet.BRINCAR ? (float) Math.sin(t / 110.0) * r * 0.08f : 0f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(8, 8, 14));
        rf.set(cx - r * 0.62f, cy + r * 0.78f + fyL, cx - r * 0.12f, cy + r * 1.08f + fyL);
        c.drawOval(rf, p);
        rf.set(cx + r * 0.12f, cy + r * 0.78f - fyL, cx + r * 0.62f, cy + r * 1.08f - fyL);
        c.drawOval(rf, p);

        c.restore();

        // órbita (parte da frente)
        orbita(c, cx, cy, r, t, true);

        // partículas
        int n = dorme ? 3 : 8;
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < n; i++) {
            float ph = ((t / 25 + i * 137L) % 1000L) / 1000f;
            float px = cx + (float) Math.sin(i * 2.1 + t / 1500.0) * r * 1.8f;
            float py = cy + r * 0.8f - ph * r * 3.2f;
            float sz = r * 0.07f * (1f - ph) + 2f;
            p.setColor(cor((int) ((1f - ph) * 220)));
            c.drawRect(px - sz, py - sz, px + sz, py + sz, p);
        }

        // fala e Zzz
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.LTGRAY);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(Math.min(w, h) * 0.05f);
        c.drawText(pet.fala, cx, cy - r * 2.5f, p);
        if (dorme) {
            p.setColor(cor(255));
            p.setTextSize(Math.min(w, h) * 0.07f);
            c.drawText("Z z z", cx + r * 1.4f, cy - r * 1.4f, p);
        }

        // nome
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(Math.min(w, h) * 0.05f);
        p.setColor(cor(255));
        c.drawText(NOME, w * 0.95f, h * 0.07f, p);

        // barras de estado
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(Math.min(w, h) * 0.035f);
        float bx0 = w * 0.05f, by = h * 0.05f, bw = w * 0.4f, gap = h * 0.04f;
        bar(c, "Fome", pet.fome, bx0, by, bw);
        bar(c, "Energia", pet.energia, bx0, by + gap, bw);
        bar(c, "Alegria", pet.alegria, bx0, by + gap * 2, bw);
        bar(c, "Vínculo", pet.vinculo, bx0, by + gap * 3, bw);
        bar(c, "Rebeldia", pet.brain.rebeldia, bx0, by + gap * 4, bw);
    }

    private void bar(Canvas c, String nome, float v, float x0, float y0, float w) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.GRAY);
        c.drawText(nome, x0, y0, p);
        float bx = x0 + w * 0.55f;
        p.setColor(Color.rgb(40, 44, 58));
        c.drawRect(bx, y0 - p.getTextSize() * 0.8f, bx + w * 0.7f, y0, p);
        p.setColor(claro(255));
        c.drawRect(bx, y0 - p.getTextSize() * 0.8f, bx + w * 0.7f * Math.max(0f, Math.min(1f, v)), y0, p);
    }
}

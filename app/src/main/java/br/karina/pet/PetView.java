package br.karina.pet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Desenha o pet e as barras de estado; toque no pet = carinho. */
public class PetView extends View {
    private final Pet pet;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float x = 0.5f, y = 0.5f, tx = 0.5f, ty = 0.5f;

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
        post(loop);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(loop);
        super.onDetachedFromWindow();
    }

    private void move() {
        boolean chegou = Math.abs(x - tx) < 0.02f && Math.abs(y - ty) < 0.02f;
        if (chegou || Math.random() < 0.03) {
            tx = 0.15f + (float) Math.random() * 0.7f;
            ty = 0.3f + (float) Math.random() * 0.5f;
        }
        float v = pet.action == Pet.DORMIR ? 0f : (pet.action == Pet.TRAVESSURA ? 0.08f : 0.025f);
        x += (tx - x) * v;
        y += (ty - y) * v;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            float dx = e.getX() - x * getWidth();
            float dy = e.getY() - y * getHeight();
            float r = Math.min(getWidth(), getHeight()) * 0.2f;
            if (dx * dx + dy * dy < r * r) pet.carinho();
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        c.drawColor(Color.rgb(18, 20, 28));
        float r = Math.min(w, h) * 0.14f;
        float bounce = (float) Math.sin(System.currentTimeMillis() / 200.0)
                * (pet.action == Pet.BRINCAR ? 12f : 3f);
        float cx = x * w, cy = y * h + bounce;

        // corpo
        p.setStyle(Paint.Style.FILL);
        p.setColor(pet.cor());
        c.drawCircle(cx, cy, r, p);

        // olhos
        float ey = cy - r * 0.2f;
        if (pet.action == Pet.DORMIR) {
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(5f);
            c.drawLine(cx - r * 0.5f, ey, cx - r * 0.2f, ey, p);
            c.drawLine(cx + r * 0.2f, ey, cx + r * 0.5f, ey, p);
        } else {
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            c.drawCircle(cx - r * 0.35f, ey, r * 0.17f, p);
            c.drawCircle(cx + r * 0.35f, ey, r * 0.17f, p);
            p.setColor(Color.BLACK);
            c.drawCircle(cx - r * 0.35f, ey, r * 0.08f, p);
            c.drawCircle(cx + r * 0.35f, ey, r * 0.08f, p);
        }

        // boca: sorriso se feliz, careta se triste/bravo
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5f);
        p.setColor(Color.WHITE);
        float humor = pet.alegria - pet.raiva - 0.4f;
        if (humor >= 0f) {
            c.drawArc(new RectF(cx - r * 0.4f, cy + r * 0.05f, cx + r * 0.4f, cy + r * 0.6f), 20, 140, false, p);
        } else {
            c.drawArc(new RectF(cx - r * 0.4f, cy + r * 0.45f, cx + r * 0.4f, cy + r * 0.85f), 200, 140, false, p);
        }

        // fala
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.LTGRAY);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(Math.min(w, h) * 0.05f);
        c.drawText(pet.fala, cx, cy - r * 1.4f, p);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(Math.min(w, h) * 0.05f);
        p.setColor(Color.WHITE);
        c.drawText("Puppy", w * 0.95f, h * 0.07f, p);
        
        // barras
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(Math.min(w, h) * 0.035f);
        float bx = w * 0.05f, by = h * 0.05f, bw = w * 0.4f, gap = h * 0.04f;
        bar(c, "Fome", pet.fome, bx, by, bw);
        bar(c, "Energia", pet.energia, bx, by + gap, bw);
        bar(c, "Alegria", pet.alegria, bx, by + gap * 2, bw);
        bar(c, "Vínculo", pet.vinculo, bx, by + gap * 3, bw);
        bar(c, "Rebeldia", pet.brain.rebeldia, bx, by + gap * 4, bw);
    }

    private void bar(Canvas c, String nome, float v, float x0, float y0, float w) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.GRAY);
        c.drawText(nome, x0, y0, p);
        float bx = x0 + w * 0.55f;
        p.setColor(Color.rgb(50, 54, 66));
        c.drawRect(bx, y0 - p.getTextSize() * 0.8f, bx + w * 0.7f, y0, p);
        p.setColor(Color.rgb(90, 200, 140));
        c.drawRect(bx, y0 - p.getTextSize() * 0.8f, bx + w * 0.7f * Math.max(0f, Math.min(1f, v)), y0, p);
    }
}

package br.karina.pet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int FUNDO = Color.rgb(5, 9, 20);
    private static final int PAINEL = Color.rgb(13, 22, 42);
    private static final int TEXTO = Color.rgb(239, 245, 255);
    private static final int SUAVE = Color.rgb(145, 163, 188);
    private static final int AZUL = Color.rgb(100, 220, 255);

    private Pet pet;
    private PetView view;
    private SharedPreferences sp, sk, sd;
    private TextView info;
    private ScrollView rolagem;
    private EditText entrada;
    private long pausadoEm = 0L;
    private static final int REQ_MODELO = 741;
    private TextView modeloStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(5, 9, 20));
        getWindow().setNavigationBarColor(FUNDO);

        sp = getSharedPreferences("pet", MODE_PRIVATE);
        sk = getSharedPreferences("conhecimento", MODE_PRIVATE);
        sd = getSharedPreferences("diario", MODE_PRIVATE);
        pet = Pet.load(sp);
        view = new PetView(this, pet);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(gradiente(new int[]{Color.rgb(5, 9, 20), Color.rgb(10, 17, 37), Color.rgb(7, 10, 24)}, 0));
        root.setPadding(dp(18), dp(10), dp(18), dp(12));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(0, dp(3), 0, dp(9));

        LinearLayout identidade = new LinearLayout(this);
        identidade.setOrientation(LinearLayout.VERTICAL);
        identidade.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView titulo = texto("PUPPY", 22, TEXTO, true);
        titulo.setLetterSpacing(0.12f);
        TextView subtitulo = texto("SEU COMPANHEIRO DIGITAL", 9, SUAVE, true);
        subtitulo.setLetterSpacing(0.13f);
        identidade.addView(titulo);
        identidade.addView(subtitulo);
        header.addView(identidade);

        TextView status = texto("●  CÉREBRO LOCAL", 10, Color.rgb(104, 224, 181), true);
        status.setLetterSpacing(0.04f);
        status.setPadding(dp(10), dp(8), dp(10), dp(8));
        status.setBackground(vidro(Color.argb(150, 16, 65, 67), dp(18), Color.argb(170, 65, 211, 190)));
        header.addView(status);

        TextView diario = texto("DIÁRIO  ↗", 10, AZUL, true);
        diario.setGravity(Gravity.CENTER);
        diario.setPadding(dp(12), dp(10), 0, dp(10));
        diario.setOnClickListener(v -> mostrarDiario());
        diario.setContentDescription("Abrir diário do Puppy");
        diario.setFocusable(true);
        header.addView(diario);

        TextView modelo = texto("MODELO", 9, AZUL, true);
        modelo.setPadding(dp(10), dp(10), 0, dp(10));
        modelo.setOnClickListener(v -> selecionarModelo());
        modelo.setContentDescription("Instalar modelo de inteligência artificial");
        header.addView(modelo);
        root.addView(header);

        modeloStatus = texto(LlamaRuntime.status(this), 9, SUAVE, false);
        modeloStatus.setPadding(dp(3), 0, dp(3), dp(7));
        root.addView(modeloStatus);

        LinearLayout palco = new LinearLayout(this);
        palco.setOrientation(LinearLayout.VERTICAL);
        palco.setBackground(gradiente(new int[]{Color.argb(220, 25, 42, 73), Color.argb(205, 13, 22, 43), Color.argb(230, 25, 24, 57)}, dp(25)));
        palco.setElevation(dp(8));
        palco.setClipToOutline(true);
        palco.addView(view, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(palco, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView rotulo = texto("SINAL DO PUPPY", 9, SUAVE, true);
        rotulo.setLetterSpacing(0.14f);
        rotulo.setPadding(dp(3), dp(12), dp(3), dp(5));
        root.addView(rotulo);

        rolagem = new ScrollView(this);
        rolagem.setFillViewport(true);
        rolagem.setVerticalScrollBarEnabled(false);
        rolagem.setBackground(vidro(Color.argb(175, 18, 29, 53), dp(16), Color.argb(150, 92, 143, 194)));
        info = texto("Oi! Eu sou o Puppy. Toque em mim para fazer carinho ou escolha uma ação abaixo. ✨", 13, TEXTO, false);
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setPadding(dp(14), dp(10), dp(14), dp(10));
        rolagem.addView(info);
        root.addView(rolagem, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(76)));

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setPadding(0, dp(9), 0, dp(8));

        entrada = new EditText(this);
        entrada.setSingleLine(true);
        entrada.setTextSize(13);
        entrada.setTextColor(TEXTO);
        entrada.setHintTextColor(SUAVE);
        entrada.setHint("Converse com o Puppy...");
        entrada.setPadding(dp(14), 0, dp(10), 0);
        entrada.setImeOptions(EditorInfo.IME_ACTION_SEND);
        entrada.setBackground(vidro(Color.argb(205, 19, 31, 57), dp(15), Color.argb(180, 88, 153, 211)));
        composer.addView(entrada, new LinearLayout.LayoutParams(0, dp(48), 1f));

        TextView enviar = texto("ENVIAR  ↑", 10, Color.rgb(5, 17, 29), true);
        enviar.setGravity(Gravity.CENTER);
        enviar.setLetterSpacing(0.04f);
        enviar.setBackground(gradiente(new int[]{Color.rgb(124, 232, 255), Color.rgb(84, 177, 255)}, dp(14)));
        enviar.setElevation(dp(5));
        LinearLayout.LayoutParams enviarLp = new LinearLayout.LayoutParams(dp(88), dp(48));
        enviarLp.leftMargin = dp(8);
        composer.addView(enviar, enviarLp);
        enviar.setOnClickListener(v -> enviar());
        entrada.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                enviar();
                return true;
            }
            return false;
        });
        root.addView(composer);

        LinearLayout acoes = new LinearLayout(this);
        acoes.setOrientation(LinearLayout.VERTICAL);
        LinearLayout primeira = linhaAcoes();
        adicionarAcao(primeira, "♡", "CARINHO", Color.rgb(255, 125, 174), () -> pet.carinho());
        adicionarAcao(primeira, "✦", "COMIDA", Color.rgb(255, 190, 93), () -> pet.comer());
        LinearLayout segunda = linhaAcoes();
        adicionarAcao(segunda, "↗", "BRINCAR", Color.rgb(105, 207, 255), () -> pet.brincar());
        adicionarAcao(segunda, "!", "BRONCA", Color.rgb(174, 157, 255), () -> pet.bronca());
        acoes.addView(primeira);
        LinearLayout.LayoutParams segundaLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        segundaLp.topMargin = dp(7);
        acoes.addView(segunda, segundaLp);
        root.addView(acoes);

        setContentView(root);
    }

    private LinearLayout linhaAcoes() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        return row;
    }

    private void adicionarAcao(LinearLayout row, String simbolo, String nome, int cor, Runnable callback) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(vidro(Color.argb(175, 22, 34, 60), dp(15), Color.argb(170, 78, 126, 174)));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> callback.run());
        card.setContentDescription(nome.toLowerCase() + " no Puppy");

        TextView icone = texto(simbolo, 19, cor, true);
        icone.setGravity(Gravity.CENTER);
        TextView label = texto(nome, 10, TEXTO, true);
        label.setLetterSpacing(0.05f);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelLp.leftMargin = dp(8);
        card.addView(icone);
        card.addView(label, labelLp);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(54), 1f);
        if (row.getChildCount() > 0) lp.leftMargin = dp(7);
        row.addView(card, lp);
    }

    private TextView texto(String valor, float tamanho, int cor, boolean negrito) {
        TextView t = new TextView(this);
        t.setText(valor);
        t.setTextSize(tamanho);
        t.setTextColor(cor);
        if (negrito) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable vidro(int cor, int raio, int borda) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(cor);
        d.setCornerRadius(raio);
        d.setStroke(dp(1), borda);
        return d;
    }

    private GradientDrawable gradiente(int[] cores, int raio) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, cores);
        d.setCornerRadius(raio);
        d.setStroke(dp(1), Color.argb(125, 121, 171, 225));
        return d;
    }

    private GradientDrawable fundo(int cor, int raio, int borda) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(cor);
        d.setCornerRadius(raio);
        d.setStroke(dp(1), borda);
        return d;
    }

    private int dp(float valor) {
        return (int) (valor * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void selecionarModelo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, "Selecione o modelo Qwen3 GGUF");
        startActivityForResult(intent, REQ_MODELO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_MODELO || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        modeloStatus.setText("Copiando modelo para o Puppy...");
        new Thread(() -> {
            String resultado;
            try {
                LlamaRuntime.importarModelo(this, uri);
                resultado = LlamaRuntime.status(this);
            } catch (Exception e) {
                resultado = "Falha ao importar: " + (e.getMessage() == null ? "erro de leitura" : e.getMessage());
            }
            final String mensagem = resultado;
            runOnUiThread(() -> {
                modeloStatus.setText(mensagem);
                info.setText(mensagem);
            });
        }).start();
    }

    private void mostrarDiario() {
        info.setText(Diario.ler(sd));
        rolagem.scrollTo(0, 0);
    }

    private void enviar() {
        final String cmd = entrada.getText().toString().trim();
        if (cmd.isEmpty()) return;
        entrada.setText("");
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(entrada.getWindowToken(), 0);
        if (Skills.norm(cmd).equals("diario")) {
            mostrarDiario();
            return;
        }
        modeloStatus.setText(LlamaRuntime.status(this));
        info.setText("O Puppy está pensando...");
        new Thread(() -> {
            String respostaLlm = LlamaRuntime.responder(this, cmd);
            Skills.Resposta r;
            if (respostaLlm == null || respostaLlm.trim().isEmpty()) {
                r = Skills.executar(cmd, sk);
            } else {
                r = new Skills.Resposta();
                r.texto = respostaLlm.trim();
            }
            runOnUiThread(() -> {
                info.setText(r.texto);
                modeloStatus.setText(LlamaRuntime.status(this));
                rolagem.scrollTo(0, 0);
                if (r.fala != null) pet.dizer(r.fala);
                if (r.aprendeu != null) pet.aprendeu(r.aprendeu);
                Diario.registrar(sd, r.diario);
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pausadoEm > 0L) {
            long ms = System.currentTimeMillis() - pausadoEm;
            pet.passarTempo(ms);
            if (ms >= 10 * 60000L) {
                Diario.registrar(sd, "Você voltou depois de " + (ms / 60000L) + " min");
            }
        }
        view.retomar();
    }

    @Override
    protected void onPause() {
        pausadoEm = System.currentTimeMillis();
        view.pausar();
        pet.save(sp);
        super.onPause();
    }
}

package br.karina.pet;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.URL;

public class MainActivity extends Activity {
    private Pet pet;
    private PetView view;
    private SharedPreferences sp;
    private SharedPreferences sk;
    private TextView info;
    private EditText entrada;
    private long pausadoEm = 0L;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("pet", MODE_PRIVATE);
        sk = getSharedPreferences("conhecimento", MODE_PRIVATE);
        pet = Pet.load(sp);
        view = new PetView(this, pet);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(view,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        info = new TextView(this);
        info.setTextColor(0xFFB0B8C8);
        info.setTextSize(13);
        info.setPadding(24, 8, 24, 8);
        info.setMaxLines(12);
        info.setText("Digite um assunto: o Puppy pesquisa na Wikipédia e guarda o que aprendeu.");
        root.addView(info, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        entrada = new EditText(this);
        entrada.setHint("Assunto...");
        entrada.setSingleLine(true);
        entrada.setTextColor(0xFFFFFFFF);
        entrada.setHintTextColor(0xFF808898);
        linha.addView(entrada, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
        linha.addView(botao("Aprender", () -> aprender()));
        linha.addView(botao("Notícias", () -> noticias()));
        root.addView(linha, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout barra = new LinearLayout(this);
        barra.setOrientation(LinearLayout.HORIZONTAL);
        barra.addView(botao("Carinho", () -> pet.carinho()));
        barra.addView(botao("Comida", () -> pet.comer()));
        barra.addView(botao("Brincar", () -> pet.brincar()));
        barra.addView(botao("Bronca", () -> pet.bronca()));
        root.addView(barra, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private Button botao(String texto, Runnable acao) {
        Button bt = new Button(this);
        bt.setText(texto);
        bt.setAllCaps(false);
        bt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bt.setOnClickListener(v -> acao.run());
        return bt;
    }

    private void aprender() {
        final String termo = entrada.getText().toString().trim();
        if (termo.isEmpty()) return;
        info.setText("Pesquisando \"" + termo + "\"...");
        new Thread(() -> {
            String msg;
            String aprendido = null;
            try {
                String[] r = Conhecimento.wikipedia(termo);
                if (r == null || r[1].isEmpty()) {
                    msg = "Não encontrei nada sobre \"" + termo + "\".";
                } else {
                    Conhecimento.guardar(sk, "Wikipédia", r[0], r[1], r[2]);
                    aprendido = r[0];
                    msg = r[0] + ": " + r[1] + "\n(Fonte: Wikipédia, CC BY-SA)";
                }
            } catch (Exception e) {
                msg = "Sem conexão ou erro: " + e.getMessage();
            }
            final String m = msg;
            final String a = aprendido;
            runOnUiThread(() -> {
                info.setText(m);
                if (a != null) pet.aprendeu(a);
            });
        }).start();
    }

    private void noticias() {
        info.setText("Buscando manchetes...");
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            int novos = 0;
            for (String feed : Conhecimento.FEEDS) {
                try {
                    String fonte = new URL(feed).getHost();
                    for (String[] it : Conhecimento.manchetes(feed, 5)) {
                        Conhecimento.guardar(sk, fonte, it[0], "", it[1]);
                        sb.append("• ").append(it[0]).append("\n");
                        novos++;
                    }
                } catch (Exception e) {
                    sb.append("(não consegui ler: ").append(feed).append(")\n");
                }
            }
            final String m = sb.length() == 0 ? "Nada encontrado." : sb.toString();
            final int n = novos;
            runOnUiThread(() -> {
                info.setText(m);
                if (n > 0) pet.aprendeu("as notícias de hoje");
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pausadoEm > 0L) pet.passarTempo(System.currentTimeMillis() - pausadoEm);
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

package br.karina.pet;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private Pet pet;
    private PetView view;
    private SharedPreferences sp;   // estado do pet
    private SharedPreferences sk;   // conhecimento
    private SharedPreferences sd;   // diário
    private TextView info;
    private ScrollView rolagem;
    private EditText entrada;
    private long pausadoEm = 0L;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("pet", MODE_PRIVATE);
        sk = getSharedPreferences("conhecimento", MODE_PRIVATE);
        sd = getSharedPreferences("diario", MODE_PRIVATE);
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
        info.setText("Fale com o Puppy. Tente: clima, notícias, aprender sobre <assunto> ou ajuda.");
        rolagem = new ScrollView(this);
        rolagem.setBackgroundColor(0xFF10131E);
        rolagem.addView(info);
        int altura = (int) (getResources().getDisplayMetrics().density * 130);
        root.addView(rolagem, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, altura));

        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        entrada = new EditText(this);
        entrada.setHint("Fale com o Puppy...");
        entrada.setSingleLine(true);
        entrada.setTextColor(0xFFFFFFFF);
        entrada.setHintTextColor(0xFF808898);
        linha.addView(entrada, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
        linha.addView(botao("Enviar", () -> enviar()));
        linha.addView(botao("Diário", () -> mostrarDiario()));
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

    private void mostrarDiario() {
        info.setText(Diario.ler(sd));
        rolagem.scrollTo(0, 0);
    }

    private void enviar() {
        final String cmd = entrada.getText().toString().trim();
        if (cmd.isEmpty()) return;
        entrada.setText("");
        if (Skills.norm(cmd).equals("diario")) {
            mostrarDiario();
            return;
        }
        info.setText("Pensando...");
        new Thread(() -> {
            Skills.Resposta r = Skills.executar(cmd, sk);
            runOnUiThread(() -> {
                info.setText(r.texto);
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
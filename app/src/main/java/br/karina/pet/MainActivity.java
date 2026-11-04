package br.karina.pet;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    private Pet pet;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("pet", MODE_PRIVATE);
        pet = Pet.load(sp);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(new PetView(this, pet),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

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

    @Override
    protected void onPause() {
        pet.save(sp);
        super.onPause();
    }
}

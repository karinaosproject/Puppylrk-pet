package br.karina.pet;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Adaptador opcional: sem biblioteca ou modelo, o app mantém o fallback Skills. */
public final class LlamaRuntime {
    private static final String MODEL_FILE = "qwen3-0.6b-q4_k_m.gguf";
    private static final int CONTEXT_SIZE = 384;
    private static final int THREADS = 2;
    private static boolean libraryLoaded;
    private static String loadedModelPath;

    static {
        try {
            System.loadLibrary("puppy_llama");
            libraryLoaded = true;
        } catch (UnsatisfiedLinkError | SecurityException ignored) {
            libraryLoaded = false;
        }
    }

    private LlamaRuntime() {}

    public static synchronized String responder(Context context, String prompt) {
        if (!libraryLoaded || prompt == null || prompt.trim().isEmpty()) return null;
        File model = new File(new File(context.getFilesDir(), "models"), MODEL_FILE);
        if (!model.isFile() || model.length() < 1024 * 1024) return null;
        if (!model.getAbsolutePath().equals(loadedModelPath)) {
            if (!nativeLoadModel(model.getAbsolutePath(), CONTEXT_SIZE, THREADS)) return null;
            loadedModelPath = model.getAbsolutePath();
        }
        try {
            String system = "Você é Puppy, um assistente pessoal útil, claro e seguro. Responda em português brasileiro. Não afirme que executou ações externas.\n";
            String chat = "<|im_start|>system\n" + system + "<|im_end|>\n"
                    + "<|im_start|>user\n" + prompt + "<|im_end|>\n"
                    + "<|im_start|>assistant\n";
            return nativeGenerate(chat, 64);
        } catch (OutOfMemoryError | RuntimeException error) {
            return null;
        }
    }

    public static boolean isLibraryAvailable() { return libraryLoaded; }

    public static synchronized boolean hasModel(Context context) {
        File model = new File(new File(context.getFilesDir(), "models"), MODEL_FILE);
        return model.isFile() && model.length() >= 1024 * 1024;
    }

    /** Copia o GGUF escolhido pelo usuário para o diretório privado do app. */
    public static synchronized void importarModelo(Context context, Uri uri) throws Exception {
        if (uri == null) throw new IllegalArgumentException("Arquivo não selecionado.");
        File dir = new File(context.getFilesDir(), "models");
        if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("Não foi possível criar a pasta de modelos.");
        File destino = new File(dir, MODEL_FILE);
        File temporario = new File(dir, MODEL_FILE + ".tmp");
        // Libera mmap/contexto antes de substituir um modelo já carregado.
        if (loadedModelPath != null) {
            nativeUnload();
            loadedModelPath = null;
        }
        long total = 0L;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(temporario)) {
            if (in == null) throw new java.io.IOException("Não foi possível abrir o arquivo selecionado.");
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                out.write(buffer, 0, n);
            }
            out.getFD().sync();
        } catch (Exception e) {
            temporario.delete();
            throw e;
        }
        if (total < 1024 * 1024) {
            temporario.delete();
            throw new java.io.IOException("O arquivo é pequeno demais para ser um modelo GGUF válido.");
        }
        if (destino.exists() && !destino.delete()) {
            temporario.delete();
            throw new java.io.IOException("Não foi possível substituir o modelo anterior.");
        }
        if (!temporario.renameTo(destino)) {
            temporario.delete();
            throw new java.io.IOException("Não foi possível finalizar a instalação do modelo.");
        }
    }

    public static String status(Context context) {
        if (!libraryLoaded) return "Biblioteca nativa indisponível";
        if (!hasModel(context)) return "Biblioteca pronta · modelo não instalado";
        return "Qwen3 GGUF encontrado · ainda não testado";
    }

    private static native boolean nativeLoadModel(String path, int contextSize, int threads);
    private static native String nativeGenerate(String prompt, int maxTokens);
    private static native void nativeUnload();
}

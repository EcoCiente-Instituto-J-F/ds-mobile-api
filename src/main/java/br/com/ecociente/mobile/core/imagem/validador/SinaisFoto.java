package br.com.ecociente.mobile.core.imagem.validador;

import java.awt.image.BufferedImage;

public final class SinaisFoto {

    static final int LADO = 256;

    static final int RAIO_MIN = 24;

    static final int RAIO_MAX = 120;

    static final double LIMIAR_PICO = 30.0;

    private SinaisFoto() {
    }

    public static boolean temPadraoDeTela(BufferedImage imagem) {
        int largura = imagem.getWidth();
        int altura = imagem.getHeight();
        if (Math.min(largura, altura) < LADO) {
            return false;
        }
        int esquerda = (largura - LADO) / 2;
        int topo = (altura - LADO) / 2;
        int[] cinza = Luminancia.canalCinza(imagem, esquerda, topo, LADO, LADO);
        double[] janela = janelaHann(LADO);
        double media = 0;
        for (int v : cinza) {
            media += v;
        }
        media /= cinza.length;
        double[][] recorte = new double[LADO][LADO];
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                recorte[y][x] = (cinza[y * LADO + x] - media) * janela[y] * janela[x];
            }
        }
        double[][] espectro = Fft.magnitude2d(recorte);
        int maiorRaio = (int) Math.hypot(LADO / 2, LADO / 2) + 1;
        double[] somaAnel = new double[maiorRaio + 1];
        int[] contagemAnel = new int[maiorRaio + 1];
        int[][] raio = new int[LADO][LADO];
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                int r = (int) Math.hypot(frequencia(y), frequencia(x));
                raio[y][x] = r;
                somaAnel[r] += espectro[y][x];
                contagemAnel[r]++;
            }
        }
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                int r = raio[y][x];
                if (r < RAIO_MIN || r > RAIO_MAX) {
                    continue;
                }
                double mediaAnel = somaAnel[r] / Math.max(contagemAnel[r], 1);
                double razao = espectro[y][x] / Math.max(mediaAnel, 1e-9);
                if (razao > LIMIAR_PICO) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int frequencia(int k) {
        return k < LADO / 2 ? k : k - LADO;
    }

    private static double[] janelaHann(int n) {
        double[] janela = new double[n];
        for (int i = 0; i < n; i++) {
            janela[i] = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (n - 1));
        }
        return janela;
    }
}

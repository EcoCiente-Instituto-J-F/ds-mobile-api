package br.com.ecociente.mobile.core.imagem.validador;

import java.awt.image.BufferedImage;

public final class Luminancia {

    private Luminancia() {
    }

    public static int de(int r, int g, int b) {
        return (r * 19595 + g * 38470 + b * 7471 + 0x8000) >> 16;
    }

    public static int[] canalCinza(BufferedImage imagem) {
        return canalCinza(imagem, 0, 0, imagem.getWidth(), imagem.getHeight());
    }

    public static int[] canalCinza(BufferedImage imagem, int x0, int y0, int largura, int altura) {
        int[] rgb = imagem.getRGB(x0, y0, largura, altura, null, 0, largura);
        int[] cinza = new int[largura * altura];
        for (int i = 0; i < rgb.length; i++) {
            int p = rgb[i];
            cinza[i] = de((p >> 16) & 0xFF, (p >> 8) & 0xFF, p & 0xFF);
        }
        return cinza;
    }
}

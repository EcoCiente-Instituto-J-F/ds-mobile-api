package br.com.ecociente.mobile.core.imagem.validador;

import java.awt.image.BufferedImage;

public final class DHash {

    private DHash() {
    }

    public static String calcular(BufferedImage imagem) {
        int largura = imagem.getWidth();
        int altura = imagem.getHeight();
        int[] cinza = Luminancia.canalCinza(imagem);
        int[] miniatura = Reamostragem.redimensionar(cinza, largura, altura, 9, 8, Reamostragem.Nucleo.LANCZOS);
        long bits = 0;
        for (int linha = 0; linha < 8; linha++) {
            for (int coluna = 0; coluna < 8; coluna++) {
                int atual = miniatura[linha * 9 + coluna];
                int vizinho = miniatura[linha * 9 + coluna + 1];
                bits = (bits << 1) | (atual > vizinho ? 1 : 0);
            }
        }
        return String.format("%016x", bits);
    }

    public static int distancia(String hashA, String hashB) {
        return Long.bitCount(Long.parseUnsignedLong(hashA, 16) ^ Long.parseUnsignedLong(hashB, 16));
    }
}

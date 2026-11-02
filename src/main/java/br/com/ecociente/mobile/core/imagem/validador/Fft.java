package br.com.ecociente.mobile.core.imagem.validador;

final class Fft {

    private Fft() {
    }

    static double[][] magnitude2d(double[][] real) {
        int n = real.length;
        double[][] re = new double[n][n];
        double[][] im = new double[n][n];
        for (int y = 0; y < n; y++) {
            re[y] = real[y].clone();
            fft(re[y], im[y]);
        }
        double[] colRe = new double[n];
        double[] colIm = new double[n];
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                colRe[y] = re[y][x];
                colIm[y] = im[y][x];
            }
            fft(colRe, colIm);
            for (int y = 0; y < n; y++) {
                re[y][x] = colRe[y];
                im[y][x] = colIm[y];
            }
        }
        double[][] magnitude = new double[n][n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                magnitude[y][x] = Math.hypot(re[y][x], im[y][x]);
            }
        }
        return magnitude;
    }

    static void fft(double[] re, double[] im) {
        int n = re.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) {
                j ^= bit;
            }
            j ^= bit;
            if (i < j) {
                double t = re[i];
                re[i] = re[j];
                re[j] = t;
                t = im[i];
                im[i] = im[j];
                im[j] = t;
            }
        }
        for (int comprimento = 2; comprimento <= n; comprimento <<= 1) {
            double angulo = -2 * Math.PI / comprimento;
            double wRe = Math.cos(angulo);
            double wIm = Math.sin(angulo);
            for (int i = 0; i < n; i += comprimento) {
                double curRe = 1;
                double curIm = 0;
                for (int k = 0; k < comprimento / 2; k++) {
                    int a = i + k;
                    int b = i + k + comprimento / 2;
                    double tRe = re[b] * curRe - im[b] * curIm;
                    double tIm = re[b] * curIm + im[b] * curRe;
                    re[b] = re[a] - tRe;
                    im[b] = im[a] - tIm;
                    re[a] += tRe;
                    im[a] += tIm;
                    double novoRe = curRe * wRe - curIm * wIm;
                    curIm = curRe * wIm + curIm * wRe;
                    curRe = novoRe;
                }
            }
        }
    }
}

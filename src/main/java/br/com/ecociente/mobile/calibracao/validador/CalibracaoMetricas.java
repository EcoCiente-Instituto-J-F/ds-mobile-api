package br.com.ecociente.mobile.calibracao.validador;

import java.util.List;
import br.com.ecociente.mobile.core.domain.validador.Categorias;

public final class CalibracaoMetricas {

    public record Resultado(String real, String detectada, double probabilidade) {
    }

    private CalibracaoMetricas() {
    }

    public static double[] taxas(List<Resultado> resultados, double limiar) {
        long residuos = resultados.stream().filter(r -> Categorias.porSlug(r.real()).isPresent()).count();
        long corretas = resultados.stream().filter(r -> Categorias.porSlug(r.real()).isPresent()).filter(r -> r.detectada().equals(r.real()) && r.probabilidade() >= limiar).count();
        long erradas = resultados.stream().filter(r -> Categorias.porSlug(r.detectada()).isPresent() && !r.detectada().equals(r.real())).filter(r -> r.probabilidade() >= limiar).count();
        return new double[] { 100.0 * corretas / Math.max(residuos, 1), 100.0 * erradas / Math.max(resultados.size(), 1) };
    }

    public static double[] taxasDaCategoria(List<Resultado> resultados, String categoria, double limiar) {
        long dela = resultados.stream().filter(r -> r.real().equals(categoria)).count();
        long deFora = resultados.size() - dela;
        long corretas = resultados.stream().filter(r -> r.real().equals(categoria) && r.detectada().equals(categoria) && r.probabilidade() >= limiar).count();
        long erradas = resultados.stream().filter(r -> !r.real().equals(categoria) && r.detectada().equals(categoria) && r.probabilidade() >= limiar).count();
        return new double[] { 100.0 * corretas / Math.max(dela, 1), 100.0 * erradas / Math.max(deFora, 1) };
    }
}

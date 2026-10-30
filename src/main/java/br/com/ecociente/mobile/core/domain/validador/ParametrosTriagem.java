package br.com.ecociente.mobile.core.domain.validador;

import java.util.Map;

/**
 * Limiares da triagem. limiarRevisao nulo = sem zona cinza: a foto ou passa ou não passa.
 */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ParametrosTriagem(double limiarPadrao, Map<String, Double> limiaresPorCategoria, Double limiarRevisao) {

  public double limiarPara(String categoria) {
    return limiaresPorCategoria.getOrDefault(categoria, limiarPadrao);
  }
}

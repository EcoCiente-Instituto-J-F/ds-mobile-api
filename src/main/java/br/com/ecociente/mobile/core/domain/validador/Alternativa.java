package br.com.ecociente.mobile.core.domain.validador;

/** Uma das outras classes que o modelo considerou para a foto, com confiança de 0 a 100. */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Alternativa(String categoria, double confianca) {
}

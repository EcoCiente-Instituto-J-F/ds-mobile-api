package br.com.ecociente.mobile.core.domain.validador;

/** Classe do modelo com a probabilidade de 0 a 1. */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ClasseProbabilidade(String classe, double probabilidade) {
}

package br.com.ecociente.mobile.core.domain.validador;

@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ResultadoVoto(int saldoConfianca, boolean pontuacaoAtiva) {
}

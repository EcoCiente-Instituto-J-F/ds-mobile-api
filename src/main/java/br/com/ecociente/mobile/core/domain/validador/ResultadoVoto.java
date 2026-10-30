package br.com.ecociente.mobile.core.domain.validador;

/** Saldo de confiança da postagem depois do voto e se a pontuação já está ativa. */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ResultadoVoto(int saldoConfianca, boolean pontuacaoAtiva) {
}

package br.com.ecociente.mobile.core.domain.educacao;

public record ResultadoPontuacaoQuiz(
    Status status,
    Integer pontos,
    Long movimentacaoId,
    Boolean rankingSincronizado) {

  public enum Status {
    CONFIRMADO,
    PENDENTE,
    NAO_APLICAVEL,
    FALHA
  }

  public static ResultadoPontuacaoQuiz pendente() {
    return new ResultadoPontuacaoQuiz(
        Status.PENDENTE, null, null, null);
  }

  public static ResultadoPontuacaoQuiz naoAplicavel() {
    return new ResultadoPontuacaoQuiz(
        Status.NAO_APLICAVEL, 0, null, null);
  }

  public static ResultadoPontuacaoQuiz falha() {
    return new ResultadoPontuacaoQuiz(
        Status.FALHA, null, null, null);
  }
}
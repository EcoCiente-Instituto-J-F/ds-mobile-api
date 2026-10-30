package br.com.ecociente.mobile.core.domain.educacao;

public record ItemRevisaoQuiz(
    Integer perguntaId,
    String enunciado,
    Integer ordem,
    Alternativa escolhida,
    Alternativa correta,
    boolean acertouNaTentativa) {

  public record Alternativa(
      Integer id,
      String texto) {
  }
}
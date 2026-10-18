package br.com.ecociente.mobile.entrypoint.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record RevisaoTentativaQuizResponseDto(
    Integer tentativaId,
    Integer quizId,
    BigDecimal nota,
    int totalPerguntas,
    int acertos,
    boolean aprovado,
    OffsetDateTime concluidoEm,
    boolean gabaritoAtual,
    List<PerguntaRevisaoResponseDto> perguntas) {

  public record PerguntaRevisaoResponseDto(
      Integer perguntaId,
      String enunciado,
      Integer ordem,
      AlternativaRevisaoResponseDto respostaEscolhida,
      AlternativaRevisaoResponseDto respostaCorreta,
      boolean acertouNaTentativa,
      String explicacao) {
  }

  public record AlternativaRevisaoResponseDto(
      Integer id,
      String texto) {
  }
}
package br.com.ecociente.mobile.core.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ResultadoTentativaQuiz(
    Integer tentativaId,
    Integer quizId,
    BigDecimal nota,
    int totalPerguntas,
    int acertos,
    boolean aprovado,
    OffsetDateTime concluidoEm) {
}
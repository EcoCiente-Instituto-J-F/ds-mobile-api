package br.com.ecociente.mobile.core.domain.educacao;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ResumoTentativaQuiz(
    Integer tentativaId,
    Integer quizId,
    String titulo,
    BigDecimal nota,
    boolean aprovado,
    OffsetDateTime concluidoEm) {
}

package br.com.ecociente.mobile.core.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// Tentativa concluída de um quiz, para o histórico do usuário. `nota` vai de 0 a 100.
public record ResumoTentativaQuiz(
    Integer tentativaId,
    Integer quizId,
    String titulo,
    BigDecimal nota,
    boolean aprovado,
    OffsetDateTime concluidoEm) {
}

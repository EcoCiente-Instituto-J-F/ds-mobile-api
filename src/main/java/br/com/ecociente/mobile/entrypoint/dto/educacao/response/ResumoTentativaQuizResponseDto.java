package br.com.ecociente.mobile.entrypoint.dto.educacao.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ResumoTentativaQuizResponseDto(
    Integer tentativaId,
    Integer quizId,
    String titulo,
    BigDecimal nota,
    boolean aprovado,
    OffsetDateTime concluidoEm) {
}

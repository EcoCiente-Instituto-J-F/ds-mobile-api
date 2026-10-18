package br.com.ecociente.mobile.core.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TentativaQuizEstado(
    Integer id,
    Integer usuarioId,
    Integer quizId,
    BigDecimal nota,
    boolean aprovado,
    OffsetDateTime concluidoEm
) {
}
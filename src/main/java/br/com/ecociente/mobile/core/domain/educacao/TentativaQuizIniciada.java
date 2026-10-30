package br.com.ecociente.mobile.core.domain.educacao;

import java.time.OffsetDateTime;

public record TentativaQuizIniciada(
    Integer tentativaId,
    OffsetDateTime iniciadoEm,
    Quiz quiz
) {
}
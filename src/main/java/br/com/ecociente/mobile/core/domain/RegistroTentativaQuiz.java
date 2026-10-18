package br.com.ecociente.mobile.core.domain;

import java.time.OffsetDateTime;

public record RegistroTentativaQuiz(
    Integer id,
    OffsetDateTime iniciadoEm
) {
}
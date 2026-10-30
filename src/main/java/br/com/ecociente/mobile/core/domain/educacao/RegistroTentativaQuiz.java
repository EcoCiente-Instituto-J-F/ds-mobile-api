package br.com.ecociente.mobile.core.domain.educacao;

import java.time.OffsetDateTime;

public record RegistroTentativaQuiz(
    Integer id,
    OffsetDateTime iniciadoEm
) {
}
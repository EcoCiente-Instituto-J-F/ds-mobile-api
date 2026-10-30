package br.com.ecociente.mobile.core.domain.calendario;

import java.time.OffsetDateTime;

public record NovaColeta(
    Integer condominioId,
    Integer cooperativaId,
    OffsetDateTime dataInicio,
    OffsetDateTime dataFim
) {
}
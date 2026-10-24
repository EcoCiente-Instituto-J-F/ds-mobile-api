package br.com.ecociente.mobile.core.domain;

import java.time.OffsetDateTime;

public record AvaliacaoColeta(
    Integer id,
    Integer agendamentoId,
    Integer visitaId,
    int nota,
    String comentario,
    OffsetDateTime avaliadoEm
) {
}

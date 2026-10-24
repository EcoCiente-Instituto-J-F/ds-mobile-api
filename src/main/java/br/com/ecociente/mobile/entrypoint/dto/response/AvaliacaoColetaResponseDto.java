package br.com.ecociente.mobile.entrypoint.dto.response;

import java.time.OffsetDateTime;

public record AvaliacaoColetaResponseDto(
    Integer id,
    Integer agendamentoId,
    Integer visitaId,
    int nota,
    String comentario,
    OffsetDateTime avaliadoEm) {
}

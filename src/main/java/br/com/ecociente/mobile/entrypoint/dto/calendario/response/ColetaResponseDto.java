package br.com.ecociente.mobile.entrypoint.dto.calendario.response;

import java.time.OffsetDateTime;

import br.com.ecociente.mobile.core.domain.calendario.StatusType;


public record ColetaResponseDto(
    Integer id,
    Integer condominioId,
    Integer cooperativaId,
    StatusType status,
    OffsetDateTime dataInicio,
    OffsetDateTime dataFim,
    boolean possuiRecorrencia,
    Integer solicitadoPorUsuarioId,
    String origemSolicitacao,
    Integer respondidoPorUsuarioId,
    OffsetDateTime respondidoEm
) {
}
package br.com.ecociente.mobile.entrypoint.dto.calendario.request;

import java.time.LocalDateTime;

import br.com.ecociente.mobile.core.domain.calendario.StatusType;

public record ListarAgendamentoRequestDto(
    @io.swagger.v3.oas.annotations.media.Schema(description = "Status usado para filtrar os agendamentos.", example = "CONFIRMADO")
    StatusType statusType,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Data inicial do período. Na criação de coleta, deve estar no futuro.", example = "2030-01-10T09:00:00")
    LocalDateTime dataInicio,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Data final do período; não pode ser anterior à data inicial.", example = "2030-01-10T11:00:00")
    LocalDateTime dataFim,

    @io.swagger.v3.oas.annotations.media.Schema(description = "ID numérico do condomínio existente no banco.", example = "1")
    Integer condominioId,

    @io.swagger.v3.oas.annotations.media.Schema(description = "ID numérico da cooperativa existente no banco.", example = "1")
    Integer cooperativaId,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Filtra agendamentos recorrentes ou não recorrentes.", example = "false")
    Boolean possuiRecorrencia
) {
  
}

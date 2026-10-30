package br.com.ecociente.mobile.entrypoint.dto.calendario.response;

import java.time.LocalDateTime;

import br.com.ecociente.mobile.core.domain.calendario.StatusType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
  name = "Agendamento",
  description = "Dados de um agendamento de coleta"
)
public record CalendarioResponseDto(
  @Schema(
    description = "Identificado do agendamento",
    example = "42"
  )
  Integer id,

  @Schema (
    description = "Identificador do condominio",
    example = "23"
  )
  Integer condominioId,

  @Schema(
    description = "Identificador da cooperativa",
    example = "21"
  )
  Integer cooperativaId,

  @Schema(
    description = "Data de inicio da coleta",
    example = "2020-08-15T08:00:00",
    type = "string",
    format = "data-time"
  )
  LocalDateTime dataInicio,

  @Schema(
    description = "Data de inicio da coleta",
    example = "2020-08-15T08:00:00",
    type = "string",
    format = "data-time"
  )
  LocalDateTime dataFim,

  @Schema(
    description = "Status atual do agendamento",
    example = "AGENDADO",
    allowableValues = {
      "AGENDADO",
      "CONFIRMADO",
      "RECUSADO",
      "CANCELADO",
      "REALIZADO"
    }
  )
  StatusType statusAgendamento,

  @Schema(
    description = "Indica se o agendamento possui recorrência",
    example = "false"
  )
  Boolean possuiRecorrencia,
  @Schema(
    description = "Quem pediu a coleta",
    example = "CONDOMINIO",
    allowableValues = {"CONDOMINIO", "COOPERATIVA"}
  )
  String origemSolicitacao,
  @Schema(
    description = "Usuário que pediu a coleta. Ele não pode responder nem cancelar o próprio pedido",
    example = "7"
  )
  Integer solicitadoPorUsuarioId,
  @Schema(
    description = "Nome da cooperativa",
    example = "Cooperativa Verde"
  )
  String cooperativaNome,
  @Schema(
    description = "Nome do condomínio",
    example = "Residencial Eco"
  )
  String condominioNome
) {
}

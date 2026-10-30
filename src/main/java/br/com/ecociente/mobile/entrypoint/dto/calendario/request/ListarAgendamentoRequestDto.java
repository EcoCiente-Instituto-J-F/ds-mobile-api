package br.com.ecociente.mobile.entrypoint.dto.calendario.request;

import java.time.LocalDateTime;

import br.com.ecociente.mobile.core.domain.calendario.StatusType;

public record ListarAgendamentoRequestDto(
  StatusType statusType, 
  LocalDateTime dataInicio,
  LocalDateTime dataFim, 
  Integer condominioId,
  Integer cooperativaId, 
  Boolean possuiRecorrencia) {
  
}

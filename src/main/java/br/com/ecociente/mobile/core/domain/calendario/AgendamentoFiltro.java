package br.com.ecociente.mobile.core.domain.calendario;

import java.time.LocalDateTime;

public record AgendamentoFiltro(
  StatusType status,
  LocalDateTime dataInicio,
  LocalDateTime dataFim,
  Integer condominioId,
  Integer cooperativaId,
  Boolean possuiRecorrencia
) {
}
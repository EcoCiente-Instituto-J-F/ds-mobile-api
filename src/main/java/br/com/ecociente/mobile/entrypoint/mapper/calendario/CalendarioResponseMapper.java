package br.com.ecociente.mobile.entrypoint.mapper.calendario;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;
import br.com.ecociente.mobile.entrypoint.dto.calendario.response.CalendarioResponseDto;

@Component
public class CalendarioResponseMapper {

  public CalendarioResponseDto toResponseDto(AgendamentoColeta domain) {

    if (domain == null) {
      return null;
    }

    return new CalendarioResponseDto(
        domain.getId(),
        domain.getCondominioId(),
        domain.getCooperativaId(),
        domain.getDataInicio(),
        domain.getDataFim(),
        domain.getStatusAgendamento(),
        domain.getPossuiRecorrencia(),
        domain.getOrigemSolicitacao(),
        domain.getSolicitadoPorUsuarioId(),
        domain.getCooperativaNome(),
        domain.getCondominioNome());

  }
}

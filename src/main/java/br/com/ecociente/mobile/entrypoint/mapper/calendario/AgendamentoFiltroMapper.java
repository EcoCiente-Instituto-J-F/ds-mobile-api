package br.com.ecociente.mobile.entrypoint.mapper.calendario;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoFiltro;
import br.com.ecociente.mobile.core.domain.calendario.StatusType;

@Component
public class AgendamentoFiltroMapper {

  public AgendamentoFiltro toDomain(
    StatusType statusType,
    LocalDateTime dataInicio,
    LocalDateTime dataFim,
    Integer condominioId,
    Integer cooperativaId,
    Boolean possuiRecorrencia
  ){
    return new AgendamentoFiltro(
      statusType,
      dataInicio,
      dataFim,
      condominioId,
      cooperativaId,
      possuiRecorrencia
    );
  }
  
}

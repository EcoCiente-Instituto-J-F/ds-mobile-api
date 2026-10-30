package br.com.ecociente.mobile.dataprovider.mapper.calendario;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;
import br.com.ecociente.mobile.core.domain.calendario.StatusType;
import br.com.ecociente.mobile.dataprovider.entity.AgendamentoColetaEntity;

@Component
public class AgendamentoColetaMapper {

  public AgendamentoColeta toDomain(AgendamentoColetaEntity entity) {

    if (entity == null) {
      return null;
    }

    return AgendamentoColeta.builder()
        .id(entity.getId())
        .condominioId(entity.getCondominioId())
        .cooperativaId(entity.getCooperativaId())
        .statusAgendamento(StatusType.valueOf(entity.getStatusAgendamento().getNomeStatus().toUpperCase()))
        .dataInicio(entity.getDataInicio())
        .dataFim(entity.getDataFim())
        .possuiRecorrencia(entity.getPossuiRecorrencia())
        .origemSolicitacao(entity.getOrigemSolicitacao())
        .solicitadoPorUsuarioId(entity.getSolicitadoPorUsuarioId())
        .build();
  }

  public AgendamentoColetaEntity toEntity(AgendamentoColeta domain) {

    if (domain == null) {
      return null;
    }

    return AgendamentoColetaEntity.builder()
        .id(domain.getId())
        .condominioId(domain.getCondominioId())
        .cooperativaId(domain.getCooperativaId())
        .dataInicio(domain.getDataInicio())
        .dataFim(domain.getDataFim())
        .possuiRecorrencia(domain.getPossuiRecorrencia())
        .origemSolicitacao(domain.getOrigemSolicitacao())
        .solicitadoPorUsuarioId(domain.getSolicitadoPorUsuarioId())
        .build();
  }

}

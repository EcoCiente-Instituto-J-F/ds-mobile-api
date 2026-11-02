package br.com.ecociente.mobile.core.service.calendario;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.calendario.PaginaCalendario;
import br.com.ecociente.mobile.core.domain.calendario.PaginacaoCalendario;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;
import br.com.ecociente.mobile.core.domain.calendario.AgendamentoFiltro;
import br.com.ecociente.mobile.core.domain.calendario.Perfil;
import br.com.ecociente.mobile.core.gateway.calendario.AgendamentoConsultaGateway;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConsultaSindico implements ConsultaAgendamentoPorPerfil{

  private final AgendamentoConsultaGateway agendamentoConsultaGateway;

  @Override
  public Perfil perfilSuportado() {
    return Perfil.SINDICO;
  }
  
  @Override
  public PaginaCalendario<AgendamentoColeta> listar(Integer usuarioId, AgendamentoFiltro filtro, PaginacaoCalendario PaginacaoCalendario ){
    return agendamentoConsultaGateway.buscarPorSindico(usuarioId, filtro, PaginacaoCalendario);
  }

  @Override
  public Optional<AgendamentoColeta> buscarProximo(Integer usuarioId, AgendamentoFiltro filtro){
    return agendamentoConsultaGateway.buscarProximoAgendamentoPorSindico(usuarioId, filtro);
  }
}

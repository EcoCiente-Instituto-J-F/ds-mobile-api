package br.com.ecociente.mobile.core.gateway.calendario;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;
import br.com.ecociente.mobile.core.domain.calendario.AgendamentoFiltro;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.calendario.PaginaCalendario;
import br.com.ecociente.mobile.core.domain.calendario.PaginacaoCalendario;

public interface AgendamentoConsultaGateway {
  PaginaCalendario<AgendamentoColeta> buscarPorSindico(Integer usuarioId, AgendamentoFiltro filtro, PaginacaoCalendario PaginacaoCalendario);
  PaginaCalendario<AgendamentoColeta> buscarPorCooperativa(Integer usuarioId, AgendamentoFiltro filtro, PaginacaoCalendario PaginacaoCalendario);
  Optional<AgendamentoColeta> buscarProximoAgendamentoPorSindico(Integer usuarioId,AgendamentoFiltro filtro);
  Optional<AgendamentoColeta> buscarProximoAgendamentoPorCooperativa(Integer usuarioId,AgendamentoFiltro filtro);
}

package br.com.ecociente.mobile.core.gateway.calendario;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;
import br.com.ecociente.mobile.core.domain.calendario.AgendamentoFiltro;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AgendamentoConsultaGateway {
  Page<AgendamentoColeta> buscarPorSindico(Integer usuarioId, AgendamentoFiltro filtro, Pageable pageable);
  Page<AgendamentoColeta> buscarPorCooperativa(Integer usuarioId, AgendamentoFiltro filtro, Pageable pageable);
  Optional<AgendamentoColeta> buscarProximoAgendamentoPorSindico(Integer usuarioId,AgendamentoFiltro filtro);
  Optional<AgendamentoColeta> buscarProximoAgendamentoPorCooperativa(Integer usuarioId,AgendamentoFiltro filtro);
}

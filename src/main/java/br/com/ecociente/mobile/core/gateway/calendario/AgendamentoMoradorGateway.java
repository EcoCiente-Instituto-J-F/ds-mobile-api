package br.com.ecociente.mobile.core.gateway.calendario;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.calendario.PaginaCalendario;
import br.com.ecociente.mobile.core.domain.calendario.PaginacaoCalendario;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;
import br.com.ecociente.mobile.core.domain.calendario.AgendamentoFiltro;

public interface AgendamentoMoradorGateway {

  PaginaCalendario<AgendamentoColeta> listar(
      Integer usuarioId,
      AgendamentoFiltro filtro,
      PaginacaoCalendario PaginacaoCalendario);

  Optional<AgendamentoColeta> buscarProximo(
      Integer usuarioId,
      AgendamentoFiltro filtro);
}
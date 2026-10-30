package br.com.ecociente.mobile.core.gateway.calendario;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;
import br.com.ecociente.mobile.core.domain.calendario.AgendamentoFiltro;

public interface AgendamentoMoradorGateway {

  Page<AgendamentoColeta> listar(
      Integer usuarioId,
      AgendamentoFiltro filtro,
      Pageable pageable);

  Optional<AgendamentoColeta> buscarProximo(
      Integer usuarioId,
      AgendamentoFiltro filtro);
}
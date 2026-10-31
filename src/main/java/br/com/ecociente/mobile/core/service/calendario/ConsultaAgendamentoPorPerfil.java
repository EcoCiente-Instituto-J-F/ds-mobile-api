package br.com.ecociente.mobile.core.service.calendario;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.calendario.PaginaCalendario;
import br.com.ecociente.mobile.core.domain.calendario.PaginacaoCalendario;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;
import br.com.ecociente.mobile.core.domain.calendario.AgendamentoFiltro;
import br.com.ecociente.mobile.core.domain.calendario.Perfil;

public interface ConsultaAgendamentoPorPerfil {
  Perfil perfilSuportado();

  PaginaCalendario<AgendamentoColeta> listar(
    Integer usuarioId, 
    AgendamentoFiltro filtro, 
    PaginacaoCalendario PaginacaoCalendario);

  Optional<AgendamentoColeta> buscarProximo(
    Integer usuarioId, 
    AgendamentoFiltro filtro);
}

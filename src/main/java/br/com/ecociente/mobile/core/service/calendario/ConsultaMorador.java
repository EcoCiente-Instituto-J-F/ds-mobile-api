package br.com.ecociente.mobile.core.service.calendario;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.calendario.PaginaCalendario;
import br.com.ecociente.mobile.core.domain.calendario.PaginacaoCalendario;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;
import br.com.ecociente.mobile.core.domain.calendario.AgendamentoFiltro;
import br.com.ecociente.mobile.core.domain.calendario.Perfil;
import br.com.ecociente.mobile.core.gateway.calendario.AgendamentoMoradorGateway;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaMorador implements ConsultaAgendamentoPorPerfil {

  private final AgendamentoMoradorGateway gateway;

  @Override
  public Perfil perfilSuportado() {
    return Perfil.MORADOR;
  }

  @Override
  public PaginaCalendario<AgendamentoColeta> listar(
      Integer usuarioId,
      AgendamentoFiltro filtro,
      PaginacaoCalendario PaginacaoCalendario) {

    return gateway.listar(usuarioId, filtro, PaginacaoCalendario);
  }

  @Override
  public Optional<AgendamentoColeta> buscarProximo(
      Integer usuarioId,
      AgendamentoFiltro filtro) {

    return gateway.buscarProximo(usuarioId, filtro);
  }
}
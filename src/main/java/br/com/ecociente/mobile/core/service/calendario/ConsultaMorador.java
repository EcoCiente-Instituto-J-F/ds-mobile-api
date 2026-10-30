package br.com.ecociente.mobile.core.service.calendario;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
  public Page<AgendamentoColeta> listar(
      Integer usuarioId,
      AgendamentoFiltro filtro,
      Pageable pageable) {

    return gateway.listar(usuarioId, filtro, pageable);
  }

  @Override
  public Optional<AgendamentoColeta> buscarProximo(
      Integer usuarioId,
      AgendamentoFiltro filtro) {

    return gateway.buscarProximo(usuarioId, filtro);
  }
}
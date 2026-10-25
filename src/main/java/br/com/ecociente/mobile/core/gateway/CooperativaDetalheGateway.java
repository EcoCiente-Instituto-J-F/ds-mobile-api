package br.com.ecociente.mobile.core.gateway;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.Cooperativa;

public interface CooperativaDetalheGateway {

  boolean sindicoTemCondominioAtivo(
      Integer usuarioId);

  Optional<Cooperativa> buscarVisivelAoSindico(
      Integer usuarioId,
      Integer cooperativaId);
}
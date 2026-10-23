package br.com.ecociente.mobile.core.gateway;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.DashboardSindico;

public interface DashboardSindicoGateway {

  Optional<DashboardSindico> buscarPorCondominio(
      Integer condominioId);
}

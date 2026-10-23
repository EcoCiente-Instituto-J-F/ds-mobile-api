package br.com.ecociente.mobile.core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.DashboardSindico;
import br.com.ecociente.mobile.core.exception.DashboardSindicoException;
import br.com.ecociente.mobile.core.exception.DashboardSindicoException.Tipo;
import br.com.ecociente.mobile.core.gateway.DashboardSindicoGateway;

/** Só o síndico responsável vê o dashboard do condomínio. */
@Service
public class ConsultarDashboardSindicoService {

  private final DashboardSindicoGateway gateway;

  public ConsultarDashboardSindicoService(DashboardSindicoGateway gateway) {
    this.gateway = gateway;
  }

  @Transactional(readOnly = true)
  public DashboardSindico executar(
      Integer usuarioId,
      Integer condominioId) {
    if (usuarioId == null || usuarioId <= 0) {
      throw new DashboardSindicoException(
          Tipo.NAO_PERMITIDO,
          "USUARIO_INVALIDO",
          "Usuário inválido");
    }

    if (condominioId == null || condominioId <= 0) {
      throw new DashboardSindicoException(
          Tipo.DADOS_INVALIDOS,
          "CONDOMINIO_ID_INVALIDO",
          "Informe um condomínio válido");
    }

    var dashboard = gateway
        .buscarPorCondominio(condominioId)
        .orElseThrow(() -> new DashboardSindicoException(
            Tipo.NAO_ENCONTRADO,
            "CONDOMINIO_NAO_ENCONTRADO",
            "Condomínio não encontrado"));

    if (!dashboard.eDoSindico(usuarioId)) {
      throw new DashboardSindicoException(
          Tipo.NAO_PERMITIDO,
          "DASHBOARD_NAO_PERMITIDO",
          "Somente o síndico responsável pode ver o dashboard deste condomínio");
    }

    return dashboard;
  }
}

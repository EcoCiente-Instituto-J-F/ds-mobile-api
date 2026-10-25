package br.com.ecociente.mobile.core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.Cooperativa;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException.Tipo;
import br.com.ecociente.mobile.core.gateway.CooperativaDetalheGateway;

@Service
public class ConsultarCooperativaService {

  private final CooperativaDetalheGateway gateway;

  public ConsultarCooperativaService(
      CooperativaDetalheGateway gateway) {
    this.gateway = gateway;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Cooperativa executar(
      Integer usuarioId,
      Integer cooperativaId) {

    if (usuarioId == null || usuarioId <= 0) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "USUARIO_INVALIDO",
          "Usuário inválido");
    }

    if (cooperativaId == null || cooperativaId <= 0) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "COOPERATIVA_ID_INVALIDO",
          "Informe uma cooperativa válida");
    }

    if (!gateway.sindicoTemCondominioAtivo(usuarioId)) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "SINDICO_SEM_CONDOMINIO",
          "Apenas o síndico responsável por um condomínio ativo pode consultar");
    }

    return gateway.buscarVisivelAoSindico(
        usuarioId,
        cooperativaId)
        .orElseThrow(() -> erro(
            Tipo.NAO_ENCONTRADO,
            "COOPERATIVA_NAO_ENCONTRADA",
            "Cooperativa não encontrada ou indisponível para consulta"));
  }

  private ConsultaCooperativaException erro(
      Tipo tipo,
      String codigo,
      String mensagem) {
    return new ConsultaCooperativaException(
        tipo,
        codigo,
        mensagem);
  }
}
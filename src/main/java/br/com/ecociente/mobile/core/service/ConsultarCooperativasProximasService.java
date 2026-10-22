package br.com.ecociente.mobile.core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.PaginaCooperativas;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException.Tipo;
import br.com.ecociente.mobile.core.gateway.CooperativaGateway;

/**
 * Cooperativas da mesma cidade e estado dos condomínios do síndico. O banco não guarda
 * coordenadas, então não há distância: o app usa o endereço em texto para abrir o mapa.
 */
@Service
public class ConsultarCooperativasProximasService {

  private final CooperativaGateway gateway;

  public ConsultarCooperativasProximasService(
      CooperativaGateway gateway) {
    this.gateway = gateway;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PaginaCooperativas executar(
      Integer usuarioId,
      int pagina,
      int tamanho) {
    if (usuarioId == null || usuarioId <= 0) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "USUARIO_INVALIDO",
          "Usuário inválido");
    }

    if (pagina < 0 || tamanho < 1 || tamanho > 100) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "PAGINACAO_INVALIDA",
          "A página deve ser maior ou igual a zero e o tamanho entre 1 e 100");
    }

    if (!gateway.sindicoTemCondominioAtivo(usuarioId)) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "SINDICO_SEM_CONDOMINIO",
          "Apenas o síndico de um condomínio ativo pode consultar as cooperativas");
    }

    long total = gateway.contarProximas(usuarioId);

    long deslocamento = (long) pagina * tamanho;

    var cooperativas = gateway.listarProximas(
        usuarioId,
        deslocamento,
        tamanho);

    long totalPaginas = total / tamanho
        + (total % tamanho == 0 ? 0 : 1);

    return new PaginaCooperativas(
        total,
        pagina,
        tamanho,
        totalPaginas,
        cooperativas);
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

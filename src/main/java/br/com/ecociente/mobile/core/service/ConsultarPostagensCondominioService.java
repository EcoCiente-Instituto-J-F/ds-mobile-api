package br.com.ecociente.mobile.core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.PaginaPostagens;
import br.com.ecociente.mobile.core.exception.PostagemException;
import br.com.ecociente.mobile.core.exception.PostagemException.Tipo;
import br.com.ecociente.mobile.core.gateway.PostagemGateway;

/** Feed de fotos de descarte do condomínio, só para quem tem vínculo aprovado nele. */
@Service
public class ConsultarPostagensCondominioService {

  private final PostagemGateway gateway;

  public ConsultarPostagensCondominioService(PostagemGateway gateway) {
    this.gateway = gateway;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PaginaPostagens executar(
      Integer usuarioId,
      Integer condominioId,
      int pagina,
      int tamanho) {
    if (usuarioId == null || usuarioId <= 0) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "USUARIO_INVALIDO",
          "Usuário inválido");
    }

    if (condominioId == null || condominioId <= 0) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "CONDOMINIO_ID_INVALIDO",
          "Informe um condomínio válido");
    }

    if (pagina < 0 || tamanho < 1 || tamanho > 100) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "PAGINACAO_INVALIDA",
          "A página deve ser maior ou igual a zero e o tamanho entre 1 e 100");
    }

    gateway
        .buscarVinculoAprovado(usuarioId, condominioId)
        .orElseThrow(() -> erro(
            Tipo.NAO_PERMITIDO,
            "SEM_VINCULO_APROVADO",
            "Você não tem vínculo aprovado com este condomínio"));

    long total = gateway.contarDoCondominio(condominioId);

    long deslocamento = (long) pagina * tamanho;

    var postagens = gateway.listarDoCondominio(
        condominioId,
        deslocamento,
        tamanho);

    long totalPaginas = total / tamanho
        + (total % tamanho == 0 ? 0 : 1);

    return new PaginaPostagens(
        total,
        pagina,
        tamanho,
        totalPaginas,
        postagens);
  }

  private PostagemException erro(
      Tipo tipo,
      String codigo,
      String mensagem) {
    return new PostagemException(
        tipo,
        codigo,
        mensagem);
  }
}

package br.com.ecociente.mobile.core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.PaginaAvaliacoesCooperativa;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException.Tipo;
import br.com.ecociente.mobile.core.gateway.AvaliacaoCooperativaGateway;

@Service
public class ConsultarAvaliacoesCooperativaService {

  private final AvaliacaoCooperativaGateway gateway;

  public ConsultarAvaliacoesCooperativaService(
      AvaliacaoCooperativaGateway gateway) {
    this.gateway = gateway;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PaginaAvaliacoesCooperativa executar(
      Integer usuarioId,
      Integer cooperativaId,
      int pagina,
      int tamanho) {
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

    if (pagina < 0 || tamanho < 1 || tamanho > 100) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "PAGINACAO_INVALIDA",
          "A página deve ser maior ou igual a zero e o tamanho entre 1 e 100");
    }

    Integer responsavelId = gateway
        .buscarResponsavelAtivo(cooperativaId)
        .orElseThrow(() -> erro(
            Tipo.NAO_ENCONTRADO,
            "COOPERATIVA_NAO_ENCONTRADA",
            "Cooperativa não encontrada ou sem responsável ativo"));

    if (!usuarioId.equals(responsavelId)) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "AVALIACOES_NAO_PERMITIDAS",
          "Você não pode consultar as avaliações desta cooperativa");
    }

    var resumo = gateway.consultarResumo(cooperativaId);

    long deslocamento = (long) pagina * tamanho;

    var avaliacoes = gateway.listar(
        cooperativaId,
        deslocamento,
        tamanho);

    long totalPaginas = resumo.total() / tamanho
        + (resumo.total() % tamanho == 0 ? 0 : 1);

    return new PaginaAvaliacoesCooperativa(
        cooperativaId,
        resumo.media(),
        resumo.total(),
        pagina,
        tamanho,
        totalPaginas,
        avaliacoes);
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
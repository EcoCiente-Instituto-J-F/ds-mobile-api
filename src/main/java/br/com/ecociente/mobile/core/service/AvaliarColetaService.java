package br.com.ecociente.mobile.core.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.AvaliacaoColeta;
import br.com.ecociente.mobile.core.exception.AvaliacaoColetaException;
import br.com.ecociente.mobile.core.exception.AvaliacaoColetaException.Tipo;
import br.com.ecociente.mobile.core.gateway.AvaliacaoColetaGateway;

/**
 * O síndico do condomínio avalia uma coleta realizada. A nota entra na média da
 * cooperativa, que a consulta de cooperativas já calcula a partir das avaliações.
 */
@Service
public class AvaliarColetaService {

  private static final int COMENTARIO_MAXIMO = 500;

  private final AvaliacaoColetaGateway gateway;

  public AvaliarColetaService(AvaliacaoColetaGateway gateway) {
    this.gateway = gateway;
  }

  @Transactional
  public AvaliacaoColeta executar(
      Integer usuarioId,
      Integer agendamentoId,
      Integer nota,
      String comentario) {
    if (usuarioId == null || usuarioId <= 0) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "USUARIO_INVALIDO",
          "Usuário inválido");
    }

    if (agendamentoId == null || agendamentoId <= 0) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "AGENDAMENTO_ID_INVALIDO",
          "Informe um agendamento válido");
    }

    if (nota == null || nota < 1 || nota > 5) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "NOTA_INVALIDA",
          "A nota deve ser de 1 a 5");
    }

    String observacao = comentario == null || comentario.isBlank()
        ? null
        : comentario.trim();

    if (observacao != null && observacao.length() > COMENTARIO_MAXIMO) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "COMENTARIO_INVALIDO",
          "O comentário deve ter até 500 caracteres");
    }

    var agendamento = gateway
        .buscarAgendamento(agendamentoId)
        .orElseThrow(() -> erro(
            Tipo.NAO_ENCONTRADO,
            "AGENDAMENTO_NAO_ENCONTRADO",
            "Agendamento não encontrado"));

    if (!usuarioId.equals(agendamento.sindicoUsuarioId())) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "AVALIACAO_NAO_PERMITIDA",
          "Somente o síndico responsável pelo condomínio pode avaliar a coleta");
    }

    if (!agendamento.realizado()) {
      throw erro(
          Tipo.CONFLITO,
          "COLETA_NAO_REALIZADA",
          "Só é possível avaliar uma coleta realizada");
    }

    Integer visitaId = gateway.obterOuCriarVisitaRealizada(agendamentoId);

    if (gateway.visitaJaAvaliada(visitaId)) {
      throw jaAvaliada();
    }

    try {
      return gateway.registrar(
          agendamentoId,
          visitaId,
          usuarioId,
          nota,
          observacao);
    } catch (DuplicateKeyException exception) {
      throw jaAvaliada();
    }
  }

  private AvaliacaoColetaException jaAvaliada() {
    return erro(
        Tipo.CONFLITO,
        "AVALIACAO_JA_REGISTRADA",
        "Esta coleta já foi avaliada");
  }

  private AvaliacaoColetaException erro(
      Tipo tipo,
      String codigo,
      String mensagem) {
    return new AvaliacaoColetaException(
        tipo,
        codigo,
        mensagem);
  }
}

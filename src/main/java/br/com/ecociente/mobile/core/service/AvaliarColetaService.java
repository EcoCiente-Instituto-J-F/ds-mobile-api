package br.com.ecociente.mobile.core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.AvaliacaoColeta;
import br.com.ecociente.mobile.core.domain.NovaAvaliacaoColeta;
import br.com.ecociente.mobile.core.exception.AvaliacaoColetaException;
import br.com.ecociente.mobile.core.exception.AvaliacaoColetaException.Tipo;
import br.com.ecociente.mobile.core.gateway.AvaliacaoColetaGateway;

/**
 * O síndico do condomínio avalia uma coleta realizada. A nota entra na média da
 * cooperativa, que a consulta de cooperativas já calcula a partir das avaliações.
 */
@Service
public class AvaliarColetaService {

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
    var avaliacao = NovaAvaliacaoColeta.de(
        usuarioId,
        agendamentoId,
        nota,
        comentario);

    var agendamento = gateway
        .buscarAgendamento(agendamentoId)
        .orElseThrow(() -> new AvaliacaoColetaException(
            Tipo.NAO_ENCONTRADO,
            "AGENDAMENTO_NAO_ENCONTRADO",
            "Agendamento não encontrado"));

    if (!agendamento.eDoSindico(usuarioId)) {
      throw new AvaliacaoColetaException(
          Tipo.NAO_PERMITIDO,
          "AVALIACAO_NAO_PERMITIDA",
          "Somente o síndico responsável pelo condomínio pode avaliar a coleta");
    }

    if (!agendamento.realizado()) {
      throw new AvaliacaoColetaException(
          Tipo.CONFLITO,
          "COLETA_NAO_REALIZADA",
          "Só é possível avaliar uma coleta realizada");
    }

    // A avaliação fica presa a uma visita; se ninguém registrou uma, ela nasce do agendamento.
    Integer visitaId = gateway
        .buscarVisitaRealizada(agendamentoId)
        .orElseGet(() -> gateway.criarVisitaRealizada(agendamentoId));

    if (gateway.visitaJaAvaliada(visitaId)) {
      throw AvaliacaoColetaException.jaAvaliada();
    }

    return gateway.registrar(visitaId, avaliacao);
  }
}

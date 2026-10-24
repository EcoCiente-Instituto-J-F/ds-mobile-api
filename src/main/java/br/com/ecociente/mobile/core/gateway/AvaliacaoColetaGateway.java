package br.com.ecociente.mobile.core.gateway;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.AgendamentoAvaliavel;
import br.com.ecociente.mobile.core.domain.AvaliacaoColeta;
import br.com.ecociente.mobile.core.domain.NovaAvaliacaoColeta;

public interface AvaliacaoColetaGateway {

  Optional<AgendamentoAvaliavel> buscarAgendamento(
      Integer agendamentoId);

  /** Visita realizada mais recente do agendamento, se alguém registrou uma. */
  Optional<Integer> buscarVisitaRealizada(
      Integer agendamentoId);

  /** Cria a visita realizada a partir das datas do agendamento. */
  Integer criarVisitaRealizada(
      Integer agendamentoId);

  boolean visitaJaAvaliada(
      Integer visitaId);

  /**
   * Grava a avaliação. Se a visita já tiver uma, lança
   * {@link br.com.ecociente.mobile.core.exception.AvaliacaoColetaException#jaAvaliada()}.
   */
  AvaliacaoColeta registrar(
      Integer visitaId,
      NovaAvaliacaoColeta avaliacao);
}

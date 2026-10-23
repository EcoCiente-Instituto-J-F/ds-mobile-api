package br.com.ecociente.mobile.core.gateway;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.AgendamentoAvaliavel;
import br.com.ecociente.mobile.core.domain.AvaliacaoColeta;

public interface AvaliacaoColetaGateway {

  Optional<AgendamentoAvaliavel> buscarAgendamento(
      Integer agendamentoId);

  /**
   * A avaliação fica presa a uma visita. Devolve a visita realizada mais recente do
   * agendamento ou, se ninguém registrou uma, cria a partir das datas do agendamento.
   */
  Integer obterOuCriarVisitaRealizada(
      Integer agendamentoId);

  boolean visitaJaAvaliada(
      Integer visitaId);

  AvaliacaoColeta registrar(
      Integer agendamentoId,
      Integer visitaId,
      Integer usuarioId,
      int nota,
      String comentario);
}

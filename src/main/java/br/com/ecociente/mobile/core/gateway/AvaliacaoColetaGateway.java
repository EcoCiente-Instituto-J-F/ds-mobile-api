package br.com.ecociente.mobile.core.gateway;

import java.util.Optional;
import br.com.ecociente.mobile.core.domain.AgendamentoAvaliavel;
import br.com.ecociente.mobile.core.domain.AvaliacaoColeta;
import br.com.ecociente.mobile.core.domain.NovaAvaliacaoColeta;

public interface AvaliacaoColetaGateway {

    Optional<AgendamentoAvaliavel> buscarAgendamento(Integer agendamentoId);

    Optional<Integer> buscarVisitaRealizada(Integer agendamentoId);

    Integer criarVisitaRealizada(Integer agendamentoId);

    boolean visitaJaAvaliada(Integer visitaId);

    AvaliacaoColeta registrar(Integer visitaId, NovaAvaliacaoColeta avaliacao);
}

package br.com.ecociente.mobile.core.gateway.calendario;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;

public interface AgendamentoComandoGateway {
  AgendamentoColeta salvar (AgendamentoColeta agendamentoColeta);
  AgendamentoColeta atualizar(Integer id, AgendamentoColeta agendamentoColeta);
  void deletar(Integer id);
  
}

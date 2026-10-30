package br.com.ecociente.mobile.core.exception.calendario;

public class AgendamentoNaoEncontradoException extends RuntimeException {
  public AgendamentoNaoEncontradoException(Integer agendamentoId) {
    super("Agendamento não encontrado: " + agendamentoId);
  }
  
}

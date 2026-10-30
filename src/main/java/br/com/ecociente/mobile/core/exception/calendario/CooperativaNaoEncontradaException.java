package br.com.ecociente.mobile.core.exception.calendario;

public class CooperativaNaoEncontradaException extends RuntimeException {
  public CooperativaNaoEncontradaException(Integer cooperativaId) {
    super("Cooperativa não encontrada: " + cooperativaId);
  } 
}

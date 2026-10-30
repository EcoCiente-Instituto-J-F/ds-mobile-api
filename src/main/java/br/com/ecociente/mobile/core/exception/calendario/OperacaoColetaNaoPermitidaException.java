package br.com.ecociente.mobile.core.exception.calendario;

public class OperacaoColetaNaoPermitidaException
    extends RuntimeException {

  public OperacaoColetaNaoPermitidaException(String mensagem) {
    super(mensagem);
  }
}
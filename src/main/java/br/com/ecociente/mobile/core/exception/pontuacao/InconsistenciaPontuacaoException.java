package br.com.ecociente.mobile.core.exception.pontuacao;

public class InconsistenciaPontuacaoException extends DomainException {

  public InconsistenciaPontuacaoException(String codigoErro, String message) {
    super(codigoErro, message);
  }
}
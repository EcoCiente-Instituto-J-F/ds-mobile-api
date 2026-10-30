package br.com.ecociente.mobile.core.exception.pontuacao;

public abstract class DomainException extends RuntimeException {

  private final String codigoErro;

  protected DomainException(String codigoErro, String message) {
    super(message);
    this.codigoErro = codigoErro;
  }

  public String getCodigoErro() {
    return codigoErro;
  }
}
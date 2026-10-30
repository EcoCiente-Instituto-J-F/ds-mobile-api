package br.com.ecociente.mobile.core.exception.cadastro;

public abstract class DomainException extends RuntimeException {
  private final String codigoErro;

  public DomainException(String codigoErro, String message){
    super(message);
    this.codigoErro=codigoErro;
  }
  public String getCodigoErro(){
    return codigoErro;
  }
  
}

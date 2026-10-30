package br.com.ecociente.mobile.core.exception.cadastro;

public class AlreadyExistsException extends DomainException {

  public AlreadyExistsException(String codigoErro, String message){
    super(codigoErro,message);
  }
}

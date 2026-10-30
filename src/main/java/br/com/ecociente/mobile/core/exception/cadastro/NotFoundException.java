package br.com.ecociente.mobile.core.exception.cadastro;

public class NotFoundException extends DomainException {

  public NotFoundException(String codigoErro, String message){
    super(codigoErro,message);

  }
  
}

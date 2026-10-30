package br.com.ecociente.mobile.core.exception.cadastro;

public class ApiException extends DomainException{

  public ApiException(String codigoErro, String message){
    super(codigoErro,message);
  }
  
}

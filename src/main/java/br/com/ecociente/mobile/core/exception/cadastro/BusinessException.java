package br.com.ecociente.mobile.core.exception.cadastro;

public class BusinessException extends DomainException {
  public BusinessException(String codigoErro, String message){
    super(codigoErro,message);
  }
  
}

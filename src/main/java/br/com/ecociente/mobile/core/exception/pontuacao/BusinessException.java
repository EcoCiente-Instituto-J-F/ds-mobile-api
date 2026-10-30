package br.com.ecociente.mobile.core.exception.pontuacao;

public class BusinessException extends DomainException {

  public BusinessException(String codigoErro, String message) {
    super(codigoErro, message);
  }
}
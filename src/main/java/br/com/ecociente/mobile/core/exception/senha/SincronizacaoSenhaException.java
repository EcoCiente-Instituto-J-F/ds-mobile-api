package br.com.ecociente.mobile.core.exception.senha;

public class SincronizacaoSenhaException extends RuntimeException {

  public SincronizacaoSenhaException(
      String mensagem,
      Throwable causa) {
    super(mensagem, causa);
  }
}
package br.com.ecociente.mobile.core.exception.validador;

/** Postagem inexistente: HTTP 404. */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String mensagem) {
    super(mensagem);
  }
}

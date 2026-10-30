package br.com.ecociente.mobile.core.exception.calendario;

public class CondominioNaoEncontradoException extends RuntimeException {
  public CondominioNaoEncontradoException(Integer condominioId) {
    super("Condomínio não encontrado: " + condominioId);
  }
}

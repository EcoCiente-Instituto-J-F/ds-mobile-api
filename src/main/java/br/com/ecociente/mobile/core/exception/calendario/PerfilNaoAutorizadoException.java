package br.com.ecociente.mobile.core.exception.calendario;

public class PerfilNaoAutorizadoException extends RuntimeException {
  public PerfilNaoAutorizadoException(String perfil) {
    super("Perfil não autorizado: " + perfil);
  }
}

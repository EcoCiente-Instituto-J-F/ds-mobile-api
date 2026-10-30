package br.com.ecociente.mobile.entrypoint.security;

import org.springframework.security.core.Authentication;

import br.com.ecociente.mobile.core.exception.validador.PermissaoNegadaException;

public final class ValidadorUsuario {

  private ValidadorUsuario() {
  }

  public static int id(
      Authentication authentication,
      Integer usuarioInformado) {

    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof Integer usuarioId)
        || usuarioId <= 0) {
      throw new PermissaoNegadaException(
          "Autenticação inválida.");
    }

    if (!usuarioId.equals(usuarioInformado)) {
      throw new PermissaoNegadaException(
          "O usuário informado deve corresponder ao usuário autenticado.");
    }

    return usuarioId;
  }
}
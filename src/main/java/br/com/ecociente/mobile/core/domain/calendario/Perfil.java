package br.com.ecociente.mobile.core.domain.calendario;

import java.util.Locale;

import br.com.ecociente.mobile.core.exception.calendario.PerfilNaoAutorizadoException;

public enum Perfil {

  SINDICO,
  COOPERATIVA,
  MORADOR;

  public static Perfil tipoPerfil(String perfil) {
    if (perfil == null || perfil.isBlank()) {
      throw new PerfilNaoAutorizadoException(perfil);
    }

    try {
      return Perfil.valueOf(
          perfil.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new PerfilNaoAutorizadoException(perfil);
    }
  }
}
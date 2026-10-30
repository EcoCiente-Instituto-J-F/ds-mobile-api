package br.com.ecociente.mobile.config.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import br.com.ecociente.mobile.core.domain.calendario.Perfil;
import br.com.ecociente.mobile.core.exception.calendario.PerfilNaoAutorizadoException;

public final class CalendarioUsuario {

  private CalendarioUsuario() {
  }

  public static Integer id(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof Integer usuarioId)
        || usuarioId <= 0) {
      throw new BadCredentialsException("Autenticação inválida.");
    }

    return usuarioId;
  }

  public static Perfil perfil(Authentication authentication) {
    id(authentication);

    var perfis = authentication.getAuthorities().stream()
        .map(authority -> authority.getAuthority())
        .filter(authority -> authority.equals("ROLE_SINDICO")
            || authority.equals("ROLE_COOPERATIVA")
            || authority.equals("ROLE_MORADOR"))
        .distinct()
        .toList();

    if (perfis.size() != 1) {
      throw new PerfilNaoAutorizadoException(
          "Perfil não autorizado para o calendário.");
    }

    return Perfil.tipoPerfil(
        perfis.getFirst().substring("ROLE_".length()));
  }
}
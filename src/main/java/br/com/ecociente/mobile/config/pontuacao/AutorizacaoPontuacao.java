
package br.com.ecociente.mobile.config.pontuacao;

import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.gateway.pontuacao.VinculoCondominialGateway;

import lombok.RequiredArgsConstructor;

@Component("autorizacaoPontuacao")
@RequiredArgsConstructor
public class AutorizacaoPontuacao {

  private final VinculoCondominialGateway vinculoGateway;

  public boolean podeConsultar(
      Authentication authentication,
      Integer usuarioId) {

    if (!autenticado(authentication)) {
      return false;
    }

    if (possuiPapel(authentication, "ROLE_ADMIN")) {
      return true;
    }

    Integer usuarioAutenticado = obterUsuarioId(authentication);

    return usuarioAutenticado != null
        && usuarioAutenticado.equals(usuarioId);
  }

  public boolean podeConsultarCondominio(
      Authentication authentication,
      Integer condominioId) {
    if (!autenticado(authentication) || condominioId == null) {
      return false;
    }

    if (possuiPapel(authentication, "ROLE_ADMIN")) {
      return true;
    }

    Integer usuarioId = obterUsuarioId(authentication);

    if (usuarioId == null) {
      return false;
    }

    if (possuiPapel(authentication, "ROLE_SINDICO")
        && vinculoGateway.ehSindico(usuarioId, condominioId)) {
      return true;
    }

    return vinculoGateway.possuiVinculoAtivo(
        usuarioId,
        condominioId);
  }

  public boolean podeConsultarTorre(
      Authentication authentication,
      Integer condominioId,
      Integer torreId) {

    return podeConsultarCondominio(
        authentication,
        condominioId)
        && vinculoGateway.torrePertenceCondominio(
            torreId,
            condominioId);
  }

  public boolean podeDecidirPostagem(
      Authentication authentication,
      Integer condominioId) {
    if (!autenticado(authentication) || condominioId == null) {
      return false;
    }

    if (possuiPapel(authentication, "ROLE_ADMIN")) {
      return true;
    }

    if (!possuiPapel(authentication, "ROLE_SINDICO")) {
      return false;
    }

    Integer usuarioId = obterUsuarioId(authentication);

    return usuarioId != null
        && vinculoGateway.ehSindico(usuarioId, condominioId);
  }

  private boolean autenticado(Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated();
  }

  private boolean possuiPapel(
      Authentication authentication,
      String papel) {

    return authentication.getAuthorities()
        .stream()
        .anyMatch(authority -> papel.equals(authority.getAuthority()));
  }

  private Integer obterUsuarioId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()) {
      return null;
    }

    if (authentication.getPrincipal() instanceof Integer usuarioId
        && usuarioId > 0) {
      return usuarioId;
    }

    return null;
  }
}
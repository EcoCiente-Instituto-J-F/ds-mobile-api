package br.com.ecociente.mobile.core.service.autenticacao;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Service;

import br.com.ecociente.mobile.core.domain.autenticacao.PerfilUsuarioType;
import br.com.ecociente.mobile.core.domain.autenticacao.Usuario;

@Service
public class ResolverPerfilAutenticacaoService {

  public PerfilUsuarioType perfil(Usuario usuario) {
    String tipo = normalizar(
        usuario.getTipoUsuario());

    if (tipo.equals("admin")
        || tipo.equals("administrador")
        || tipo.equals("administradora")) {
      return PerfilUsuarioType.ADMIN;
    }

    if (tipo.contains("cooperativa")) {
      return PerfilUsuarioType.COOPERATIVA;
    }

    if (tipo.contains("sindico")) {
      return PerfilUsuarioType.SINDICO;
    }

    if (tipo.contains("morador")) {
      return PerfilUsuarioType.MORADOR;
    }

    return PerfilUsuarioType.USUARIO_COMUM;
  }

  private String normalizar(String valor) {
    if (valor == null) {
      return "";
    }

    return Normalizer
        .normalize(valor, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .trim()
        .toLowerCase(Locale.ROOT);
  }
}
package br.com.ecociente.mobile.core.gateway.autenticacao;

import br.com.ecociente.mobile.core.domain.autenticacao.PerfilUsuarioType;
import br.com.ecociente.mobile.core.domain.autenticacao.Usuario;

public interface TokenProvaiderPort {
  String gerarToken(Usuario usuario, PerfilUsuarioType perfil);
  Long getExpiracaoSegundos();
}

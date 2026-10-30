package br.com.ecociente.mobile.core.gateway.autenticacao;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.autenticacao.Usuario;

public interface UsuarioGateway {
  Optional<Usuario> buscarPorEmail(String email);
  boolean existePorEmail(String email);
}

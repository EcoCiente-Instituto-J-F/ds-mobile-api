package br.com.ecociente.mobile.core.gateway.senha;

import br.com.ecociente.mobile.core.domain.senha.Usuario;

import java.util.Optional;

public interface UsuarioGateway {

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorId(Integer id);

    void atualizarSenha(Integer usuarioId, String senhaHash);

    Optional<Usuario> buscarAtivoPorId(Integer id);
}
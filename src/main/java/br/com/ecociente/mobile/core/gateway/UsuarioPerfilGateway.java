package br.com.ecociente.mobile.core.gateway;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.UsuarioPerfil;

public interface UsuarioPerfilGateway {

    Optional<UsuarioPerfil> buscarPorUsuarioId(Integer usuarioId);
}
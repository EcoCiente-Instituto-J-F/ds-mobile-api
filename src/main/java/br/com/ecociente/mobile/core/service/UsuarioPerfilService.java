package br.com.ecociente.mobile.core.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.UsuarioPerfil;
import br.com.ecociente.mobile.core.gateway.UsuarioPerfilGateway;

@Service
public class UsuarioPerfilService {

    private final UsuarioPerfilGateway gateway;

    public UsuarioPerfilService(UsuarioPerfilGateway gateway) {
        this.gateway = gateway;
    }

    @Transactional(readOnly = true)
    public Optional<UsuarioPerfil> buscarMeuPerfil(Integer usuarioId) {
        return gateway.buscarPorUsuarioId(usuarioId);
    }
}
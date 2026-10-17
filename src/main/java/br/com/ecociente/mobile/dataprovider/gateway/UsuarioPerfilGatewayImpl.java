package br.com.ecociente.mobile.dataprovider.gateway;

import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.UsuarioPerfil;
import br.com.ecociente.mobile.core.gateway.UsuarioPerfilGateway;
import br.com.ecociente.mobile.dataprovider.mapper.UsuarioPerfilPersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.UsuarioPerfilRepository;

@Component
public class UsuarioPerfilGatewayImpl implements UsuarioPerfilGateway {

    private final UsuarioPerfilRepository repository;
    private final UsuarioPerfilPersistenceMapper mapper;

    public UsuarioPerfilGatewayImpl(
        UsuarioPerfilRepository repository,
        UsuarioPerfilPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<UsuarioPerfil> buscarPorUsuarioId(Integer usuarioId) {
        return repository.buscarPorUsuarioId(usuarioId)
            .map(mapper::toDomain);
    }
}
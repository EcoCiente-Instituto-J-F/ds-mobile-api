package br.com.ecociente.mobile.dataprovider.mapper.senha;

import br.com.ecociente.mobile.core.domain.senha.Usuario;
import br.com.ecociente.mobile.dataprovider.entity.UsuarioEntity;

import org.springframework.stereotype.Component;

@Component
public class UsuarioSenhaPersistenceMapper {

    public Usuario toDomain(UsuarioEntity entity) {
        return Usuario.builder()
                .id(entity.getId())
                .email(entity.getEmailUsuario())
                .senha(entity.getSenhaHash())
                .build();
    }
}
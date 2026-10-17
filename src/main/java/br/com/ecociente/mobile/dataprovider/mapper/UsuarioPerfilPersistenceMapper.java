package br.com.ecociente.mobile.dataprovider.mapper;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ecociente.mobile.core.domain.UsuarioPerfil;

@Component
public class UsuarioPerfilPersistenceMapper {

    private final ObjectMapper objectMapper;

    public UsuarioPerfilPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public UsuarioPerfil toDomain(String json) {
        try {
            return objectMapper.readValue(json, UsuarioPerfil.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                "Não foi possível mapear o perfil do usuário",
                exception
            );
        }
    }
}
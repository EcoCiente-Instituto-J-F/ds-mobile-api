package br.com.ecociente.mobile.entrypoint.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.Curso;
import br.com.ecociente.mobile.entrypoint.dto.response.CursoResponseDto;

@Component
public class CursoMapper {

    public CursoResponseDto toResponse(Curso curso) {
        return new CursoResponseDto(
            curso.id(),
            curso.titulo(),
            curso.descricao()
        );
    }
}
package br.com.ecociente.mobile.entrypoint.mapper.educacao;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.educacao.Curso;
import br.com.ecociente.mobile.entrypoint.dto.educacao.response.CursoResponseDto;

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
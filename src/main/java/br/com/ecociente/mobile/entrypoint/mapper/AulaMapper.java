package br.com.ecociente.mobile.entrypoint.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.Aula;
import br.com.ecociente.mobile.entrypoint.dto.response.AulaResponseDto;

@Component
public class AulaMapper {

    public AulaResponseDto toResponse(Aula aula) {
        return new AulaResponseDto(
            aula.id(),
            aula.cursoId(),
            aula.titulo(),
            aula.conteudo(),
            aula.ordem()
        );
    }
}
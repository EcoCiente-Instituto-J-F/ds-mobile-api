package br.com.ecociente.mobile.entrypoint.mapper.educacao;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.educacao.Aula;
import br.com.ecociente.mobile.entrypoint.dto.educacao.response.AulaResponseDto;

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
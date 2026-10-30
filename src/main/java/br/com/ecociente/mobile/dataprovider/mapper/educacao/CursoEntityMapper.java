package br.com.ecociente.mobile.dataprovider.mapper.educacao;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.educacao.Curso;
import br.com.ecociente.mobile.dataprovider.entity.educacao.CursoEntity;

@Component
public class CursoEntityMapper {

    public Curso toDomain(CursoEntity entity) {
        return new Curso(
            entity.getId(),
            entity.getTitulo(),
            entity.getDescricao(),
            entity.isEstaAtivo()
        );
    }
}
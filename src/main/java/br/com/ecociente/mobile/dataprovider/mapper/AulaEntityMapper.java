package br.com.ecociente.mobile.dataprovider.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.Aula;
import br.com.ecociente.mobile.dataprovider.entity.AulaEntity;

@Component
public class AulaEntityMapper {

    public Aula toDomain(AulaEntity entity) {
        return new Aula(
            entity.getId(),
            entity.getCursoId(),
            entity.getTitulo(),
            entity.getConteudo(),
            entity.getOrdem()
        );
    }
}
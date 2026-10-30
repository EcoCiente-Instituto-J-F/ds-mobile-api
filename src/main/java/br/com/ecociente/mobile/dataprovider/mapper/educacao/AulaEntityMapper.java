package br.com.ecociente.mobile.dataprovider.mapper.educacao;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.educacao.Aula;
import br.com.ecociente.mobile.dataprovider.entity.educacao.AulaEntity;

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
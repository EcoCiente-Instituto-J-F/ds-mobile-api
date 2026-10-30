package br.com.ecociente.mobile.dataprovider.mapper.pontuacao;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.pontuacao.CategoriaResiduo;
import br.com.ecociente.mobile.dataprovider.entity.CategoriaResiduoEntity;

@Component
public class PontuacaoCategoriaResiduoMapper {

    public CategoriaResiduo toDomain(CategoriaResiduoEntity entity) {
        return CategoriaResiduo.builder()
                .id(entity.getId())
                .nomeCategoria(entity.getNomeCategoria())
                .pontosBase(entity.getPontosBase())
                .limitePontosDiario(entity.getLimitePontosDiario())
                .build();
    }
}
package br.com.ecociente.mobile.dataprovider.mapper.pontuacao;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.pontuacao.Postagem;
import br.com.ecociente.mobile.dataprovider.entity.PostagemEntity;

@Component
public class PontuacaoPostagemMapper {

    public Postagem toDomain(PostagemEntity entity) {
        return Postagem.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuarioId())
                .condominioId(entity.getCondominioId())
                .torreId(entity.getTorreId())
                .categoriaId(entity.getCategoriaId())
                .statusValidacaoId(entity.getStatusValidacaoId())
                .saldoConfianca(entity.getSaldoConfianca())
                .pontuacaoAtiva(entity.getPontuacaoAtiva())
                .pontuacaoReconciliacaoPendente(
                        entity.getPontuacaoReconciliacaoPendente())
                .dataPostagem(entity.getDataPostagem())
                .dataLimiteAnalise(entity.getDataLimiteAnalise())
                .resolvidoEm(entity.getResolvidoEm())
                .build();
    }
}
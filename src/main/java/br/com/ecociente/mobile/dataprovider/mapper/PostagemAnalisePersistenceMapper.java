package br.com.ecociente.mobile.dataprovider.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.Postagem;
import br.com.ecociente.mobile.dataprovider.entity.PostagemEntity;

@Component
public class PostagemAnalisePersistenceMapper {

  public Postagem toDomain(
      PostagemEntity entity,
      Object[] dados) {

    return new Postagem(
        entity.getId(),
        entity.getUsuarioId(),
        (String) dados[1],
        entity.getCondominioId(),
        entity.getTorreId(),
        entity.getCategoriaId(),
        (String) dados[2],
        entity.getUrlFoto(),
        entity.getCapturadaEm(),
        entity.getDataPostagem(),
        (String) dados[3],
        entity.getSaldoConfianca(),
        entity.getDataLimiteAnalise(),
        entity.getResolvidoEm());
  }
}
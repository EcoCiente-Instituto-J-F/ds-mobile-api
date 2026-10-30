package br.com.ecociente.mobile.dataprovider.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.Cooperativa;
import br.com.ecociente.mobile.dataprovider.entity.CooperativaEntity;

@Component
public class CooperativaDetalhePersistenceMapper {

  public Cooperativa toDomain(
      CooperativaEntity entity,
      Object[] dados) {

    BigDecimal media = dados[6] == null
        ? null
        : new BigDecimal(dados[6].toString());

    long total = dados[7] == null
        ? 0L
        : ((Number) dados[7]).longValue();

    return new Cooperativa(
        entity.getId(),
        entity.getNome(),
        entity.getEmail(),
        entity.getTelefone(),
        (String) dados[0],
        (String) dados[1],
        (String) dados[2],
        (String) dados[3],
        (String) dados[4],
        (String) dados[5],
        media,
        total);
  }
}
package br.com.ecociente.mobile.dataprovider.mapper.autenticacao;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.autenticacao.Autenticacao;
import br.com.ecociente.mobile.dataprovider.entity.autenticacao.AutenticacaoEntity;

@Component 
public class AutenticacaoPersistenceMapper {
  public Autenticacao toDomain(AutenticacaoEntity entity) {
    return new Autenticacao(
      entity.getId(),
      entity.getUsuarioId(),
      entity.getToken(),
      entity.getTipoToken(),
      entity.getCriadoEm(),
      entity.getExpiradoEm()
    );
  }

  public AutenticacaoEntity toEntity(Autenticacao domain) {
    return AutenticacaoEntity.builder()
      .usuarioId(domain.getUsuarioId())
      .token(domain.getToken())
      .tipoToken(domain.getTipoToken())
      .criadoEm(OffsetDateTime.now())
      .expiradoEm(domain.getExpiradoEm())
      .build();
  }
}

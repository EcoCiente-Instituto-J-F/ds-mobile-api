package br.com.ecociente.mobile.dataprovider.mapper.cadastro;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.Morador;
import br.com.ecociente.mobile.dataprovider.entity.VinculoUsuarioCondominioEntity;

@Component
public class MoradorPersistenceMapper {

  public VinculoUsuarioCondominioEntity toEntity(
      Morador morador) {
    return VinculoUsuarioCondominioEntity.builder()
        .idMorador(morador.getIdMorador())
        .usuarioId(morador.getUsuarioId())
        .condominioId(morador.getCondominioId())
        .dataEntrada(morador.getDataEntrada())
        .dataSaida(morador.getDataSaida())
        .aprovado(morador.getAprovado())
        .nivelConfiancaId(morador.getNivelConfiancaId())
        .trustScore(morador.getTrustScore())
        .postagensValidasSemContestacao(
            morador.getPostagensValidadasSemContestacao())
        .denunciasRealizadas(
            morador.getDenunciasRealizadas())
        .denunciasProcedentes(
            morador.getDenunciasProcedentes())
        .taxaAcertoDenuncias(
            morador.getTaxaAcertoDenuncias())
        .build();
  }

  public Morador toDomain(
      VinculoUsuarioCondominioEntity entity) {
    return Morador.builder()
        .idMorador(entity.getIdMorador())
        .usuarioId(entity.getUsuarioId())
        .condominioId(entity.getCondominioId())
        .dataEntrada(entity.getDataEntrada())
        .dataSaida(entity.getDataSaida())
        .aprovado(entity.getAprovado())
        .aprovadoPorUsuarioId(
            entity.getAprovadoPorUser() == null
                ? null
                : entity.getAprovadoPorUser().getId())
        .nivelConfiancaId(entity.getNivelConfiancaId())
        .trustScore(entity.getTrustScore())
        .postagensValidadasSemContestacao(
            entity.getPostagensValidasSemContestacao())
        .denunciasRealizadas(entity.getDenunciasRealizadas())
        .denunciasProcedentes(entity.getDenunciasProcedentes())
        .taxaAcertoDenuncias(entity.getTaxaAcertoDenuncias())
        .build();
  }
}
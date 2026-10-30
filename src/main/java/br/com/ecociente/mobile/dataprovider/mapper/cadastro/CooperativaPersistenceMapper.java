package br.com.ecociente.mobile.dataprovider.mapper.cadastro;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.Cooperativa;
import br.com.ecociente.mobile.dataprovider.entity.CooperativaEntity;

@Component 
public class CooperativaPersistenceMapper {

  public CooperativaEntity toEntity(Cooperativa cooperativa){
    return CooperativaEntity.builder()
    .id(cooperativa.getIdCoop())
    .cnpj(cooperativa.getCnpjCoop())
    .nome(cooperativa.getNomeCoop())
    .email(cooperativa.getEmailCoop())
    .telefone(cooperativa.getTelefoneCoop())
    .dataCadastro(cooperativa.getDataCadastro())
    .usuarioId(cooperativa.getUsuarioId())
    .enderecoId(cooperativa.getEnderecoId())
    .build();
  }

  public Cooperativa toDomain(CooperativaEntity cooperativaEntity){
    return Cooperativa.builder()
    .idCoop(cooperativaEntity.getId())
    .cnpjCoop(cooperativaEntity.getCnpj())
    .nomeCoop(cooperativaEntity.getNome())
    .emailCoop(cooperativaEntity.getEmail())
    .telefoneCoop(cooperativaEntity.getTelefone())
    .dataCadastro(cooperativaEntity.getDataCadastro())
    .usuarioId(cooperativaEntity.getUsuarioId())
    .enderecoId(cooperativaEntity.getEnderecoId())
    .build();
    
  }
  
}

package br.com.ecociente.mobile.dataprovider.mapper.cadastro;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.dataprovider.entity.EnderecoEntity;

@Component 
public class EnderecoPersistenceMapper {
  public EnderecoEntity toEntity(Endereco endereco){
    return EnderecoEntity.builder()
    .idEndereco(endereco.getIdEndereco())
    .cep(endereco.getCep())
    .estado(endereco.getEstado())
    .cidade(endereco.getCidade())
    .logradouro(endereco.getLogradouro())
    .numero(endereco.getNumero())
    .complemento(endereco.getComplemento())
    .build();
  }

  public Endereco toDomain(EnderecoEntity enderecoEntity){
    return Endereco.builder()
    .idEndereco(enderecoEntity.getIdEndereco())
    .cep(enderecoEntity.getCep())
    .estado(enderecoEntity.getEstado())
    .cidade(enderecoEntity.getCidade())
    .logradouro(enderecoEntity.getLogradouro())
    .numero(enderecoEntity.getNumero())
    .complemento(enderecoEntity.getComplemento())
    .build();
  }
  
}

package br.com.ecociente.mobile.dataprovider.mapper.cadastro;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.Condominio;
import br.com.ecociente.mobile.dataprovider.entity.CondominioEntity;

@Component 
public class CondominioPersistenceMapper {

  public CondominioEntity toEntity(Condominio condominio){
    return CondominioEntity.builder()
    .idCondominio(condominio.getIdCondominio())
    .nomeCondominio(condominio.getNomeCondominio())
    .cnpj(condominio.getCnpj())
    .codigoAcesso(condominio.getCodigoAcesso())
    .ativo(condominio.getAtivo())
    .tipoCondominioId(condominio.getTipoCondominioId())
    .enderecoId(condominio.getEnderecoId())
    .build();
  }

  public Condominio toDomain(CondominioEntity condominio){
    return Condominio.builder()
    .idCondominio(condominio.getIdCondominio())
    .nomeCondominio(condominio.getNomeCondominio())
    .cnpj(condominio.getCnpj())
    .codigoAcesso(condominio.getCodigoAcesso())
    .ativo(condominio.getAtivo())
    .tipoCondominioId(condominio.getTipoCondominioId())
    .enderecoId(condominio.getEnderecoId())
    .build();
  }

  
}

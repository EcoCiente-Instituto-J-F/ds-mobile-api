package br.com.ecociente.mobile.dataprovider.gateway.cadastro;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.exception.cadastro.NotFoundException;
import br.com.ecociente.mobile.core.gateway.cadastro.EnderecoGateway;
import br.com.ecociente.mobile.dataprovider.entity.EnderecoEntity;
import br.com.ecociente.mobile.dataprovider.mapper.cadastro.EnderecoPersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.EnderecoRepository;

@Component 
public class EnderecoGatewayImpl implements EnderecoGateway  {

  private final EnderecoRepository enderecoRepository;
  private final EnderecoPersistenceMapper enderecoPersistenceMapper;

  public EnderecoGatewayImpl(EnderecoPersistenceMapper enderecoPersistenceMapper, EnderecoRepository enEnderecoRepository){
    this.enderecoPersistenceMapper=enderecoPersistenceMapper;
    this.enderecoRepository=enEnderecoRepository;
  }


  @Override
  public Endereco salvar(Endereco endereco) {
    EnderecoEntity entity = enderecoPersistenceMapper.toEntity(endereco);
    EnderecoEntity salvar = enderecoRepository.save(entity);
    return enderecoPersistenceMapper.toDomain(salvar);
  }

  @Override
  public Endereco buscarPorId(Integer idEndereco) {
    return enderecoRepository.findById(idEndereco).map(enderecoPersistenceMapper::toDomain).orElseThrow(() -> new NotFoundException("ENDERECO_NAO_ENCONTRADO",
      "Endereço não encontrado"
    ));
  }
  
}

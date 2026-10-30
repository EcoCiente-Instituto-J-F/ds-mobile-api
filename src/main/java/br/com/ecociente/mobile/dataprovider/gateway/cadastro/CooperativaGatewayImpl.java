package br.com.ecociente.mobile.dataprovider.gateway.cadastro;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.Cooperativa;
import br.com.ecociente.mobile.core.gateway.cadastro.CooperativaGateway;
import br.com.ecociente.mobile.dataprovider.entity.CooperativaEntity;
import br.com.ecociente.mobile.dataprovider.mapper.cadastro.CooperativaPersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.CooperativaRepository;

@Component 
public class CooperativaGatewayImpl implements CooperativaGateway {

  private final CooperativaRepository cooperativaRepository;
  private final CooperativaPersistenceMapper cooperativaPersistenceMapper;

  public CooperativaGatewayImpl(CooperativaRepository cooperativaRepository,CooperativaPersistenceMapper cooperativaPersistenceMapper){
    this.cooperativaRepository=cooperativaRepository;
    this.cooperativaPersistenceMapper=cooperativaPersistenceMapper;
  }


  @Override
  public Cooperativa salvar(Cooperativa cooperativa) {
    CooperativaEntity entity = cooperativaPersistenceMapper.toEntity(cooperativa);
    CooperativaEntity salvar = cooperativaRepository.save(entity);
    return cooperativaPersistenceMapper.toDomain(salvar);
  }

  @Override
  public boolean existePorCnpj(String cnpj) {
    return cooperativaRepository.existsByCnpj(cnpj);
  }
  
}

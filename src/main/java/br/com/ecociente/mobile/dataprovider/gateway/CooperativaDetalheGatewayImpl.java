package br.com.ecociente.mobile.dataprovider.gateway;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.Cooperativa;
import br.com.ecociente.mobile.core.gateway.CooperativaDetalheGateway;
import br.com.ecociente.mobile.dataprovider.mapper.CooperativaDetalhePersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.CooperativaRepository;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class CooperativaDetalheGatewayImpl
    implements CooperativaDetalheGateway {

  private final CooperativaRepository repository;
  private final CooperativaDetalhePersistenceMapper mapper;

  public CooperativaDetalheGatewayImpl(
      CooperativaRepository repository,
      CooperativaDetalhePersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public boolean sindicoTemCondominioAtivo(
      Integer usuarioId) {
    return repository.sindicoTemCondominioAtivo(
        usuarioId);
  }

  @Override
  public Optional<Cooperativa> buscarVisivelAoSindico(
      Integer usuarioId,
      Integer cooperativaId) {

    return repository.buscarVisivelAoSindico(
        usuarioId,
        cooperativaId)
        .map(entity -> {
          var dados = repository.buscarDadosComplementares(
              cooperativaId);

          if (dados.size() != 1) {
            throw new IllegalStateException(
                "Dados complementares da cooperativa não encontrados");
          }

          return mapper.toDomain(
              entity,
              dados.get(0));
        });
  }
}
package br.com.ecociente.mobile.dataprovider.gateway;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.Postagem;
import br.com.ecociente.mobile.core.gateway.PostagemAnaliseGateway;
import br.com.ecociente.mobile.dataprovider.entity.PostagemAnaliseEntity;
import br.com.ecociente.mobile.dataprovider.mapper.PostagemAnalisePersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.PostagemAnaliseRepository;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class PostagemAnaliseGatewayImpl
    implements PostagemAnaliseGateway {

  private final PostagemAnaliseRepository repository;
  private final PostagemAnalisePersistenceMapper mapper;

  public PostagemAnaliseGatewayImpl(
      PostagemAnaliseRepository repository,
      PostagemAnalisePersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<Integer> buscarResponsavelAtivo(
      Integer condominioId) {
    return repository.buscarResponsavelAtivo(
        condominioId);
  }

  @Override
  public long contarEmAnalise(
      Integer condominioId) {
    return repository.contarEmAnalise(
        condominioId);
  }

  @Override
  public List<Postagem> listarEmAnalise(
      Integer condominioId,
      long deslocamento,
      int limite) {

    var entities = repository.listarEmAnalise(
        condominioId,
        deslocamento,
        limite);

    if (entities.isEmpty()) {
      return List.of();
    }

    var ids = entities.stream()
        .map(PostagemAnaliseEntity::getId)
        .toList();

    var complementos = repository.buscarDadosComplementares(ids)
        .stream()
        .collect(Collectors.toMap(
            dados -> ((Number) dados[0]).intValue(),
            Function.identity()));

    return entities.stream()
        .map(entity -> {
          var dados = complementos.get(entity.getId());

          if (dados == null) {
            throw new IllegalStateException(
                "Dados complementares da postagem não encontrados");
          }

          return mapper.toDomain(entity, dados);
        })
        .toList();
  }
}
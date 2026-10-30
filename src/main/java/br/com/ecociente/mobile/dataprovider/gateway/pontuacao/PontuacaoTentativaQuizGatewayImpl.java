package br.com.ecociente.mobile.dataprovider.gateway.pontuacao;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.pontuacao.TentativaQuiz;
import br.com.ecociente.mobile.core.gateway.pontuacao.TentativaQuizGateway;
import br.com.ecociente.mobile.dataprovider.mapper.pontuacao.PontuacaoTentativaQuizMapper;
import br.com.ecociente.mobile.dataprovider.repository.TentativaQuizRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PontuacaoTentativaQuizGatewayImpl implements TentativaQuizGateway {

  private final TentativaQuizRepository repository;
  private final PontuacaoTentativaQuizMapper mapper;

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<TentativaQuiz> buscarComBloqueio(Integer tentativaId) {
    return repository.buscarComBloqueio(tentativaId)
        .map(mapper::toDomain);
  }
}
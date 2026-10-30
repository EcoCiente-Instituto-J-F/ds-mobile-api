package br.com.ecociente.mobile.core.gateway.pontuacao;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.pontuacao.TentativaQuiz;

public interface TentativaQuizGateway {

  Optional<TentativaQuiz> buscarComBloqueio(Integer tentativaId);
}
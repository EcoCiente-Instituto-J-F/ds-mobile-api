package br.com.ecociente.mobile.dataprovider.repository.educacao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.ecociente.mobile.dataprovider.entity.educacao.PerguntaQuizEntity;

public interface PerguntaQuizRepository
    extends JpaRepository<PerguntaQuizEntity, Integer> {

    List<PerguntaQuizEntity> findByQuizIdOrderByOrdemAscIdAsc(
        Integer quizId
    );
}
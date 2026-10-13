package br.com.ecociente.mobile.dataprovider.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.ecociente.mobile.dataprovider.entity.PerguntaQuizEntity;

public interface PerguntaQuizRepository
    extends JpaRepository<PerguntaQuizEntity, Integer> {

    List<PerguntaQuizEntity> findByQuizIdOrderByOrdemAscIdAsc(
        Integer quizId
    );
}
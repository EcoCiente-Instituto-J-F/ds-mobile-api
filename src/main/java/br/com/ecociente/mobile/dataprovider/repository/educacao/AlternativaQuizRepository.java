package br.com.ecociente.mobile.dataprovider.repository.educacao;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.ecociente.mobile.dataprovider.entity.educacao.AlternativaQuizEntity;

public interface AlternativaQuizRepository
    extends JpaRepository<AlternativaQuizEntity, Integer> {

    List<AlternativaQuizEntity>
        findByPerguntaIdInOrderByIdAsc(Collection<Integer> perguntaIds);
}
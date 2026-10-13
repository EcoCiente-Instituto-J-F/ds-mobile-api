package br.com.ecociente.mobile.dataprovider.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.ecociente.mobile.dataprovider.entity.AlternativaQuizEntity;

public interface AlternativaQuizRepository
    extends JpaRepository<AlternativaQuizEntity, Integer> {

    List<AlternativaQuizEntity>
        findByPerguntaIdInOrderByIdAsc(Collection<Integer> perguntaIds);
}
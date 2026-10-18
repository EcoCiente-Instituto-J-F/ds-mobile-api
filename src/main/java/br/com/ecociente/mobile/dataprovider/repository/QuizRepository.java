package br.com.ecociente.mobile.dataprovider.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.ecociente.mobile.dataprovider.entity.QuizEntity;

public interface QuizRepository
        extends JpaRepository<QuizEntity, Integer> {

    @Query("""
            SELECT q
            FROM QuizEntity q
            WHERE q.aulaId = :aulaId
              AND q.ativo = true
              AND EXISTS (
                  SELECT a.id
                  FROM AulaEntity a
                  WHERE a.id = q.aulaId
                    AND a.cursoId = q.cursoId
              )
              AND EXISTS (
                  SELECT c.id
                  FROM CursoEntity c
                  WHERE c.id = q.cursoId
                    AND c.estaAtivo = true
              )
            """)
    Optional<QuizEntity> buscarDisponivelPorAulaId(
            @Param("aulaId") Integer aulaId);

    @Query("""
            SELECT q
            FROM QuizEntity q
            WHERE q.id = :quizId
              AND q.ativo = true
              AND EXISTS (
                  SELECT a.id
                  FROM AulaEntity a
                  WHERE a.id = q.aulaId
                    AND a.cursoId = q.cursoId
              )
              AND EXISTS (
                  SELECT c.id
                  FROM CursoEntity c
                  WHERE c.id = q.cursoId
                    AND c.estaAtivo = true
              )
            """)
    Optional<QuizEntity> buscarDisponivelPorId(
            @Param("quizId") Integer quizId);
}
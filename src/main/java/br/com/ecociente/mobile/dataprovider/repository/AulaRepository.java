package br.com.ecociente.mobile.dataprovider.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.ecociente.mobile.dataprovider.entity.AulaEntity;

public interface AulaRepository
    extends JpaRepository<AulaEntity, Integer> {

    List<AulaEntity> findByCursoIdOrderByOrdemAscIdAsc(Integer cursoId);

    @Query("""
        SELECT a
        FROM AulaEntity a
        WHERE a.id = :aulaId
          AND EXISTS (
              SELECT c.id
              FROM CursoEntity c
              WHERE c.id = a.cursoId
                AND c.estaAtivo = true
          )
        """)
    Optional<AulaEntity> buscarAulaDeCursoAtivoPorId(
        @Param("aulaId") Integer aulaId
    );
}
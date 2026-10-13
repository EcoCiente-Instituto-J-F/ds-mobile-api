package br.com.ecociente.mobile.dataprovider.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.ecociente.mobile.dataprovider.entity.CursoEntity;

public interface CursoRepository
    extends JpaRepository<CursoEntity, Integer> {

    List<CursoEntity> findByEstaAtivoTrueOrderByTituloAscIdAsc();

    Optional<CursoEntity> findByIdAndEstaAtivoTrue(Integer id);
}
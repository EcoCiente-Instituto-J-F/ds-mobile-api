package br.com.ecociente.mobile.dataprovider.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.ecociente.mobile.dataprovider.entity.TipoUsuarioEntity;

public interface TipoUsuarioRepository
    extends JpaRepository<TipoUsuarioEntity, Integer> {

  @Query("""
      SELECT tipo
      FROM TipoUsuarioEntity tipo
      WHERE LOWER(TRIM(tipo.nomeTipo)) IN :nomes
      """)
  List<TipoUsuarioEntity> findByNomesNormalizados(
      @Param("nomes") Collection<String> nomes);
}
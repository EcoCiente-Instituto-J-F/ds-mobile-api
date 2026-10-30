package br.com.ecociente.mobile.dataprovider.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.ecociente.mobile.dataprovider.entity.UsuarioEntity;
import jakarta.persistence.LockModeType;

public interface UsuarioRepository
    extends JpaRepository<UsuarioEntity, Integer> {

  @EntityGraph(attributePaths = "tipoUsuario")
  Optional<UsuarioEntity> findByEmailUsuarioIgnoreCase(
      String emailUsuario);

  boolean existsByEmailUsuarioIgnoreCase(
      String emailUsuario);

  boolean existsByCpf(String cpf);

  boolean existsByEmailUsuarioIgnoreCaseAndIdNot(
      String emailUsuario,
      Integer id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT u
      FROM UsuarioEntity u
      WHERE u.id = :id
      """)
  Optional<UsuarioEntity> buscarPorIdComBloqueio(
      @Param("id") Integer id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT u
      FROM UsuarioEntity u
      WHERE LOWER(u.emailUsuario) = LOWER(:email)
      """)
  Optional<UsuarioEntity> buscarPorEmailComBloqueio(
      @Param("email") String email);
}
package br.com.ecociente.mobile.dataprovider.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import br.com.ecociente.mobile.dataprovider.entity.PostagemEntity;

public interface PostagemAnaliseRepository
    extends Repository<PostagemEntity, Integer> {

  @Query(value = """
      SELECT s.usuario_id
      FROM tb_condominios c
      JOIN tb_sindicos s
        ON s.id_sindico = c.sindico_id
      JOIN tb_usuarios u
        ON u.id_usuario = s.usuario_id
      WHERE c.id_condominio = :condominioId
        AND c.ativo = TRUE
        AND u.ativo = TRUE
      """, nativeQuery = true)
  Optional<Integer> buscarResponsavelAtivo(
      @Param("condominioId") Integer condominioId);

  @Query(value = """
      SELECT COUNT(*)
      FROM tb_postagens p
      JOIN tb_lkp_status_validacoes_postagens s
        ON s.id_status_validacao = p.status_validacao_id
      WHERE p.condominio_id = :condominioId
        AND s.nome_status = 'em_analise'
      """, nativeQuery = true)
  long contarEmAnalise(
      @Param("condominioId") Integer condominioId);

  @Query(value = """
      SELECT p.*
      FROM tb_postagens p
      JOIN tb_lkp_status_validacoes_postagens s
        ON s.id_status_validacao = p.status_validacao_id
      WHERE p.condominio_id = :condominioId
        AND s.nome_status = 'em_analise'
      ORDER BY p.data_postagem ASC, p.id_postagem ASC
      LIMIT :limite OFFSET :deslocamento
      """, nativeQuery = true)
  List<PostagemEntity> listarEmAnalise(
      @Param("condominioId") Integer condominioId,
      @Param("deslocamento") long deslocamento,
      @Param("limite") int limite);

  @Query(value = """
      SELECT
          p.id_postagem,
          u.nome_usuario,
          c.nome_categoria,
          s.nome_status
      FROM tb_postagens p
      JOIN tb_usuarios u
        ON u.id_usuario = p.usuario_id
      JOIN tb_lkp_categorias_residuos c
        ON c.id_categoria = p.categoria_id
      JOIN tb_lkp_status_validacoes_postagens s
        ON s.id_status_validacao = p.status_validacao_id
      WHERE p.id_postagem IN (:ids)
      """, nativeQuery = true)
  List<Object[]> buscarDadosComplementares(
      @Param("ids") Collection<Integer> ids);
}
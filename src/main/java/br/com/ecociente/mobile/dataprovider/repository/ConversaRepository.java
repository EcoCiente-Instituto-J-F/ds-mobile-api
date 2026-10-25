package br.com.ecociente.mobile.dataprovider.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.ecociente.mobile.dataprovider.entity.ConversaEntity;

import jakarta.persistence.LockModeType;

public interface ConversaRepository
    extends JpaRepository<ConversaEntity, Integer> {

  String JOINS = """
      FROM tb_conversas c
      JOIN tb_condominios cond
        ON cond.id_condominio = c.condominio_id
      LEFT JOIN tb_sindicos s
        ON s.id_sindico = cond.sindico_id
      JOIN tb_cooperativas coop
        ON coop.id_cooperativa = c.cooperativa_id
      """;

  String FILTRO_PARTICIPANTE = """
      JOIN tb_usuarios u
        ON u.id_usuario = :usuarioId
       AND u.ativo = TRUE
      JOIN tb_usuarios dono
        ON dono.id_usuario = coop.usuario_id
       AND dono.ativo = TRUE
      WHERE cond.ativo = TRUE
        AND c.ativa = :ativa
        AND (
            COALESCE(s.usuario_id = :usuarioId, FALSE)
            <>
            COALESCE(coop.usuario_id = :usuarioId, FALSE)
        )
      """;

  @Query(value = """
      SELECT CASE
          WHEN s.usuario_id = :usuarioId
           AND coop.usuario_id = :usuarioId
          THEN 'AMBIGUO'
          WHEN s.usuario_id = :usuarioId
          THEN 'CONDOMINIO'
          WHEN coop.usuario_id = :usuarioId
          THEN 'COOPERATIVA'
      END
      FROM tb_condominios cond
      LEFT JOIN tb_sindicos s
        ON s.id_sindico = cond.sindico_id
      JOIN tb_cooperativas coop
        ON coop.id_cooperativa = :cooperativaId
      JOIN tb_usuarios u
        ON u.id_usuario = :usuarioId
       AND u.ativo = TRUE
      JOIN tb_usuarios dono
        ON dono.id_usuario = coop.usuario_id
       AND dono.ativo = TRUE
      WHERE cond.id_condominio = :condominioId
        AND cond.ativo = TRUE
      """, nativeQuery = true)
  Optional<String> buscarLado(
      @Param("usuarioId") Integer usuarioId,
      @Param("condominioId") Integer condominioId,
      @Param("cooperativaId") Integer cooperativaId);

  @Modifying
  @Query(value = """
      INSERT INTO tb_conversas (
          condominio_id,
          cooperativa_id
      )
      VALUES (:condominioId, :cooperativaId)
      ON CONFLICT (condominio_id, cooperativa_id)
      DO NOTHING
      """, nativeQuery = true)
  int inserirSeAusente(
      @Param("condominioId") Integer condominioId,
      @Param("cooperativaId") Integer cooperativaId);

  @Query("""
      SELECT c.id
      FROM ConversaEntity c
      WHERE c.condominioId = :condominioId
        AND c.cooperativaId = :cooperativaId
      """)
  Optional<Integer> buscarIdPorPartes(
      @Param("condominioId") Integer condominioId,
      @Param("cooperativaId") Integer cooperativaId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT c
      FROM ConversaEntity c
      WHERE c.id = :conversaId
      """)
  Optional<ConversaEntity> buscarComBloqueio(
      @Param("conversaId") Integer conversaId);

  @Query(value = "SELECT COUNT(*) "
      + JOINS
      + FILTRO_PARTICIPANTE, nativeQuery = true)
  long contarDoParticipante(
      @Param("usuarioId") Integer usuarioId,
      @Param("ativa") boolean ativa);

  @Query(value = "SELECT c.* "
      + JOINS
      + FILTRO_PARTICIPANTE
      + """
          ORDER BY
              COALESCE(
                  c.ultima_mensagem_em,
                  c.criada_em
              ) DESC,
              c.id_conversa DESC
          LIMIT :limite OFFSET :deslocamento
          """, nativeQuery = true)
  List<ConversaEntity> listarDoParticipante(
      @Param("usuarioId") Integer usuarioId,
      @Param("ativa") boolean ativa,
      @Param("deslocamento") long deslocamento,
      @Param("limite") int limite);

  @Query(value = """
      SELECT
          c.id_conversa AS "id",
          cond.nome_condominio AS "nomeCondominio",
          coop.nome_cooperativa AS "nomeCooperativa",
          (
              SELECT COUNT(*)
              FROM tb_mensagens_conversas m
              WHERE m.conversa_id = c.id_conversa
                AND m.lida_em IS NULL
                AND m.remetente_tipo <> CASE
                    WHEN s.usuario_id = :usuarioId
                    THEN 'CONDOMINIO'
                    ELSE 'COOPERATIVA'
                END
          ) AS "naoLidas"
      """
      + JOINS
      + """
          WHERE c.id_conversa IN (:ids)
          """, nativeQuery = true)
  List<Object[]> buscarResumos(
      @Param("usuarioId") Integer usuarioId,
      @Param("ids") Collection<Integer> ids);
}

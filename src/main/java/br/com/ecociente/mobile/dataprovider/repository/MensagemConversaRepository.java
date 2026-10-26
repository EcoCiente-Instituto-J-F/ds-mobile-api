package br.com.ecociente.mobile.dataprovider.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.ecociente.mobile.dataprovider.entity.MensagemConversaEntity;

public interface MensagemConversaRepository
    extends JpaRepository<MensagemConversaEntity, Long> {

  long countByConversaId(Integer conversaId);

  @Query(value = """
      SELECT *
      FROM tb_mensagens_conversas
      WHERE conversa_id = :conversaId
      ORDER BY enviada_em DESC, id_mensagem DESC
      LIMIT :limite OFFSET :deslocamento
      """, nativeQuery = true)
  List<MensagemConversaEntity> listarDaConversa(
      @Param("conversaId") Integer conversaId,
      @Param("deslocamento") long deslocamento,
      @Param("limite") int limite);

  @Modifying(flushAutomatically = true)
  @Query(value = """
      UPDATE tb_mensagens_conversas
      SET lida_em = GREATEST(
          clock_timestamp(),
          enviada_em
      )
      WHERE conversa_id = :conversaId
        AND remetente_tipo <> :lado
        AND lida_em IS NULL
      """, nativeQuery = true)
  int marcarTodasRecebidas(
      @Param("conversaId") Integer conversaId,
      @Param("lado") String lado);

  @Modifying(flushAutomatically = true)
  @Query(value = """
      UPDATE tb_mensagens_conversas
      SET lida_em = GREATEST(
          clock_timestamp(),
          enviada_em
      )
      WHERE conversa_id = :conversaId
        AND remetente_tipo <> :lado
        AND lida_em IS NULL
        AND id_mensagem <= :ateMensagemId
      """, nativeQuery = true)
  int marcarRecebidasAte(
      @Param("conversaId") Integer conversaId,
      @Param("lado") String lado,
      @Param("ateMensagemId") Long ateMensagemId);
}
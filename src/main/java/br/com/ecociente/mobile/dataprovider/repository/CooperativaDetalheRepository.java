package br.com.ecociente.mobile.dataprovider.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import br.com.ecociente.mobile.dataprovider.entity.CooperativaDetalheEntity;

public interface CooperativaDetalheRepository
    extends Repository<CooperativaDetalheEntity, Integer> {

  @Query(value = """
      SELECT EXISTS (
          SELECT 1
          FROM tb_sindicos s
          JOIN tb_usuarios u
            ON u.id_usuario = s.usuario_id
           AND u.ativo = TRUE
          JOIN tb_condominios c
            ON c.sindico_id = s.id_sindico
           AND c.ativo = TRUE
          WHERE s.usuario_id = :usuarioId
      )
      """, nativeQuery = true)
  boolean sindicoTemCondominioAtivo(
      @Param("usuarioId") Integer usuarioId);

  @Query(value = """
      SELECT c.*
      FROM tb_cooperativas c
      JOIN tb_enderecos e
        ON e.id_endereco = c.endereco_id
      LEFT JOIN tb_usuarios responsavel
        ON responsavel.id_usuario = c.usuario_id
      WHERE c.id_cooperativa = :cooperativaId
        AND (
            responsavel.id_usuario IS NULL
            OR responsavel.ativo = TRUE
        )
        AND EXISTS (
            SELECT 1
            FROM tb_sindicos s
            JOIN tb_usuarios usuario
              ON usuario.id_usuario = s.usuario_id
             AND usuario.ativo = TRUE
            JOIN tb_condominios cond
              ON cond.sindico_id = s.id_sindico
             AND cond.ativo = TRUE
            JOIN tb_enderecos endereco_condominio
              ON endereco_condominio.id_endereco = cond.endereco_id
            WHERE s.usuario_id = :usuarioId
              AND LOWER(TRIM(endereco_condominio.cidade))
                  = LOWER(TRIM(e.cidade))
              AND UPPER(TRIM(endereco_condominio.estado))
                  = UPPER(TRIM(e.estado))
        )
      """, nativeQuery = true)
  Optional<CooperativaDetalheEntity> buscarVisivelAoSindico(
      @Param("usuarioId") Integer usuarioId,
      @Param("cooperativaId") Integer cooperativaId);

  @Query(value = """
      SELECT
          e.cep,
          e.logradouro,
          e.numero,
          e.complemento,
          e.cidade,
          e.estado,
          (
              SELECT ROUND(AVG(av.nota), 2)
              FROM tb_avaliacoes_visitas_coletas av
              JOIN tb_visitas_coletas v
                ON v.id_visita_coleta = av.visita_coleta_id
              JOIN tb_agendamentos_coletas ag
                ON ag.id_agendamento_coleta = v.agendamento_coleta_id
              WHERE ag.cooperativa_id = c.id_cooperativa
          ) AS media_avaliacoes,
          (
              SELECT COUNT(*)
              FROM tb_avaliacoes_visitas_coletas av
              JOIN tb_visitas_coletas v
                ON v.id_visita_coleta = av.visita_coleta_id
              JOIN tb_agendamentos_coletas ag
                ON ag.id_agendamento_coleta = v.agendamento_coleta_id
              WHERE ag.cooperativa_id = c.id_cooperativa
          ) AS total_avaliacoes
      FROM tb_cooperativas c
      JOIN tb_enderecos e
        ON e.id_endereco = c.endereco_id
      WHERE c.id_cooperativa = :cooperativaId
      """, nativeQuery = true)
  List<Object[]> buscarDadosComplementares(
      @Param("cooperativaId") Integer cooperativaId);
}
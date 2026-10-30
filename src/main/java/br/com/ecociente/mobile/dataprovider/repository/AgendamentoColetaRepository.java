package br.com.ecociente.mobile.dataprovider.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.ecociente.mobile.dataprovider.entity.AgendamentoColetaEntity;
import java.util.Collection;
import java.util.List;

public interface AgendamentoColetaRepository extends JpaRepository<AgendamentoColetaEntity, Integer> {

  /** Id e nome de uma cooperativa ou de um condomínio. */
  interface NomeProjecao {
    Integer getId();

    String getNome();
  }

  @Query(value = """
    SELECT id_cooperativa AS id, nome_cooperativa AS nome
    FROM tb_cooperativas
    WHERE id_cooperativa IN (:ids)
    """,
    nativeQuery = true)
  List<NomeProjecao> nomesDeCooperativas(@Param("ids") Collection<Integer> ids);

  @Query(value = """
    SELECT id_condominio AS id, nome_condominio AS nome
    FROM tb_condominios
    WHERE id_condominio IN (:ids)
    """,
    nativeQuery = true)
  List<NomeProjecao> nomesDeCondominios(@Param("ids") Collection<Integer> ids);

  @Query(value = """
    SELECT ac.*
    FROM tb_agendamentos_coletas ac
    INNER JOIN tb_condominios c 
        ON c.id_condominio = ac.condominio_id
    INNER JOIN tb_sindicos s 
        ON s.id_sindico = c.sindico_id    
    INNER JOIN tb_lkp_status_agendamentos sa
        ON sa.id_status = ac.status_agendamento_id        
    WHERE s.usuario_id = :usuarioId
        AND (CAST(:status AS VARCHAR) IS NULL OR UPPER(sa.nome_status) = CAST(:status AS VARCHAR))
        AND (CAST(:dataInicio AS TIMESTAMP) IS NULL OR ac.data_inicio >= CAST(:dataInicio AS TIMESTAMP))
        AND (CAST(:dataFim AS TIMESTAMP) IS NULL OR ac.data_fim <= CAST(:dataFim AS TIMESTAMP))
        AND (CAST(:condominioId AS INTEGER) IS NULL OR ac.condominio_id = CAST(:condominioId AS INTEGER))
        AND (CAST(:cooperativaId AS INTEGER) IS NULL OR ac.cooperativa_id = CAST(:cooperativaId AS INTEGER))
        AND (CAST(:possuiRecorrencia AS BOOLEAN) IS NULL OR ac.possui_recorrencia = CAST(:possuiRecorrencia AS BOOLEAN))
        ORDER BY ac.data_inicio, ac.id_agendamento_coleta
    """,
    countQuery = """
    SELECT COUNT(*)
    FROM tb_agendamentos_coletas ac
    INNER JOIN tb_condominios c
     ON c.id_condominio = ac.condominio_id
    INNER JOIN tb_sindicos s 
        ON s.id_sindico = c.sindico_id    
    INNER JOIN tb_lkp_status_agendamentos sa
        ON sa.id_status = ac.status_agendamento_id
    WHERE s.usuario_id = :usuarioId
        AND (CAST(:status AS VARCHAR) IS NULL OR UPPER(sa.nome_status) = CAST(:status AS VARCHAR))
        AND (CAST(:dataInicio AS TIMESTAMP) IS NULL OR ac.data_inicio >= CAST(:dataInicio AS TIMESTAMP))
        AND (CAST(:dataFim AS TIMESTAMP) IS NULL OR ac.data_fim <= CAST(:dataFim AS TIMESTAMP))
        AND (CAST(:condominioId AS INTEGER) IS NULL OR ac.condominio_id = CAST(:condominioId AS INTEGER))
        AND (CAST(:cooperativaId AS INTEGER) IS NULL OR ac.cooperativa_id = CAST(:cooperativaId AS INTEGER))
        AND (CAST(:possuiRecorrencia AS BOOLEAN) IS NULL OR ac.possui_recorrencia = CAST(:possuiRecorrencia AS BOOLEAN))
    """,
    nativeQuery = true)
    Page<AgendamentoColetaEntity> buscarPorSindico(
      @Param("usuarioId") Integer usuarioId,
      @Param("status") String status,
      @Param("dataInicio") LocalDateTime dataInicio,
      @Param("dataFim") LocalDateTime dataFim,
      @Param("condominioId") Integer condominioId,
      @Param("cooperativaId") Integer cooperativaId,
      @Param("possuiRecorrencia") Boolean possuiRecorrencia,
      Pageable pageable);


 @Query(value = """
     SELECT ac.*
      FROM tb_agendamentos_coletas ac
      INNER JOIN tb_cooperativas co
          ON co.id_cooperativa = ac.cooperativa_id
      INNER JOIN tb_condominios c
          ON c.id_condominio = ac.condominio_id    
      INNER JOIN tb_lkp_status_agendamentos sa
          ON sa.id_status = ac.status_agendamento_id    
      WHERE co.usuario_id = :usuarioId
          AND (CAST(:status AS VARCHAR) IS NULL OR UPPER(sa.nome_status) = CAST(:status AS VARCHAR))
          AND (CAST(:dataInicio AS TIMESTAMP) IS NULL OR ac.data_inicio >= CAST(:dataInicio AS TIMESTAMP))
          AND (CAST(:dataFim AS TIMESTAMP) IS NULL OR ac.data_fim <= CAST(:dataFim AS TIMESTAMP))
          AND (CAST(:condominioId AS INTEGER) IS NULL OR ac.condominio_id = CAST(:condominioId AS INTEGER))
          AND (CAST(:possuiRecorrencia AS BOOLEAN) IS NULL OR ac.possui_recorrencia = CAST(:possuiRecorrencia AS BOOLEAN))    
        ORDER BY ac.data_inicio, ac.id_agendamento_coleta
      """,
      countQuery = """
      SELECT COUNT(*)
      FROM tb_agendamentos_coletas ac
      INNER JOIN tb_cooperativas co
          ON co.id_cooperativa = ac.cooperativa_id
      INNER JOIN tb_condominios c
          ON c.id_condominio = ac.condominio_id    
      INNER JOIN tb_lkp_status_agendamentos sa
          ON sa.id_status = ac.status_agendamento_id        
      WHERE co.usuario_id = :usuarioId
          AND (CAST(:condominioId AS INTEGER) IS NULL OR ac.condominio_id = CAST(:condominioId AS INTEGER))  
          AND (CAST(:status AS VARCHAR) IS NULL OR UPPER(sa.nome_status) = CAST(:status AS VARCHAR))
          AND (CAST(:dataInicio AS TIMESTAMP) IS NULL OR ac.data_inicio >= CAST(:dataInicio AS TIMESTAMP))
          AND (CAST(:dataFim AS TIMESTAMP) IS NULL OR ac.data_fim <= CAST(:dataFim AS TIMESTAMP))
          AND (CAST(:condominioId AS INTEGER) IS NULL OR ac.condominio_id = CAST(:condominioId AS INTEGER))
          AND (CAST(:possuiRecorrencia AS BOOLEAN) IS NULL OR ac.possui_recorrencia = CAST(:possuiRecorrencia AS BOOLEAN))
      """,
      nativeQuery = true)
      Page<AgendamentoColetaEntity> buscarPorCooperativa(
        @Param("usuarioId") Integer usuarioId,
        @Param("status") String status,
        @Param("dataInicio") LocalDateTime dataInicio,
        @Param("dataFim") LocalDateTime dataFim,
        @Param("condominioId") Integer condominioId,
        @Param("possuiRecorrencia") Boolean possuiRecorrencia,
        Pageable pageable);

  @Query(value = """
    SELECT ac.*
    FROM tb_agendamentos_coletas ac
    INNER JOIN tb_condominios c 
        ON c.id_condominio = ac.condominio_id
    INNER JOIN tb_sindicos s
        ON s.id_sindico = c.sindico_id
    INNER JOIN tb_lkp_status_agendamentos sa
        ON sa.id_status = ac.status_agendamento_id    
    WHERE s.usuario_id = :usuarioId
        AND UPPER(sa.nome_status) NOT IN ('CANCELADO', 'REALIZADO')
        AND ac.data_inicio > NOW()
        AND (CAST(:possuiRecorrencia AS BOOLEAN) IS NULL OR ac.possui_recorrencia = CAST(:possuiRecorrencia AS BOOLEAN))
        AND (CAST(:cooperativaId AS INTEGER) IS NULL OR ac.cooperativa_id = CAST(:cooperativaId AS INTEGER))
        AND (CAST(:dataInicio AS TIMESTAMP) IS NULL OR ac.data_inicio >= CAST(:dataInicio AS TIMESTAMP))
        AND (CAST(:dataFim AS TIMESTAMP) IS NULL OR ac.data_fim <= CAST(:dataFim AS TIMESTAMP))
    ORDER BY ac.data_inicio
    LIMIT 1
    """,
    nativeQuery = true)
    Optional<AgendamentoColetaEntity> buscarProximoAgendamentoPorSindico(
    @Param("usuarioId") Integer usuarioId, 
    @Param("dataInicio") LocalDateTime dataInicio,
    @Param("dataFim") LocalDateTime dataFim,
    @Param("condominioId") Integer condominioId,
    @Param("possuiRecorrencia") Boolean possuiRecorrencia, 
    @Param("cooperativaId") Integer cooperativaId);

  @Query(value = """
    SELECT ac.*
    FROM tb_agendamentos_coletas ac
    INNER JOIN tb_cooperativas co
        ON co.id_cooperativa = ac.cooperativa_id
    INNER JOIN tb_lkp_status_agendamentos sa
        ON sa.id_status = ac.status_agendamento_id    
    WHERE co.usuario_id = :usuarioId
        AND UPPER(sa.nome_status) NOT IN ('CANCELADO', 'REALIZADO')
        AND ac.data_inicio > NOW()
        AND (CAST(:possuiRecorrencia AS BOOLEAN) IS NULL OR ac.possui_recorrencia = CAST(:possuiRecorrencia AS BOOLEAN))
        AND (CAST(:condominioId AS INTEGER) IS NULL OR ac.condominio_id = CAST(:condominioId AS INTEGER))
        AND (CAST(:dataInicio AS TIMESTAMP) IS NULL OR ac.data_inicio >= CAST(:dataInicio AS TIMESTAMP))
        AND (CAST(:dataFim AS TIMESTAMP) IS NULL OR ac.data_fim <= CAST(:dataFim AS TIMESTAMP))
    ORDER BY ac.data_inicio
    LIMIT 1
    """,
    nativeQuery = true)
    Optional<AgendamentoColetaEntity> buscarProximoAgendamentoPorCooperativa(
    @Param("usuarioId") Integer usuarioId,
    @Param("dataInicio") LocalDateTime dataInicio,
    @Param("dataFim") LocalDateTime dataFim,
    @Param("possuiRecorrencia") Boolean possuiRecorrencia,
    @Param("condominioId") Integer condominioId
  );  



}
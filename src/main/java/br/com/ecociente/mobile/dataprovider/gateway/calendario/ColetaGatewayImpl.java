package br.com.ecociente.mobile.dataprovider.gateway.calendario;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.calendario.Coleta;
import br.com.ecociente.mobile.core.domain.calendario.NovaColeta;
import br.com.ecociente.mobile.core.domain.calendario.StatusType;
import br.com.ecociente.mobile.core.gateway.calendario.ColetaGateway;
import br.com.ecociente.mobile.dataprovider.mapper.calendario.ColetaPersistenciaMapper;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ColetaGatewayImpl implements ColetaGateway {

  private final JdbcTemplate jdbc;
  private final ColetaPersistenciaMapper mapper;

  @Override
  public boolean usuarioAtivo(Integer usuarioId) {
    return existe("""
        SELECT EXISTS (
          SELECT 1
          FROM tb_usuarios
          WHERE id_usuario = ?
            AND ativo = TRUE
        )
        """, usuarioId);
  }

  @Override
  public boolean condominioAtivo(Integer condominioId) {
    return existe("""
        SELECT EXISTS (
          SELECT 1
          FROM tb_condominios
          WHERE id_condominio = ?
            AND ativo = TRUE
        )
        """, condominioId);
  }

  @Override
  public boolean cooperativaAtiva(Integer cooperativaId) {
    return existe("""
        SELECT EXISTS (
          SELECT 1
          FROM tb_cooperativas c
          JOIN tb_usuarios u
            ON u.id_usuario = c.usuario_id
          WHERE c.id_cooperativa = ?
            AND u.ativo = TRUE
        )
        """, cooperativaId);
  }

  @Override
  public boolean sindicoResponsavel(
      Integer usuarioId,
      Integer condominioId) {

    return existe("""
        SELECT EXISTS (
          SELECT 1
          FROM tb_condominios c
          JOIN tb_sindicos s
            ON s.id_sindico = c.sindico_id
          JOIN tb_usuarios u
            ON u.id_usuario = s.usuario_id
          WHERE c.id_condominio = ?
            AND s.usuario_id = ?
            AND c.ativo = TRUE
            AND u.ativo = TRUE
        )
        """, condominioId, usuarioId);
  }

  @Override
  public boolean responsavelCooperativa(
      Integer usuarioId,
      Integer cooperativaId) {

    return existe("""
        SELECT EXISTS (
          SELECT 1
          FROM tb_cooperativas c
          JOIN tb_usuarios u
            ON u.id_usuario = c.usuario_id
          WHERE c.id_cooperativa = ?
            AND c.usuario_id = ?
            AND u.ativo = TRUE
        )
        """, cooperativaId, usuarioId);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Coleta criar(
      Integer usuarioId,
      String origem,
      NovaColeta entrada) {

    return jdbc.queryForObject("""
        INSERT INTO tb_agendamentos_coletas (
          condominio_id,
          cooperativa_id,
          status_agendamento_id,
          data_inicio,
          data_fim,
          possui_recorrencia,
          solicitado_por_usuario_id,
          origem_solicitacao
        )
        VALUES (?, ?, ?, ?, ?, FALSE, ?, ?)
        RETURNING *
        """,
        mapper,
        entrada.condominioId(),
        entrada.cooperativaId(),
        StatusType.AGENDADO.getId(),
        entrada.dataInicio(),
        entrada.dataFim(),
        usuarioId,
        origem);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<Coleta> buscarComBloqueio(Integer coletaId) {
    return jdbc.query("""
        SELECT *
        FROM tb_agendamentos_coletas
        WHERE id_agendamento_coleta = ?
        FOR UPDATE
        """,
        mapper,
        coletaId)
        .stream()
        .findFirst();
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Coleta atualizarStatus(
      Integer coletaId,
      StatusType status,
      Integer respondidoPorUsuarioId) {

    if (status == StatusType.REALIZADO) {
      return jdbc.queryForObject("""
          UPDATE tb_agendamentos_coletas
          SET status_agendamento_id = ?
          WHERE id_agendamento_coleta = ?
          RETURNING *
          """,
          mapper,
          status.getId(),
          coletaId);
    }

    return jdbc.queryForObject("""
        UPDATE tb_agendamentos_coletas
        SET status_agendamento_id = ?,
            respondido_por_usuario_id = ?,
            respondido_em = CURRENT_TIMESTAMP
        WHERE id_agendamento_coleta = ?
        RETURNING *
        """,
        mapper,
        status.getId(),
        respondidoPorUsuarioId,
        coletaId);
  }

  private boolean existe(String sql, Object... argumentos) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            sql,
            Boolean.class,
            argumentos));
  }
}
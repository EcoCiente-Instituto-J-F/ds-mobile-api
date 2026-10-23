package br.com.ecociente.mobile.dataprovider.gateway;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.AgendamentoAvaliavel;
import br.com.ecociente.mobile.core.domain.AvaliacaoColeta;
import br.com.ecociente.mobile.core.domain.NovaAvaliacaoColeta;
import br.com.ecociente.mobile.core.exception.AvaliacaoColetaException;
import br.com.ecociente.mobile.core.gateway.AvaliacaoColetaGateway;

@Component
public class AvaliacaoColetaGatewayImpl
    implements AvaliacaoColetaGateway {

  private final JdbcTemplate jdbc;

  public AvaliacaoColetaGatewayImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<AgendamentoAvaliavel> buscarAgendamento(
      Integer agendamentoId) {
    return jdbc.query("""
        SELECT ag.id_agendamento_coleta,
               s.usuario_id AS sindico_usuario_id,
               UPPER(st.nome_status) = 'REALIZADO' AS realizado
        FROM tb_agendamentos_coletas ag
        JOIN tb_condominios c
          ON c.id_condominio = ag.condominio_id
        LEFT JOIN tb_sindicos s
          ON s.id_sindico = c.sindico_id
        JOIN tb_lkp_status_agendamentos st
          ON st.id_status = ag.status_agendamento_id
        WHERE ag.id_agendamento_coleta = ?
        """,
        (rs, rowNum) -> new AgendamentoAvaliavel(
            rs.getInt("id_agendamento_coleta"),
            rs.getObject("sindico_usuario_id", Integer.class),
            rs.getBoolean("realizado")),
        agendamentoId).stream().findFirst();
  }

  @Override
  public Optional<Integer> buscarVisitaRealizada(
      Integer agendamentoId) {
    return jdbc.query("""
        SELECT id_visita_coleta
        FROM tb_visitas_coletas
        WHERE agendamento_coleta_id = ?
          AND foi_realizada = TRUE
        ORDER BY data_visita DESC, id_visita_coleta DESC
        LIMIT 1
        """,
        (rs, rowNum) -> rs.getInt("id_visita_coleta"),
        agendamentoId).stream().findFirst();
  }

  @Override
  public Integer criarVisitaRealizada(
      Integer agendamentoId) {
    return jdbc.queryForObject("""
        INSERT INTO tb_visitas_coletas (
            agendamento_coleta_id, data_visita,
            foi_realizada, houve_confirmacao, confirmado_em)
        SELECT id_agendamento_coleta,
               COALESCE(data_fim, data_inicio),
               TRUE, TRUE, now()
        FROM tb_agendamentos_coletas
        WHERE id_agendamento_coleta = ?
        RETURNING id_visita_coleta
        """,
        Integer.class,
        agendamentoId);
  }

  @Override
  public boolean visitaJaAvaliada(
      Integer visitaId) {
    return Boolean.TRUE.equals(jdbc.queryForObject("""
        SELECT EXISTS (
            SELECT 1
            FROM tb_avaliacoes_visitas_coletas
            WHERE visita_coleta_id = ?)
        """,
        Boolean.class,
        visitaId));
  }

  @Override
  public AvaliacaoColeta registrar(
      Integer visitaId,
      NovaAvaliacaoColeta avaliacao) {
    try {
      return jdbc.queryForObject("""
          INSERT INTO tb_avaliacoes_visitas_coletas (
              visita_coleta_id, usuario_avaliador_id, nota, comentario)
          VALUES (?, ?, ?, ?)
          RETURNING id_avaliacao, nota, comentario, avaliado_em
          """,
          (rs, rowNum) -> new AvaliacaoColeta(
              rs.getInt("id_avaliacao"),
              avaliacao.agendamentoId(),
              visitaId,
              rs.getInt("nota"),
              rs.getString("comentario"),
              rs.getObject("avaliado_em", OffsetDateTime.class)),
          visitaId,
          avaliacao.usuarioId(),
          avaliacao.nota(),
          avaliacao.comentario());
    } catch (DuplicateKeyException exception) {
      // duas avaliações ao mesmo tempo: a restrição única do banco decide
      throw AvaliacaoColetaException.jaAvaliada();
    }
  }
}

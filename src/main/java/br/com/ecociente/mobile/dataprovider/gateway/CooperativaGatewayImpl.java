package br.com.ecociente.mobile.dataprovider.gateway;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.Cooperativa;
import br.com.ecociente.mobile.core.gateway.CooperativaGateway;

@Component
public class CooperativaGatewayImpl implements CooperativaGateway {

  // Sem coordenadas no banco: "próxima" é a cooperativa da mesma cidade e estado do condomínio.
  private static final String PROXIMAS_DO_SINDICO = """
      FROM tb_cooperativas c
      JOIN tb_enderecos e
        ON e.id_endereco = c.endereco_id
      LEFT JOIN tb_usuarios u
        ON u.id_usuario = c.usuario_id
      WHERE (u.id_usuario IS NULL OR u.ativo = TRUE)
        AND EXISTS (
          SELECT 1
          FROM tb_sindicos s
          JOIN tb_condominios cond
            ON cond.sindico_id = s.id_sindico
           AND cond.ativo = TRUE
          JOIN tb_enderecos ec
            ON ec.id_endereco = cond.endereco_id
          WHERE s.usuario_id = ?
            AND lower(trim(ec.cidade)) = lower(trim(e.cidade))
            AND upper(trim(ec.estado)) = upper(trim(e.estado)))
      """;

  private final JdbcTemplate jdbc;

  public CooperativaGatewayImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean sindicoTemCondominioAtivo(Integer usuarioId) {
    return Boolean.TRUE.equals(jdbc.queryForObject("""
        SELECT EXISTS (
          SELECT 1
          FROM tb_sindicos s
          JOIN tb_condominios cond
            ON cond.sindico_id = s.id_sindico
           AND cond.ativo = TRUE
          WHERE s.usuario_id = ?)
        """,
        Boolean.class,
        usuarioId));
  }

  @Override
  public long contarProximas(Integer usuarioId) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) " + PROXIMAS_DO_SINDICO,
        Long.class,
        usuarioId);
  }

  @Override
  public List<Cooperativa> listarProximas(
      Integer usuarioId,
      long deslocamento,
      int limite) {
    return jdbc.query("""
        SELECT c.id_cooperativa,
               c.nome_cooperativa,
               c.email_cooperativa,
               c.telefone_cooperativa,
               e.cep,
               e.logradouro,
               e.numero,
               e.complemento,
               e.cidade,
               e.estado,
               a.media,
               COALESCE(a.total, 0) AS total_avaliacoes
        """
        + PROXIMAS_DO_SINDICO.replace(
            "WHERE (u.id_usuario",
            """
            LEFT JOIN (
              SELECT ag.cooperativa_id,
                     ROUND(AVG(av.nota), 2) AS media,
                     COUNT(*) AS total
              FROM tb_avaliacoes_visitas_coletas av
              JOIN tb_visitas_coletas v
                ON v.id_visita_coleta = av.visita_coleta_id
              JOIN tb_agendamentos_coletas ag
                ON ag.id_agendamento_coleta = v.agendamento_coleta_id
              GROUP BY ag.cooperativa_id) a
              ON a.cooperativa_id = c.id_cooperativa
            WHERE (u.id_usuario""")
        + """
        ORDER BY c.nome_cooperativa, c.id_cooperativa
        LIMIT ? OFFSET ?
        """,
        (rs, rowNum) -> new Cooperativa(
            rs.getInt("id_cooperativa"),
            rs.getString("nome_cooperativa"),
            rs.getString("email_cooperativa"),
            rs.getString("telefone_cooperativa"),
            rs.getString("cep"),
            rs.getString("logradouro"),
            rs.getString("numero"),
            rs.getString("complemento"),
            rs.getString("cidade"),
            rs.getString("estado"),
            rs.getBigDecimal("media"),
            rs.getLong("total_avaliacoes")),
        usuarioId,
        limite,
        deslocamento);
  }
}

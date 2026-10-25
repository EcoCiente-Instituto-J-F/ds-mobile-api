package br.com.ecociente.mobile.dataprovider.gateway;

import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import br.com.ecociente.mobile.core.domain.CondominioAtendido;
import br.com.ecociente.mobile.core.gateway.CondominioAtendidoGateway;

@Component
public class CondominioAtendidoGatewayImpl implements CondominioAtendidoGateway {
  private final JdbcTemplate jdbc;

  public CondominioAtendidoGatewayImpl(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  private static final String ATENDIDOS = """
      FROM tb_condominios c
      WHERE c.ativo = TRUE AND EXISTS (
        SELECT 1 FROM tb_agendamentos_coletas ag
        JOIN tb_lkp_status_agendamentos s ON s.id_status = ag.status_agendamento_id
        WHERE ag.condominio_id = c.id_condominio AND ag.cooperativa_id = ?
          AND UPPER(s.nome_status) IN ('CONFIRMADO', 'REALIZADO')
      )
      """;

  @Override
  public long contar(Integer cooperativaId) {
    return jdbc.queryForObject("SELECT COUNT(*) " + ATENDIDOS, Long.class, cooperativaId);
  }

  @Override
  public List<CondominioAtendido> listar(Integer cooperativaId, long deslocamento, int limite) {
    return jdbc.query("""
        WITH pagina AS (
          SELECT c.id_condominio, c.nome_condominio, c.endereco_id,
            (SELECT COUNT(*) FROM tb_torres t WHERE t.condominio_id = c.id_condominio) AS total_torres
        """ + ATENDIDOS + """
          ORDER BY c.nome_condominio, c.id_condominio LIMIT ? OFFSET ?
        )
        SELECT p.*, e.cep, e.estado, e.cidade, e.logradouro, e.numero, e.complemento,
          ARRAY(
            SELECT DISTINCT d.nome_dia
            FROM tb_agendamentos_coletas ag
            JOIN tb_lkp_status_agendamentos s ON s.id_status = ag.status_agendamento_id
            JOIN tb_rel_recorrencias_agendamentos r ON r.agendamento_coleta_id = ag.id_agendamento_coleta
            JOIN tb_lkp_dias_semanas d ON d.id_dia_semana = r.dia_semana_id
            WHERE ag.condominio_id = p.id_condominio AND ag.cooperativa_id = ?
              AND ag.possui_recorrencia = TRUE AND UPPER(s.nome_status) = 'CONFIRMADO'
            ORDER BY d.nome_dia
          ) AS dias_coleta
        FROM pagina p LEFT JOIN tb_enderecos e ON e.id_endereco = p.endereco_id
        ORDER BY p.nome_condominio, p.id_condominio
        """, (rs, linha) -> {
          var dias = rs.getArray("dias_coleta");
          List<String> nomes;
          try { nomes = List.copyOf(Arrays.asList((String[]) dias.getArray())); }
          finally { dias.free(); }
          var endereco = rs.getObject("endereco_id") == null ? null : new CondominioAtendido.Endereco(
              rs.getString("cep"), rs.getString("estado"), rs.getString("cidade"),
              rs.getString("logradouro"), rs.getString("numero"), rs.getString("complemento"));
          return new CondominioAtendido(rs.getInt("id_condominio"), rs.getString("nome_condominio"),
              endereco, rs.getLong("total_torres"), nomes);
        }, cooperativaId, limite, deslocamento, cooperativaId);
  }
}

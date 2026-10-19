package br.com.ecociente.mobile.dataprovider.gateway;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.AvaliacaoCooperativa;
import br.com.ecociente.mobile.core.domain.ResumoAvaliacoesCooperativa;
import br.com.ecociente.mobile.core.gateway.AvaliacaoCooperativaGateway;

@Component
public class AvaliacaoCooperativaGatewayImpl
    implements AvaliacaoCooperativaGateway {

    private final JdbcTemplate jdbc;

    public AvaliacaoCooperativaGatewayImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Integer> buscarResponsavelAtivo(
        Integer cooperativaId
    ) {
        return jdbc.query("""
            SELECT c.usuario_id
            FROM tb_cooperativas c
            JOIN tb_usuarios u
              ON u.id_usuario = c.usuario_id
            WHERE c.id_cooperativa = ?
              AND u.ativo = TRUE
            """,
            (rs, rowNum) -> rs.getInt("usuario_id"),
            cooperativaId
        ).stream().findFirst();
    }

    @Override
    public ResumoAvaliacoesCooperativa consultarResumo(
        Integer cooperativaId
    ) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) AS total,
                   ROUND(AVG(a.nota), 2) AS media
            FROM tb_avaliacoes_visitas_coletas a
            JOIN tb_visitas_coletas v
              ON v.id_visita_coleta = a.visita_coleta_id
            JOIN tb_agendamentos_coletas ag
              ON ag.id_agendamento_coleta = v.agendamento_coleta_id
            WHERE ag.cooperativa_id = ?
            """,
            (rs, rowNum) -> new ResumoAvaliacoesCooperativa(
                rs.getLong("total"),
                rs.getBigDecimal("media")
            ),
            cooperativaId
        );
    }

    @Override
    public List<AvaliacaoCooperativa> listar(
        Integer cooperativaId,
        long deslocamento,
        int limite
    ) {
        return jdbc.query("""
            SELECT a.id_avaliacao,
                   v.id_visita_coleta,
                   ag.id_agendamento_coleta,
                   c.id_condominio,
                   c.nome_condominio,
                   a.nota,
                   a.comentario,
                   a.avaliado_em
            FROM tb_avaliacoes_visitas_coletas a
            JOIN tb_visitas_coletas v
              ON v.id_visita_coleta = a.visita_coleta_id
            JOIN tb_agendamentos_coletas ag
              ON ag.id_agendamento_coleta = v.agendamento_coleta_id
            JOIN tb_condominios c
              ON c.id_condominio = ag.condominio_id
            WHERE ag.cooperativa_id = ?
            ORDER BY a.avaliado_em DESC, a.id_avaliacao DESC
            LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> new AvaliacaoCooperativa(
                rs.getInt("id_avaliacao"),
                rs.getInt("id_visita_coleta"),
                rs.getInt("id_agendamento_coleta"),
                rs.getInt("id_condominio"),
                rs.getString("nome_condominio"),
                rs.getInt("nota"),
                rs.getString("comentario"),
                rs.getObject(
                    "avaliado_em",
                    OffsetDateTime.class
                )
            ),
            cooperativaId,
            limite,
            deslocamento
        );
    }
}
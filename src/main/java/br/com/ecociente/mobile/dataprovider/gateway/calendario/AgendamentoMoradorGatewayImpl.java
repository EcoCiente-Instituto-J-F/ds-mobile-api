package br.com.ecociente.mobile.dataprovider.gateway.calendario;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;
import br.com.ecociente.mobile.core.domain.calendario.AgendamentoFiltro;
import br.com.ecociente.mobile.core.domain.calendario.StatusType;
import br.com.ecociente.mobile.core.exception.calendario.AgendamentoInvalidoException;
import br.com.ecociente.mobile.core.gateway.calendario.AgendamentoMoradorGateway;

@Component
public class AgendamentoMoradorGatewayImpl
    implements AgendamentoMoradorGateway {

  private static final String COLUNAS = """
      SELECT ac.id_agendamento_coleta,
             ac.condominio_id,
             ac.cooperativa_id,
             ac.data_inicio,
             ac.data_fim,
             ac.possui_recorrencia,
             ac.solicitado_por_usuario_id,
             ac.origem_solicitacao,
             co.nome_cooperativa,
             c.nome_condominio,
             sa.nome_status
      """;

  private static final String BASE = """
      FROM tb_agendamentos_coletas ac
      JOIN tb_condominios c
        ON c.id_condominio = ac.condominio_id
      LEFT JOIN tb_cooperativas co
        ON co.id_cooperativa = ac.cooperativa_id
      JOIN tb_lkp_status_agendamentos sa
        ON sa.id_status = ac.status_agendamento_id
      WHERE c.ativo = TRUE
        AND EXISTS (
            SELECT 1
            FROM tb_moradores m
            JOIN tb_usuarios u
              ON u.id_usuario = m.usuario_id
            JOIN tb_rel_usuarios_condominios r
              ON r.usuario_id = m.usuario_id
             AND r.condominio_id = m.condominio_id
            WHERE m.usuario_id = :usuarioId
              AND m.condominio_id = ac.condominio_id
              AND u.ativo = TRUE
              AND r.aprovado = TRUE
              AND r.data_saida IS NULL
        )
      """;

  private static final Map<String, String> ORDENACOES = Map.of(
      "id", "ac.id_agendamento_coleta",
      "dataInicio", "ac.data_inicio",
      "dataFim", "ac.data_fim",
      "condominioId", "ac.condominio_id",
      "cooperativaId", "ac.cooperativa_id",
      "statusAgendamento", "sa.nome_status",
      "possuiRecorrencia", "ac.possui_recorrencia");

  private final NamedParameterJdbcTemplate jdbc;
  private final ZoneId zona;

  public AgendamentoMoradorGatewayImpl(
      NamedParameterJdbcTemplate jdbc,
      @Value("${app.timezone:America/Sao_Paulo}") String timezone) {

    this.jdbc = jdbc;
    this.zona = ZoneId.of(timezone);
  }

  @Override
  public Page<AgendamentoColeta> listar(
      Integer usuarioId,
      AgendamentoFiltro filtro,
      Pageable pageable) {

    validarEntrada(usuarioId, filtro);

    var parametros = new MapSqlParameterSource(
        "usuarioId", usuarioId);

    String where = BASE + filtros(filtro, parametros);
    String sql = COLUNAS + where + ordenar(pageable.getSort());

    if (pageable.isPaged()) {
      sql += " LIMIT :limite OFFSET :deslocamento";

      parametros.addValue("limite", pageable.getPageSize());
      parametros.addValue("deslocamento", pageable.getOffset());
    }

    var registros = jdbc.query(
        sql,
        parametros,
        (rs, rowNum) -> mapear(rs));

    if (pageable.isUnpaged()) {
      return new PageImpl<>(registros);
    }

    Long total = jdbc.queryForObject(
        "SELECT COUNT(*) " + where,
        parametros,
        Long.class);

    return new PageImpl<>(
        registros,
        pageable,
        total == null ? 0L : total);
  }

  @Override
  public Optional<AgendamentoColeta> buscarProximo(
      Integer usuarioId,
      AgendamentoFiltro filtro) {

    validarEntrada(usuarioId, filtro);

    var parametros = new MapSqlParameterSource(
        "usuarioId", usuarioId);

    String sql = COLUNAS
        + BASE
        + filtros(filtro, parametros)
        + """
            AND UPPER(sa.nome_status) IN ('AGENDADO', 'CONFIRMADO')
            AND ac.data_inicio > CURRENT_TIMESTAMP
            ORDER BY ac.data_inicio, ac.id_agendamento_coleta
            LIMIT 1
            """;

    return jdbc.query(
        sql,
        parametros,
        (rs, rowNum) -> mapear(rs))
        .stream()
        .findFirst();
  }

  private String filtros(
      AgendamentoFiltro filtro,
      MapSqlParameterSource parametros) {

    StringBuilder sql = new StringBuilder();

    if (filtro.status() != null) {
      sql.append(" AND UPPER(sa.nome_status) = :status ");
      parametros.addValue("status", filtro.status().name());
    }

    if (filtro.condominioId() != null) {
      sql.append(" AND ac.condominio_id = :condominioId ");
      parametros.addValue(
          "condominioId", filtro.condominioId());
    }

    if (filtro.cooperativaId() != null) {
      sql.append(" AND ac.cooperativa_id = :cooperativaId ");
      parametros.addValue(
          "cooperativaId", filtro.cooperativaId());
    }

    if (filtro.possuiRecorrencia() != null) {
      sql.append(" AND ac.possui_recorrencia = :recorrencia ");
      parametros.addValue(
          "recorrencia", filtro.possuiRecorrencia());
    }

    if (filtro.dataInicio() != null) {
      sql.append(" AND ac.data_inicio >= :dataInicio ");
      parametros.addValue(
          "dataInicio",
          filtro.dataInicio().atZone(zona).toOffsetDateTime(),
          Types.TIMESTAMP_WITH_TIMEZONE);
    }

    if (filtro.dataFim() != null) {
      sql.append("""
           AND COALESCE(ac.data_fim, ac.data_inicio) <= :dataFim
          """);
      parametros.addValue(
          "dataFim",
          filtro.dataFim().atZone(zona).toOffsetDateTime(),
          Types.TIMESTAMP_WITH_TIMEZONE);
    }

    return sql.toString();
  }

  private String ordenar(Sort sort) {
    var partes = new ArrayList<String>();

    for (Sort.Order ordem : sort) {
      String coluna = ORDENACOES.get(ordem.getProperty());

      if (coluna == null || ordem.isIgnoreCase()) {
        throw new AgendamentoInvalidoException(
            "Ordenação não suportada: " + ordem.getProperty());
      }

      String expressao = coluna
          + (ordem.isAscending() ? " ASC" : " DESC");

      expressao += switch (ordem.getNullHandling()) {
        case NULLS_FIRST -> " NULLS FIRST";
        case NULLS_LAST -> " NULLS LAST";
        case NATIVE -> "";
      };

      partes.add(expressao);
    }

    if (partes.isEmpty()) {
      partes.add("ac.data_inicio ASC");
    }

    if (sort.getOrderFor("id") == null) {
      partes.add("ac.id_agendamento_coleta ASC");
    }

    return " ORDER BY " + String.join(", ", partes);
  }

  private AgendamentoColeta mapear(ResultSet rs)
      throws SQLException {

    return AgendamentoColeta.builder()
        .id(rs.getInt("id_agendamento_coleta"))
        .condominioId(rs.getInt("condominio_id"))
        .cooperativaId(rs.getInt("cooperativa_id"))
        .statusAgendamento(StatusType.valueOf(
            rs.getString("nome_status")
                .toUpperCase(java.util.Locale.ROOT)))
        .dataInicio(data(rs, "data_inicio"))
        .dataFim(data(rs, "data_fim"))
        .possuiRecorrencia(rs.getBoolean("possui_recorrencia"))
        .solicitadoPorUsuarioId(rs.getObject("solicitado_por_usuario_id", Integer.class))
        .origemSolicitacao(rs.getString("origem_solicitacao"))
        .cooperativaNome(rs.getString("nome_cooperativa"))
        .condominioNome(rs.getString("nome_condominio"))
        .build();
  }

  private LocalDateTime data(
      ResultSet rs,
      String coluna) throws SQLException {

    OffsetDateTime valor = rs.getObject(
        coluna, OffsetDateTime.class);

    return valor == null
        ? null
        : valor.atZoneSameInstant(zona).toLocalDateTime();
  }

  private void validarEntrada(
      Integer usuarioId,
      AgendamentoFiltro filtro) {

    if (usuarioId == null || usuarioId <= 0 || filtro == null) {
      throw new AgendamentoInvalidoException(
          "Dados de consulta inválidos");
    }

    if (filtro.dataInicio() != null
        && filtro.dataFim() != null
        && filtro.dataFim().isBefore(filtro.dataInicio())) {
      throw new AgendamentoInvalidoException(
          "Data final deve ser igual ou posterior à inicial");
    }
  }
}
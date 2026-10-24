package br.com.ecociente.mobile.dataprovider.gateway;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ecociente.mobile.core.domain.DashboardSindico;
import br.com.ecociente.mobile.core.domain.DashboardSindico.MaterialPorCategoria;
import br.com.ecociente.mobile.core.domain.DashboardSindico.Materiais;
import br.com.ecociente.mobile.core.domain.DashboardSindico.NotaPorQuiz;
import br.com.ecociente.mobile.core.domain.DashboardSindico.Pontos;
import br.com.ecociente.mobile.core.domain.DashboardSindico.Quizzes;
import br.com.ecociente.mobile.core.gateway.DashboardSindicoGateway;

@Component
public class DashboardSindicoGatewayImpl
    implements DashboardSindicoGateway {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final JdbcTemplate jdbc;

  public DashboardSindicoGatewayImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<DashboardSindico> buscarPorCondominio(
      Integer condominioId) {
    return jdbc.query("""
        SELECT *
        FROM vw_dashboard_sindico
        WHERE id_condominio = ?
        """,
        (rs, rowNum) -> new DashboardSindico(
            rs.getInt("id_condominio"),
            rs.getString("nome_condominio"),
            rs.getObject("sindico_usuario_id", Integer.class),
            rs.getString("nome_sindico"),
            rs.getLong("total_moradores"),
            new Materiais(
                rs.getLong("materiais_reciclados"),
                rs.getLong("postagens_em_validacao"),
                rs.getLong("postagens_aguardando_sindico"),
                rs.getLong("postagens_reprovadas"),
                categorias(rs.getString("materiais_por_categoria"))),
            new Pontos(
                rs.getLong("pontos_total"),
                rs.getLong("pontos_postagens"),
                rs.getLong("pontos_quizzes"),
                rs.getLong("pontos_mes_atual")),
            new Quizzes(
                rs.getLong("tentativas_quiz_concluidas"),
                rs.getLong("tentativas_quiz_aprovadas"),
                rs.getLong("moradores_com_quiz"),
                rs.getBigDecimal("nota_media_quizzes"),
                rs.getBigDecimal("taxa_aprovacao_quizzes"),
                notas(rs.getString("notas_por_quiz")))),
        condominioId).stream().findFirst();
  }

  // As listas vêm da view como jsonb: [{ id_categoria, categoria, cor, quantidade }]
  private static List<MaterialPorCategoria> categorias(String json) {
    List<MaterialPorCategoria> lista = new ArrayList<>();

    for (JsonNode item : ler(json)) {
      lista.add(new MaterialPorCategoria(
          item.path("id_categoria").asInt(),
          texto(item, "categoria"),
          texto(item, "cor"),
          item.path("quantidade").asLong()));
    }

    return lista;
  }

  // [{ id_quiz, quiz, tentativas, aprovadas, nota_media }]
  private static List<NotaPorQuiz> notas(String json) {
    List<NotaPorQuiz> lista = new ArrayList<>();

    for (JsonNode item : ler(json)) {
      lista.add(new NotaPorQuiz(
          item.path("id_quiz").asInt(),
          texto(item, "quiz"),
          item.path("tentativas").asLong(),
          item.path("aprovadas").asLong(),
          item.hasNonNull("nota_media")
              ? new BigDecimal(item.get("nota_media").asText())
              : null));
    }

    return lista;
  }

  private static JsonNode ler(String json) {
    try {
      return JSON.readTree(json == null ? "[]" : json);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static String texto(JsonNode item, String campo) {
    return item.hasNonNull(campo) ? item.get(campo).asText() : null;
  }
}

package br.com.ecociente.mobile.dataprovider.gateway;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import br.com.ecociente.mobile.core.domain.Conversa;
import br.com.ecociente.mobile.core.domain.MensagemConversa;
import br.com.ecociente.mobile.core.gateway.ChatGateway;

@Component
public class ChatGatewayImpl implements ChatGateway {
  private final NamedParameterJdbcTemplate jdbc;
  public ChatGatewayImpl(JdbcTemplate jdbc) { this.jdbc = new NamedParameterJdbcTemplate(jdbc); }

  private static final String PARTES = """
      FROM tb_condominios cond
      LEFT JOIN tb_sindicos s ON s.id_sindico = cond.sindico_id
      JOIN tb_cooperativas coop ON coop.id_cooperativa = :cooperativa
      JOIN tb_usuarios u ON u.id_usuario = :usuario AND u.ativo = TRUE
      JOIN tb_usuarios dono ON dono.id_usuario = coop.usuario_id AND dono.ativo = TRUE
      WHERE cond.id_condominio = :condominio AND cond.ativo = TRUE
      """;
  private static final String LADO = """
      CASE WHEN s.usuario_id = :usuario AND coop.usuario_id = :usuario THEN 'AMBIGUO'
           WHEN s.usuario_id = :usuario THEN 'CONDOMINIO'
           WHEN coop.usuario_id = :usuario THEN 'COOPERATIVA' END
      """;
  private static final String COLUNAS = """
      SELECT c.*, cond.nome_condominio, coop.nome_cooperativa,
        (SELECT COUNT(*) FROM tb_mensagens_conversas m
         WHERE m.conversa_id = c.id_conversa AND m.lida_em IS NULL
           AND m.remetente_tipo <> CASE WHEN s.usuario_id = :usuario
             THEN 'CONDOMINIO' ELSE 'COOPERATIVA' END) AS nao_lidas
      """;
  private static final String JOINS = """
      FROM tb_conversas c
      JOIN tb_condominios cond ON cond.id_condominio = c.condominio_id
      LEFT JOIN tb_sindicos s ON s.id_sindico = cond.sindico_id
      JOIN tb_cooperativas coop ON coop.id_cooperativa = c.cooperativa_id
      """;
  private static final String FILTRO_LISTA = """
      JOIN tb_usuarios u ON u.id_usuario = :usuario AND u.ativo = TRUE
      JOIN tb_usuarios dono ON dono.id_usuario = coop.usuario_id AND dono.ativo = TRUE
      WHERE cond.ativo = TRUE AND c.ativa = :ativa AND (
        COALESCE(s.usuario_id = :usuario, FALSE) <> COALESCE(coop.usuario_id = :usuario, FALSE)
      )
      """;

  @Override
  public Optional<String> buscarLado(Integer usuarioId, Integer condominioId, Integer cooperativaId) {
    return jdbc.query("SELECT " + LADO + PARTES, parametros(usuarioId)
        .addValue("condominio", condominioId).addValue("cooperativa", cooperativaId),
        (rs, n) -> rs.getString(1)).stream().filter(java.util.Objects::nonNull).findFirst();
  }

  @Override
  public Integer criarOuObter(Integer condominioId, Integer cooperativaId) {
    var p = new MapSqlParameterSource("condominio", condominioId).addValue("cooperativa", cooperativaId);
    jdbc.update("""
        INSERT INTO tb_conversas(condominio_id, cooperativa_id)
        VALUES (:condominio, :cooperativa)
        ON CONFLICT (condominio_id, cooperativa_id) DO NOTHING
        """, p);
    return jdbc.queryForObject("""
        SELECT id_conversa FROM tb_conversas
        WHERE condominio_id = :condominio AND cooperativa_id = :cooperativa
        """, p, Integer.class);
  }

  @Override
  public Optional<Conversa> buscar(Integer conversaId, Integer usuarioId, boolean bloquear) {
    return jdbc.query(COLUNAS + JOINS + " WHERE c.id_conversa = :id "
        + (bloquear ? " FOR UPDATE OF c" : ""), parametros(usuarioId).addValue("id", conversaId),
        this::conversa).stream().findFirst();
  }
  @Override
  public long contarConversas(Integer usuarioId, boolean arquivadas) {
    return jdbc.queryForObject("SELECT COUNT(*) " + JOINS + FILTRO_LISTA,
        parametros(usuarioId).addValue("ativa", !arquivadas), Long.class);
  }
  @Override
  public List<Conversa> listarConversas(Integer usuarioId, boolean arquivadas, long offset, int limite) {
    return jdbc.query(COLUNAS + JOINS + FILTRO_LISTA + """
        ORDER BY COALESCE(c.ultima_mensagem_em, c.criada_em) DESC, c.id_conversa DESC
        LIMIT :limite OFFSET :offset
        """, parametros(usuarioId).addValue("ativa", !arquivadas)
        .addValue("limite", limite).addValue("offset", offset), this::conversa);
  }
  @Override
  public long contarMensagens(Integer conversaId) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM tb_mensagens_conversas WHERE conversa_id = :id",
        new MapSqlParameterSource("id", conversaId), Long.class);
  }
  @Override
  public List<MensagemConversa> listarMensagens(Integer conversaId, long offset, int limite) {
    return jdbc.query("""
        SELECT * FROM tb_mensagens_conversas WHERE conversa_id = :id
        ORDER BY enviada_em DESC, id_mensagem DESC LIMIT :limite OFFSET :offset
        """, new MapSqlParameterSource("id", conversaId).addValue("offset", offset).addValue("limite", limite), this::mensagem);
  }
  @Override
  public MensagemConversa enviar(Integer conversaId, Integer usuarioId, String lado, String conteudo) {
    var p = parametros(usuarioId).addValue("id", conversaId).addValue("lado", lado).addValue("conteudo", conteudo);
    var mensagem = jdbc.queryForObject("""
        INSERT INTO tb_mensagens_conversas(conversa_id, remetente_usuario_id, remetente_tipo, conteudo, enviada_em)
        VALUES (:id, :usuario, :lado, :conteudo, clock_timestamp()) RETURNING *
        """, p, this::mensagem);
    jdbc.update("UPDATE tb_conversas SET ultima_mensagem_em = :enviada WHERE id_conversa = :id",
        p.addValue("enviada", mensagem.enviadaEm()));
    return mensagem;
  }
  @Override
  public int marcarLeitura(Integer conversaId, String lado, Long ateMensagemId) {
    return jdbc.update("""
        UPDATE tb_mensagens_conversas SET lida_em = GREATEST(clock_timestamp(), enviada_em)
        WHERE conversa_id = :id AND remetente_tipo <> :lado AND lida_em IS NULL
        """ + (ateMensagemId == null ? "" : " AND id_mensagem <= :ate"),
        new MapSqlParameterSource("id", conversaId).addValue("lado", lado).addValue("ate", ateMensagemId));
  }
  @Override
  public void definirAtiva(Integer conversaId, boolean ativa) {
    jdbc.update("UPDATE tb_conversas SET ativa = :ativa WHERE id_conversa = :id",
        new MapSqlParameterSource("id", conversaId).addValue("ativa", ativa));
  }
  private MapSqlParameterSource parametros(Integer usuarioId) { return new MapSqlParameterSource("usuario", usuarioId); }
  private Conversa conversa(ResultSet rs, int n) throws SQLException {
    return new Conversa(rs.getInt("id_conversa"), rs.getInt("condominio_id"), rs.getInt("cooperativa_id"),
        rs.getString("nome_condominio"), rs.getString("nome_cooperativa"), rs.getObject("criada_em", OffsetDateTime.class),
        rs.getObject("ultima_mensagem_em", OffsetDateTime.class), rs.getBoolean("ativa"), rs.getLong("nao_lidas"));
  }
  private MensagemConversa mensagem(ResultSet rs, int n) throws SQLException {
    return new MensagemConversa(rs.getLong("id_mensagem"), rs.getInt("conversa_id"), rs.getInt("remetente_usuario_id"),
        rs.getString("remetente_tipo"), rs.getString("conteudo"), rs.getObject("enviada_em", OffsetDateTime.class),
        rs.getObject("lida_em", OffsetDateTime.class));
  }
}

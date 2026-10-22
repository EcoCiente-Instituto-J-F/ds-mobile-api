package br.com.ecociente.mobile.dataprovider.gateway;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.CategoriaResiduo;
import br.com.ecociente.mobile.core.domain.ContextoCondominialQuiz;
import br.com.ecociente.mobile.core.domain.Postagem;
import br.com.ecociente.mobile.core.exception.PostagemException;
import br.com.ecociente.mobile.core.gateway.PostagemGateway;

@Component
public class PostagemGatewayImpl implements PostagemGateway {

  private static final String SELECT_POSTAGEM = """
      SELECT p.id_postagem,
             p.usuario_id,
             u.nome_usuario,
             p.condominio_id,
             p.torre_id,
             p.categoria_id,
             c.nome_categoria,
             p.url_foto,
             p.capturada_em,
             p.data_postagem,
             s.nome_status,
             p.saldo_confianca,
             p.data_limite_analise,
             p.resolvido_em
      FROM tb_postagens p
      JOIN tb_usuarios u
        ON u.id_usuario = p.usuario_id
      JOIN tb_lkp_categorias_residuos c
        ON c.id_categoria = p.categoria_id
      JOIN tb_lkp_status_validacoes_postagens s
        ON s.id_status_validacao = p.status_validacao_id
      """;

  private static final RowMapper<Postagem> MAPPER = (rs, rowNum) -> new Postagem(
      rs.getInt("id_postagem"),
      rs.getInt("usuario_id"),
      rs.getString("nome_usuario"),
      rs.getInt("condominio_id"),
      rs.getObject("torre_id", Integer.class),
      rs.getInt("categoria_id"),
      rs.getString("nome_categoria"),
      rs.getString("url_foto"),
      rs.getObject("capturada_em", OffsetDateTime.class),
      rs.getObject("data_postagem", OffsetDateTime.class),
      rs.getString("nome_status"),
      rs.getInt("saldo_confianca"),
      rs.getObject("data_limite_analise", OffsetDateTime.class),
      rs.getObject("resolvido_em", OffsetDateTime.class));

  private final JdbcTemplate jdbc;

  public PostagemGatewayImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<ContextoCondominialQuiz> buscarVinculoAprovado(
      Integer usuarioId,
      Integer condominioId) {
    return jdbc.query("""
        SELECT m.condominio_id,
               m.torre_id,
               t.condominio_id AS condominio_torre_id
        FROM tb_moradores m
        JOIN tb_condominios c
          ON c.id_condominio = m.condominio_id
        JOIN tb_rel_usuarios_condominios r
          ON r.usuario_id = m.usuario_id
         AND r.condominio_id = m.condominio_id
        LEFT JOIN tb_torres t
          ON t.id_torre = m.torre_id
        WHERE m.usuario_id = ?
          AND m.condominio_id = ?
          AND c.ativo = TRUE
          AND r.aprovado = TRUE
          AND r.data_saida IS NULL
        """,
        (rs, rowNum) -> {
          Integer torreId = rs.getObject("torre_id", Integer.class);
          Integer condominioTorreId = rs.getObject(
              "condominio_torre_id", Integer.class);

          if (torreId != null
              && !condominioId.equals(condominioTorreId)) {
            throw new PostagemException(
                PostagemException.Tipo.CONFLITO,
                "TORRE_CONDOMINIO_INCONSISTENTE",
                "A torre do morador não pertence ao condomínio");
          }

          return new ContextoCondominialQuiz(
              rs.getInt("condominio_id"),
              torreId);
        },
        usuarioId,
        condominioId).stream().findFirst();
  }

  @Override
  public boolean categoriaExiste(Integer categoriaId) {
    return Boolean.TRUE.equals(jdbc.queryForObject(
        "SELECT EXISTS (SELECT 1 FROM tb_lkp_categorias_residuos"
            + " WHERE id_categoria = ?)",
        Boolean.class,
        categoriaId));
  }

  @Override
  public List<CategoriaResiduo> listarCategorias() {
    return jdbc.query("""
        SELECT id_categoria,
               nome_categoria,
               descricao_material,
               pontos_base
        FROM tb_lkp_categorias_residuos
        WHERE permite_reciclagem = TRUE
        ORDER BY nome_categoria, id_categoria
        """,
        (rs, rowNum) -> new CategoriaResiduo(
            rs.getInt("id_categoria"),
            rs.getString("nome_categoria"),
            rs.getString("descricao_material"),
            rs.getInt("pontos_base")));
  }

  @Override
  public boolean hashFotoJaUsado(String hashFoto) {
    return Boolean.TRUE.equals(jdbc.queryForObject(
        "SELECT EXISTS (SELECT 1 FROM tb_postagens WHERE hash_foto = ?)",
        Boolean.class,
        hashFoto));
  }

  @Override
  public Integer inserir(
      Integer usuarioId,
      Integer condominioId,
      Integer torreId,
      Integer categoriaId,
      String urlFoto,
      String hashFoto,
      OffsetDateTime capturadaEm) {
    // Status, saldo e prazo de análise ficam com o padrão do banco (em validação, 24 h).
    return jdbc.queryForObject("""
        INSERT INTO tb_postagens (
            usuario_id, condominio_id, torre_id, categoria_id,
            url_foto, hash_foto, capturada_em)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        RETURNING id_postagem
        """,
        Integer.class,
        usuarioId,
        condominioId,
        torreId,
        categoriaId,
        urlFoto,
        hashFoto,
        capturadaEm);
  }

  @Override
  public Optional<Postagem> buscarPorId(Integer postagemId) {
    return jdbc.query(
        SELECT_POSTAGEM + " WHERE p.id_postagem = ?",
        MAPPER,
        postagemId).stream().findFirst();
  }

  @Override
  public long contarDoCondominio(Integer condominioId) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM tb_postagens WHERE condominio_id = ?",
        Long.class,
        condominioId);
  }

  @Override
  public List<Postagem> listarDoCondominio(
      Integer condominioId,
      long deslocamento,
      int limite) {
    return jdbc.query(
        SELECT_POSTAGEM
            + " WHERE p.condominio_id = ?"
            + " ORDER BY p.data_postagem DESC, p.id_postagem DESC"
            + " LIMIT ? OFFSET ?",
        MAPPER,
        condominioId,
        limite,
        deslocamento);
  }

  @Override
  public long contarDoUsuario(Integer usuarioId) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM tb_postagens WHERE usuario_id = ?",
        Long.class,
        usuarioId);
  }

  @Override
  public List<Postagem> listarDoUsuario(
      Integer usuarioId,
      long deslocamento,
      int limite) {
    return jdbc.query(
        SELECT_POSTAGEM
            + " WHERE p.usuario_id = ?"
            + " ORDER BY p.data_postagem DESC, p.id_postagem DESC"
            + " LIMIT ? OFFSET ?",
        MAPPER,
        usuarioId,
        limite,
        deslocamento);
  }
}

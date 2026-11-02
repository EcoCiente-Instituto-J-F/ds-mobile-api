package br.com.ecociente.mobile.dataprovider.gateway.educacao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.educacao.AlternativaCorrecaoQuiz;
import br.com.ecociente.mobile.core.domain.educacao.ContextoCondominialQuiz;
import br.com.ecociente.mobile.core.domain.educacao.ItemRevisaoQuiz;
import br.com.ecociente.mobile.core.domain.educacao.RegistroTentativaQuiz;
import br.com.ecociente.mobile.core.domain.educacao.RespostaQuizRegistrada;
import br.com.ecociente.mobile.core.domain.educacao.ResumoTentativaQuiz;
import br.com.ecociente.mobile.core.domain.educacao.TentativaQuizEstado;
import br.com.ecociente.mobile.core.exception.educacao.FluxoQuizException;
import br.com.ecociente.mobile.core.gateway.educacao.TentativaQuizGateway;

@Component
public class TentativaQuizGatewayImpl implements TentativaQuizGateway {

  private final JdbcTemplate jdbc;

  public TentativaQuizGatewayImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<String> buscarTipoUsuarioAtivo(
      Integer usuarioId) {
    return jdbc.query("""
        SELECT t.nome_tipo
        FROM tb_usuarios u
        JOIN tb_lkp_tipos_usuarios t
          ON t.id_tipo_usuario = u.tipo_usuario_id
        WHERE u.id_usuario = ?
          AND u.ativo = TRUE
        """,
        (rs, rowNum) -> rs.getString("nome_tipo"),
        usuarioId).stream().findFirst();
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public RegistroTentativaQuiz criar(
      Integer usuarioId,
      Integer quizId) {
    return jdbc.queryForObject("""
        INSERT INTO tb_tentativas_quiz (
            usuario_id,
            quiz_id
        )
        VALUES (?, ?)
        RETURNING id_tentativa, iniciado_em
        """,
        (rs, rowNum) -> new RegistroTentativaQuiz(
            rs.getInt("id_tentativa"),
            rs.getObject("iniciado_em", OffsetDateTime.class)),
        usuarioId,
        quizId);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<TentativaQuizEstado> buscarComBloqueio(
      Integer tentativaId) {
    return jdbc.query("""
        SELECT id_tentativa,
               usuario_id,
               quiz_id,
               nota,
               aprovado,
               concluido_em
        FROM tb_tentativas_quiz
        WHERE id_tentativa = ?
        FOR UPDATE
        """,
        (rs, rowNum) -> mapearTentativa(rs),
        tentativaId).stream().findFirst();
  }

  @Override
  public List<AlternativaCorrecaoQuiz> buscarGabarito(
      Integer quizId) {
    return jdbc.query("""
        SELECT p.id_pergunta,
               a.id_alternativa,
               COALESCE(a.correta, FALSE) AS correta
        FROM tb_perguntas_quiz p
        LEFT JOIN tb_alternativas_quiz a
          ON a.pergunta_id = p.id_pergunta
        WHERE p.quiz_id = ?
        ORDER BY p.ordem, p.id_pergunta, a.id_alternativa
        """,
        (rs, rowNum) -> new AlternativaCorrecaoQuiz(
            rs.getInt("id_pergunta"),
            rs.getObject("id_alternativa", Integer.class),
            rs.getBoolean("correta")),
        quizId);
  }

  @Override
  public List<RespostaQuizRegistrada> buscarRespostas(
      Integer tentativaId) {
    return jdbc.query("""
        SELECT pergunta_id,
               alternativa_escolhida_id,
               correta
        FROM tb_rel_respostas_tentativas_quiz
        WHERE tentativa_id = ?
        ORDER BY pergunta_id
        """,
        (rs, rowNum) -> new RespostaQuizRegistrada(
            rs.getInt("pergunta_id"),
            rs.getInt("alternativa_escolhida_id"),
            rs.getBoolean("correta")),
        tentativaId);
  }

  @Override
  public List<ContextoCondominialQuiz> buscarContextosAprovadosDoMorador(
      Integer usuarioId) {
    return jdbc.query("""
        SELECT DISTINCT
               m.condominio_id,
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
          AND c.ativo = TRUE
          AND r.aprovado = TRUE
          AND r.data_saida IS NULL
        """,
        (rs, rowNum) -> {
          Integer condominioId = rs.getInt("condominio_id");
          Integer torreId = rs.getObject(
              "torre_id", Integer.class);
          Integer condominioTorreId = rs.getObject(
              "condominio_torre_id", Integer.class);

          if (torreId != null
              && !condominioId.equals(condominioTorreId)) {
            throw new FluxoQuizException(
                FluxoQuizException.Tipo.CONFLITO,
                "TORRE_CONDOMINIO_INCONSISTENTE",
                "A torre do morador não pertence ao condomínio");
          }

          return new ContextoCondominialQuiz(
              condominioId,
              torreId);
        },
        usuarioId);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void salvarRespostas(
      Integer tentativaId,
      List<RespostaQuizRegistrada> respostas) {
    jdbc.batchUpdate("""
        INSERT INTO tb_rel_respostas_tentativas_quiz (
            tentativa_id,
            pergunta_id,
            alternativa_escolhida_id,
            correta
        )
        VALUES (?, ?, ?, ?)
        """,
        respostas,
        100,
        (statement, resposta) -> {
          statement.setInt(1, tentativaId);
          statement.setInt(2, resposta.perguntaId());
          statement.setInt(3, resposta.alternativaId());
          statement.setBoolean(4, resposta.correta());
        });
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public TentativaQuizEstado concluir(
      Integer tentativaId,
      BigDecimal nota,
      boolean aprovado,
      Integer condominioId,
      Integer torreId) {
    return jdbc.queryForObject("""
        UPDATE tb_tentativas_quiz
        SET nota = ?,
            aprovado = ?,
            condominio_id = ?,
            torre_id = ?,
            concluido_em = CURRENT_TIMESTAMP
        WHERE id_tentativa = ?
          AND concluido_em IS NULL
        RETURNING id_tentativa,
                  usuario_id,
                  quiz_id,
                  nota,
                  aprovado,
                  concluido_em
        """,
        (rs, rowNum) -> mapearTentativa(rs),
        nota,
        aprovado,
        new org.springframework.jdbc.core.SqlParameterValue(
            Types.INTEGER,
            condominioId),
        new org.springframework.jdbc.core.SqlParameterValue(
            Types.INTEGER,
            torreId),
        tentativaId);
  }

  @Override
  public Optional<TentativaQuizEstado> buscarPorId(
      Integer tentativaId) {
    return jdbc.query("""
        SELECT id_tentativa,
               usuario_id,
               quiz_id,
               nota,
               aprovado,
               concluido_em
        FROM tb_tentativas_quiz
        WHERE id_tentativa = ?
        """,
        (rs, rowNum) -> mapearTentativa(rs),
        tentativaId).stream().findFirst();
  }

  @Override
  public List<ItemRevisaoQuiz> buscarItensRevisao(
      Integer tentativaId) {
    return jdbc.query("""
        SELECT p.id_pergunta,
               p.enunciado,
               p.ordem,
               p.explicacao,
               escolhida.id_alternativa AS escolhida_id,
               escolhida.texto_alternativa AS escolhida_texto,
               r.correta AS acertou_na_tentativa,
               gabarito.correta_id,
               gabarito.correta_texto,
               gabarito.quantidade_corretas
        FROM tb_rel_respostas_tentativas_quiz r
        JOIN tb_tentativas_quiz t
          ON t.id_tentativa = r.tentativa_id
        JOIN tb_perguntas_quiz p
          ON p.id_pergunta = r.pergunta_id
         AND p.quiz_id = t.quiz_id
        JOIN tb_alternativas_quiz escolhida
          ON escolhida.id_alternativa = r.alternativa_escolhida_id
         AND escolhida.pergunta_id = p.id_pergunta
        CROSS JOIN LATERAL (
            SELECT COUNT(*) AS quantidade_corretas,
                   MAX(a.id_alternativa) AS correta_id,
                   MAX(a.texto_alternativa) AS correta_texto
            FROM tb_alternativas_quiz a
            WHERE a.pergunta_id = p.id_pergunta
              AND a.correta = TRUE
        ) gabarito
        WHERE r.tentativa_id = ?
        ORDER BY p.ordem, p.id_pergunta
        """,
        (rs, rowNum) -> {
          if (rs.getLong("quantidade_corretas") != 1) {
            throw new FluxoQuizException(
                FluxoQuizException.Tipo.CONFLITO,
                "GABARITO_ATUAL_INVALIDO",
                "O gabarito atual não permite consultar a revisão");
          }

          return new ItemRevisaoQuiz(
              rs.getInt("id_pergunta"),
              rs.getString("enunciado"),
              rs.getInt("ordem"),
              new ItemRevisaoQuiz.Alternativa(
                  rs.getInt("escolhida_id"),
                  rs.getString("escolhida_texto")),
              new ItemRevisaoQuiz.Alternativa(
                  rs.getInt("correta_id"),
                  rs.getString("correta_texto")),
              rs.getBoolean("acertou_na_tentativa"),
              rs.getString("explicacao"));
        },
        tentativaId);
  }

  private TentativaQuizEstado mapearTentativa(ResultSet rs)
      throws SQLException {

    return new TentativaQuizEstado(
        rs.getInt("id_tentativa"),
        rs.getInt("usuario_id"),
        rs.getInt("quiz_id"),
        rs.getBigDecimal("nota"),
        rs.getBoolean("aprovado"),
        rs.getObject("concluido_em", OffsetDateTime.class));
  }

  @Override
  public List<ResumoTentativaQuiz> listarConcluidas(
      Integer usuarioId,
      int limite) {
    return jdbc.query("""
        SELECT t.id_tentativa,
               t.quiz_id,
               q.titulo_quiz,
               t.nota,
               t.aprovado,
               t.concluido_em
        FROM tb_tentativas_quiz t
        JOIN tb_quizzes q
          ON q.id_quiz = t.quiz_id
        WHERE t.usuario_id = ?
          AND t.concluido_em IS NOT NULL
          AND t.nota IS NOT NULL
        ORDER BY t.concluido_em DESC, t.id_tentativa DESC
        LIMIT ?
        """,
        (rs, rowNum) -> new ResumoTentativaQuiz(
            rs.getInt("id_tentativa"),
            rs.getInt("quiz_id"),
            rs.getString("titulo_quiz"),
            rs.getBigDecimal("nota"),
            rs.getBoolean("aprovado"),
            rs.getObject("concluido_em", OffsetDateTime.class)),
        usuarioId,
        limite);
  }

  @Override
  public List<Integer> listarQuizzesConcluidos(
      Integer usuarioId) {
    return jdbc.queryForList("""
        SELECT DISTINCT quiz_id
        FROM tb_tentativas_quiz
        WHERE usuario_id = ?
          AND concluido_em IS NOT NULL
          AND nota IS NOT NULL
        ORDER BY quiz_id
        """,
        Integer.class,
        usuarioId);
  }
}

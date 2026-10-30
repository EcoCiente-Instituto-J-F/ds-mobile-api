package br.com.ecociente.mobile.dataprovider.gateway.pontuacao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.gateway.pontuacao.ConfiancaPostagemGateway;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ConfiancaPostagemGatewayImpl
    implements ConfiancaPostagemGateway {

  private final JdbcTemplate jdbc;

  @Override
  public void atualizarEnvolvidos(Integer postagemId) {
    var envolvidos = jdbc.query("""
        SELECT DISTINCT p.condominio_id, e.usuario_id
        FROM tb_postagens p
        CROSS JOIN LATERAL (
            SELECT p.usuario_id AS usuario_id
            UNION
            SELECT v.usuario_id
            FROM tb_rel_votos_postagens v
            WHERE v.postagem_id = p.id_postagem
        ) e
        JOIN tb_rel_usuarios_condominios r
          ON r.usuario_id = e.usuario_id
         AND r.condominio_id = p.condominio_id
        WHERE p.id_postagem = ?
        ORDER BY e.usuario_id
        """,
        (rs, indice) -> new Envolvido(
            rs.getInt("usuario_id"),
            rs.getInt("condominio_id")),
        postagemId);

    for (var envolvido : envolvidos) {
      jdbc.update(
          "CALL sp_atualizar_trust_score(?, ?)",
          envolvido.usuarioId(),
          envolvido.condominioId());
    }
  }

  private record Envolvido(
      Integer usuarioId,
      Integer condominioId) {
  }
}
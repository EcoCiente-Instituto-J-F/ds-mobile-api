
package br.com.ecociente.mobile.dataprovider.gateway.pontuacao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.gateway.pontuacao.VotoPostagemGateway;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PontuacaoVotoPostagemGatewayImpl
    implements VotoPostagemGateway {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void registrar(
      Integer postagemId,
      Integer usuarioId,
      String tipoVoto,
      Integer motivoDenunciaId,
      String comentario) {

    jdbcTemplate.update(
        "CALL sp_processar_voto_postagem(?, ?, ?, ?, ?)",
        postagemId,
        usuarioId,
        tipoVoto,
        motivoDenunciaId,
        comentario);
  }
}

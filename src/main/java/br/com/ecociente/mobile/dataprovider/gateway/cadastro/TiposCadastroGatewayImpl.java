package br.com.ecociente.mobile.dataprovider.gateway.cadastro;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.exception.cadastro.BusinessException;
import br.com.ecociente.mobile.core.gateway.cadastro.TiposCadastroGateway;

@Component
public class TiposCadastroGatewayImpl
    implements TiposCadastroGateway {

  private final JdbcTemplate jdbc;

  public TiposCadastroGatewayImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Integer usuarioComum() {
    return buscarTipo(
        List.of("usuario comum", "comum"));
  }

  @Override
  public Integer cooperativa() {
    return buscarTipo(
        List.of("cooperativa"));
  }

  @Override
  public Integer morador(Integer tipoCondominioId) {
    var tipos = jdbc.query(
        """
            SELECT nome_tipo
            FROM tb_lkp_tipos_condominios
            WHERE id_tipo_condominio = ?
            """,
        (rs, linha) -> rs.getString("nome_tipo"),
        tipoCondominioId);

    if (tipos.isEmpty()) {
      throw configuracao(
          "Tipo de condomínio não encontrado");
    }

    String tipoCondominio = normalizar(
        tipos.get(0));

    if (tipoCondominio.contains("residencial")) {
      return buscarTipo(
          List.of(
              "morador residencial",
              "morador",
              "moradora"));
    }

    return buscarTipo(
        List.of(
            "usuario comercial",
            "morador comercial",
            "morador",
            "moradora"));
  }

  @Override
  public Integer nivelInicial() {
    var niveis = jdbc.query(
        """
            SELECT id_nivel_confianca, nome_nivel
            FROM tb_lkp_niveis_confianca
            """,
        (rs, linha) -> new Opcao(
            rs.getInt("id_nivel_confianca"),
            rs.getString("nome_nivel")));

    var encontrados = niveis.stream()
        .filter(opcao -> normalizar(opcao.nome())
            .equals("morador comum"))
        .toList();

    if (encontrados.size() != 1) {
      throw configuracao(
          "O catálogo precisa identificar um único nível morador_comum");
    }

    return encontrados.get(0).id();
  }

  private Integer buscarTipo(
      List<String> prioridades) {
    var tipos = jdbc.query(
        """
            SELECT id_tipo_usuario, nome_tipo
            FROM tb_lkp_tipos_usuarios
            """,
        (rs, linha) -> new Opcao(
            rs.getInt("id_tipo_usuario"),
            rs.getString("nome_tipo")));

    for (String nome : prioridades) {
      var encontrados = tipos.stream()
          .filter(opcao -> normalizar(opcao.nome()).equals(nome))
          .toList();

      if (encontrados.size() > 1) {
        throw configuracao(
            "Há mais de um tipo correspondente a: " + nome);
      }

      if (encontrados.size() == 1) {
        return encontrados.get(0).id();
      }
    }

    throw configuracao(
        "Tipo de usuário não encontrado: "
            + String.join(", ", prioridades));
  }

  private String normalizar(String valor) {
    return Normalizer
        .normalize(
            valor == null ? "" : valor,
            Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replace('_', ' ')
        .replace('-', ' ')
        .trim()
        .replaceAll("\\s+", " ")
        .toLowerCase(Locale.ROOT);
  }

  private BusinessException configuracao(
      String mensagem) {
    return new BusinessException(
        "CATALOG_CONFIGURATION_ERROR",
        mensagem);
  }

  private record Opcao(
      Integer id,
      String nome) {
  }
}
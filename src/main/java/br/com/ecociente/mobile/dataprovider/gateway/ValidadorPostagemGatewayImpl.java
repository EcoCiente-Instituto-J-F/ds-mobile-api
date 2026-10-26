package br.com.ecociente.mobile.dataprovider.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.ecociente.mobile.core.domain.ResultadoVoto;
import br.com.ecociente.mobile.core.exception.PostagemException;
import br.com.ecociente.mobile.core.exception.PostagemException.Tipo;
import br.com.ecociente.mobile.core.gateway.ValidadorPostagemGateway;

@Component
public class ValidadorPostagemGatewayImpl implements ValidadorPostagemGateway {

  private static final Logger log = LoggerFactory.getLogger(ValidadorPostagemGatewayImpl.class);

  private static final String MENSAGEM_INDISPONIVEL = "Não foi possível registrar o voto agora. Tente novamente.";

  private final RestClient client;
  private final String chaveApi;

  public ValidadorPostagemGatewayImpl(
      RestClient.Builder builder,
      @Value("${app.integracoes.validador.base-url:}") String baseUrl,
      @Value("${app.integracoes.validador.chave-api:}") String chaveApi) {
    this.chaveApi = chaveApi.trim();

    if (baseUrl.isBlank()) {
      this.client = null;
      return;
    }

    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(2000);
    factory.setReadTimeout(5000);

    this.client = builder.clone()
        .baseUrl(baseUrl.trim())
        .requestFactory(factory)
        .build();
  }

  @Override
  public ResultadoVoto votar(
      Integer postagemId,
      Integer usuarioId,
      String tipo,
      Integer motivoDenunciaId,
      String comentario) {
    if (client == null || chaveApi.isBlank()) {
      log.warn("Integração do validador não configurada postagemId={}", postagemId);
      throw indisponivel();
    }

    VotoResponse resposta;

    try {
      resposta = client.post()
          .uri("/v1/postagens/{postagemId}/votos", postagemId)
          .header("X-Api-Key", chaveApi)
          .body(new VotoRequest(usuarioId, tipo, motivoDenunciaId, comentario))
          .retrieve()
          .body(VotoResponse.class);

    } catch (RestClientResponseException exception) {
      int status = exception.getStatusCode().value();

      if (status == 401) {
        log.error("Validador recusou a chave de API postagemId={} statusHttp={}", postagemId, status);
      } else {
        log.warn("Voto recusado pelo validador postagemId={} statusHttp={}", postagemId, status);
      }

      throw traduzir(status, exception);

    } catch (RestClientException exception) {
      log.warn(
          "Validador indisponível ou resposta ilegível postagemId={} causa={}",
          postagemId,
          exception.getClass().getSimpleName());
      throw indisponivel();
    }

    if (resposta == null || resposta.saldoConfianca() == null || resposta.pontuacaoAtiva() == null) {
      log.warn("Resposta vazia ou incompleta do validador postagemId={}", postagemId);
      throw indisponivel();
    }

    return new ResultadoVoto(resposta.saldoConfianca(), resposta.pontuacaoAtiva());
  }

  private PostagemException traduzir(int status, RestClientResponseException exception) {
    return switch (status) {
      case 404 -> erro(Tipo.NAO_ENCONTRADO, "POSTAGEM_NAO_ENCONTRADA", "Postagem não encontrada", exception);
      case 403 -> erro(Tipo.NAO_PERMITIDO, "VOTO_NAO_PERMITIDO", "Você não pode votar nesta postagem", exception);
      case 409 -> erro(Tipo.CONFLITO, "VOTO_JA_REGISTRADO", "Você já votou nesta postagem", exception);
      case 400, 422 -> traduzirRegra(detalhe(exception, ""));
      default -> indisponivel();
    };
  }

  private PostagemException traduzirRegra(String detail) {
    if (detail.contains("Janela comunitaria da postagem") || detail.contains("nao aceita novos votos")) {
      return new PostagemException(Tipo.CONFLITO, "VOTACAO_ENCERRADA", "A votação desta postagem já foi encerrada");
    }
    if (detail.contains("motivo_denuncia_id e obrigatorio")) {
      return new PostagemException(
          Tipo.DADOS_INVALIDOS, "MOTIVO_DENUNCIA_OBRIGATORIO", "Informe o motivo da denúncia");
    }
    if (detail.contains("motivo_denuncia_id deve ser nulo")) {
      return new PostagemException(
          Tipo.DADOS_INVALIDOS, "MOTIVO_DENUNCIA_INDEVIDO", "Voto de aprovação não leva motivo de denúncia");
    }
    if (detail.contains("nao possui vinculo aprovado")) {
      return new PostagemException(
          Tipo.NAO_PERMITIDO, "SEM_VINCULO_APROVADO", "Você não tem vínculo aprovado com este condomínio");
    }
    return new PostagemException(Tipo.DADOS_INVALIDOS, "VOTO_INVALIDO", "Voto inválido");
  }

  private PostagemException erro(
      Tipo tipo,
      String codigo,
      String mensagemPadrao,
      RestClientResponseException exception) {
    return new PostagemException(tipo, codigo, detalhe(exception, mensagemPadrao));
  }

  private String detalhe(RestClientResponseException exception, String padrao) {
    try {
      var erro = exception.getResponseBodyAs(ErroResponse.class);

      if (erro != null && erro.detail() != null && !erro.detail().isBlank()) {
        return erro.detail();
      }
    } catch (RuntimeException ignorada) {
      // corpo fora do formato esperado: vale a mensagem padrão
    }

    return padrao;
  }

  private PostagemException indisponivel() {
    return new PostagemException(Tipo.INDISPONIVEL, "VALIDADOR_INDISPONIVEL", MENSAGEM_INDISPONIVEL);
  }

  private record VotoRequest(
      @JsonProperty("usuario_id") Integer usuarioId,
      @JsonProperty("tipo") String tipo,
      @JsonProperty("motivo_denuncia_id") Integer motivoDenunciaId,
      @JsonProperty("comentario") String comentario) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record VotoResponse(
      @JsonProperty("saldo_confianca") Integer saldoConfianca,
      @JsonProperty("pontuacao_ativa") Boolean pontuacaoAtiva) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ErroResponse(String detail) {
  }
}

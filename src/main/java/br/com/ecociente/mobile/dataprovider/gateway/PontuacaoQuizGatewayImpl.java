package br.com.ecociente.mobile.dataprovider.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.com.ecociente.mobile.core.domain.ResultadoPontuacaoQuiz;
import br.com.ecociente.mobile.core.gateway.PontuacaoQuizGateway;

@Component
public class PontuacaoQuizGatewayImpl implements PontuacaoQuizGateway {

  private static final Logger log = LoggerFactory.getLogger(PontuacaoQuizGatewayImpl.class);

  private final RestClient client;
  private final String tokenServico;

  public PontuacaoQuizGatewayImpl(
      RestClient.Builder builder,
      @Value("${app.integracoes.pontuacao.base-url:}") String baseUrl,
      @Value("${app.integracoes.pontuacao.token-servico:}") String tokenServico) {
    this.tokenServico = tokenServico.trim();

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
  public ResultadoPontuacaoQuiz consolidar(
      Integer tentativaId,
      Integer usuarioId) {
    if (client == null || tokenServico.isBlank()) {
      log.warn(
          "Integração de pontuação não configurada tentativaId={}",
          tentativaId);

      return ResultadoPontuacaoQuiz.pendente();
    }

    try {
      PontuacaoResponse resposta = client.post()
          .uri(
              "/api/v1/pontuacoes/quizzes/tentativas/{id}/consolidar",
              tentativaId)
          .header(
              HttpHeaders.AUTHORIZATION,
              "Bearer " + tokenServico)
          .header(
              "Idempotency-Key",
              "quiz-tentativa-" + tentativaId)
          .retrieve()
          .body(PontuacaoResponse.class);

      if (!respostaValida(resposta, tentativaId, usuarioId)) {
        log.error(
            "Resposta inconsistente da pontuação tentativaId={}",
            tentativaId);

        return ResultadoPontuacaoQuiz.falha();
      }

      return new ResultadoPontuacaoQuiz(
          ResultadoPontuacaoQuiz.Status.CONFIRMADO,
          resposta.pontosConcedidos(),
          resposta.movimentacaoId(),
          resposta.redis() == null
              ? null
              : resposta.redis().sincronizado());

    } catch (RestClientResponseException exception) {
      int status = exception.getStatusCode().value();

      log.warn(
          "Consolidação recusada tentativaId={} statusHttp={}",
          tentativaId,
          status);

      if (status >= 500 || status == 408 || status == 429) {
        return ResultadoPontuacaoQuiz.pendente();
      }

      return ResultadoPontuacaoQuiz.falha();

    } catch (ResourceAccessException exception) {
      log.warn(
          "Pontuação indisponível tentativaId={}",
          tentativaId);

      return ResultadoPontuacaoQuiz.pendente();

    } catch (RestClientException exception) {
      log.error(
          "Não foi possível interpretar a resposta da pontuação tentativaId={}",
          tentativaId);

      return ResultadoPontuacaoQuiz.falha();
    }
  }

  private boolean respostaValida(
      PontuacaoResponse resposta,
      Integer tentativaId,
      Integer usuarioId) {
    return resposta != null
        && tentativaId.equals(resposta.tentativaId())
        && usuarioId.equals(resposta.usuarioId())
        && "QUIZ".equals(resposta.origemTipo())
        && "CREDITO".equals(resposta.tipoMovimentacao())
        && resposta.movimentacaoId() != null
        && resposta.movimentacaoId() > 0
        && resposta.pontosConcedidos() != null
        && resposta.pontosConcedidos() > 0;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PontuacaoResponse(
      String origemTipo,
      Integer tentativaId,
      Integer usuarioId,
      Integer pontosConcedidos,
      String tipoMovimentacao,
      Long movimentacaoId,
      RedisResponse redis) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RedisResponse(
      boolean sincronizado,
      String status) {
  }
}
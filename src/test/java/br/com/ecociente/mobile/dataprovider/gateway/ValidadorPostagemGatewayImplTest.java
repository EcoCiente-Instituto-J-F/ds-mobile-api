package br.com.ecociente.mobile.dataprovider.gateway;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

import br.com.ecociente.mobile.core.exception.PostagemException;
import br.com.ecociente.mobile.core.exception.PostagemException.Tipo;

class ValidadorPostagemGatewayImplTest {

  private static final String PADRAO = "Não foi possível registrar o voto agora. Tente novamente.";

  private HttpServer servidor;
  private final AtomicInteger chamadas = new AtomicInteger();
  private final AtomicReference<String> metodo = new AtomicReference<>();
  private final AtomicReference<String> caminho = new AtomicReference<>();
  private final AtomicReference<String> chave = new AtomicReference<>();
  private final AtomicReference<String> corpo = new AtomicReference<>();

  private volatile int statusResposta;
  private volatile String corpoResposta;

  @BeforeEach
  void subir() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext("/", troca -> {
      chamadas.incrementAndGet();
      metodo.set(troca.getRequestMethod());
      caminho.set(troca.getRequestURI().getPath());
      chave.set(troca.getRequestHeaders().getFirst("X-Api-Key"));
      corpo.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

      byte[] bytes = corpoResposta.getBytes(StandardCharsets.UTF_8);
      troca.getResponseHeaders().add("Content-Type", "application/json");
      troca.sendResponseHeaders(statusResposta, bytes.length);
      troca.getResponseBody().write(bytes);
      troca.close();
    });
    servidor.start();
  }

  @AfterEach
  void parar() {
    servidor.stop(0);
  }

  private String url() {
    return "http://127.0.0.1:" + servidor.getAddress().getPort();
  }

  private ValidadorPostagemGatewayImpl gateway() {
    return new ValidadorPostagemGatewayImpl(RestClient.builder(), url(), "segredo-teste");
  }

  private PostagemException votarComFalha() {
    return assertThrows(
        PostagemException.class,
        () -> gateway().votar(42, 7, "denunciar", 2, "foto errada"));
  }

  @Test
  void deveEnviarVotoEmSnakeCaseEMapearResultado() {
    statusResposta = 200;
    corpoResposta = "{\"saldo_confianca\":3,\"pontuacao_ativa\":true,\"extra\":1}";

    var resultado = gateway().votar(42, 7, "denunciar", 2, "foto errada");

    assertEquals(3, resultado.saldoConfianca());
    assertTrue(resultado.pontuacaoAtiva());
    assertEquals("POST", metodo.get());
    assertEquals("/v1/postagens/42/votos", caminho.get());
    assertEquals("segredo-teste", chave.get());
    assertTrue(corpo.get().contains("\"usuario_id\":7"), corpo.get());
    assertTrue(corpo.get().contains("\"tipo\":\"denunciar\""), corpo.get());
    assertTrue(corpo.get().contains("\"motivo_denuncia_id\":2"), corpo.get());
    assertTrue(corpo.get().contains("\"comentario\":\"foto errada\""), corpo.get());
  }

  @Test
  void deveMapearErrosDoValidadorUsandoODetail404403409() {
    verificar(404, Tipo.NAO_ENCONTRADO, "POSTAGEM_NAO_ENCONTRADA");
    verificar(403, Tipo.NAO_PERMITIDO, "VOTO_NAO_PERMITIDO");
    verificar(409, Tipo.CONFLITO, "VOTO_JA_REGISTRADO");
  }

  private void verificar(int status, Tipo tipo, String codigo) {
    statusResposta = status;
    corpoResposta = "{\"detail\":\"mensagem " + status + "\"}";

    var erro = votarComFalha();

    assertEquals(tipo, erro.getTipo(), "status " + status);
    assertEquals(codigo, erro.getCodigo(), "status " + status);
    assertEquals("mensagem " + status, erro.getMessage(), "status " + status);
  }

  private void verificarRegra(int status, String detail, Tipo tipo, String codigo, String mensagem) {
    statusResposta = status;
    corpoResposta = detail == null ? "{}" : "{\"detail\":\"" + detail + "\"}";

    var erro = votarComFalha();

    assertEquals(tipo, erro.getTipo(), detail);
    assertEquals(codigo, erro.getCodigo(), detail);
    assertEquals(mensagem, erro.getMessage(), detail);
  }

  @Test
  void deveTraduzirRegrasDoProcedureParaMensagensLegiveis() {
    for (int status : new int[] { 400, 422 }) {
      verificarRegra(status, "Janela comunitaria da postagem 42 encerrou em 2026-10-24 13:22:01+00",
          Tipo.CONFLITO, "VOTACAO_ENCERRADA", "A votação desta postagem já foi encerrada");
      verificarRegra(status, "Postagem 42 nao aceita novos votos",
          Tipo.CONFLITO, "VOTACAO_ENCERRADA", "A votação desta postagem já foi encerrada");
      verificarRegra(status, "erro: motivo_denuncia_id e obrigatorio para denunciar",
          Tipo.DADOS_INVALIDOS, "MOTIVO_DENUNCIA_OBRIGATORIO", "Informe o motivo da denúncia");
      verificarRegra(status, "motivo_denuncia_id deve ser nulo para aprovar",
          Tipo.DADOS_INVALIDOS, "MOTIVO_DENUNCIA_INDEVIDO", "Voto de aprovação não leva motivo de denúncia");
      verificarRegra(status, "Usuario 7 nao possui vinculo aprovado e ativo com o condominio da postagem",
          Tipo.NAO_PERMITIDO, "SEM_VINCULO_APROVADO", "Você não tem vínculo aprovado com este condomínio");
      verificarRegra(status, "texto cru desconhecido sp_x",
          Tipo.DADOS_INVALIDOS, "VOTO_INVALIDO", "Voto inválido");
      verificarRegra(status, null, Tipo.DADOS_INVALIDOS, "VOTO_INVALIDO", "Voto inválido");
    }
  }

  @Test
  void deveFicarIndisponivelQuandoRespostaVierIncompleta() {
    statusResposta = 200;
    corpoResposta = "{\"saldo_confianca\": 3}";

    var erro = votarComFalha();

    assertEquals(Tipo.INDISPONIVEL, erro.getTipo());
    assertEquals(PADRAO, erro.getMessage());
  }

  @Test
  void deveUsarMensagemPadraoQuandoNaoHaDetail() {
    statusResposta = 409;
    corpoResposta = "{}";

    assertEquals("Você já votou nesta postagem", votarComFalha().getMessage());
  }

  @Test
  void naoDeveRepassarDetailDe401NemDe5xxNemDeOutros4xx() {
    for (int status : new int[] { 401, 500, 503, 418 }) {
      statusResposta = status;
      corpoResposta = "{\"detail\":\"segredo interno\"}";

      var erro = votarComFalha();

      assertEquals(Tipo.INDISPONIVEL, erro.getTipo(), "status " + status);
      assertEquals("VALIDADOR_INDISPONIVEL", erro.getCodigo(), "status " + status);
      assertEquals(PADRAO, erro.getMessage(), "status " + status);
    }
  }

  @Test
  void deveFicarIndisponivelQuandoRespostaForIlegivel() {
    statusResposta = 200;
    corpoResposta = "isto nao e json";

    var erro = votarComFalha();

    assertEquals(Tipo.INDISPONIVEL, erro.getTipo());
    assertEquals(PADRAO, erro.getMessage());
  }

  @Test
  void deveFicarIndisponivelQuandoNaoHaConexao() {
    String urlFechada = url();
    servidor.stop(0);

    var gateway = new ValidadorPostagemGatewayImpl(RestClient.builder(), urlFechada, "segredo-teste");

    var erro = assertThrows(
        PostagemException.class,
        () -> gateway.votar(42, 7, "aprovar", null, null));

    assertEquals(Tipo.INDISPONIVEL, erro.getTipo());
    assertEquals(PADRAO, erro.getMessage());
  }

  @Test
  void deveFicarIndisponivelSemChamarHttpQuandoNaoConfigurado() {
    var semUrl = new ValidadorPostagemGatewayImpl(RestClient.builder(), "", "segredo-teste");
    var semChave = new ValidadorPostagemGatewayImpl(RestClient.builder(), url(), " ");

    for (var gateway : new ValidadorPostagemGatewayImpl[] { semUrl, semChave }) {
      var erro = assertThrows(
          PostagemException.class,
          () -> gateway.votar(42, 7, "aprovar", null, null));

      assertEquals(Tipo.INDISPONIVEL, erro.getTipo());
      assertEquals("VALIDADOR_INDISPONIVEL", erro.getCodigo());
    }
    assertEquals(0, chamadas.get());
  }
}

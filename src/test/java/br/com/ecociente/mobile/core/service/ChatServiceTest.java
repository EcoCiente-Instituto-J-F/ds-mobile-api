package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.ecociente.mobile.core.domain.Conversa;
import br.com.ecociente.mobile.core.domain.MensagemConversa;
import br.com.ecociente.mobile.core.exception.ChatException;
import br.com.ecociente.mobile.core.gateway.ChatGateway;

class ChatServiceTest {

  private ChatGateway gateway;
  private ChatService service;

  @BeforeEach
  void configurar() {
    gateway = mock(ChatGateway.class);
    service = new ChatService(gateway);
  }

  @Test
  void deveCriarOuRecuperarConversaDoParticipante() {
    var conversa = conversa(true);

    when(gateway.buscarLado(10, 2, 3))
        .thenReturn(Optional.of("COOPERATIVA"));

    when(gateway.criarOuObter(2, 3))
        .thenReturn(1);

    when(gateway.buscar(1, 10, false))
        .thenReturn(Optional.of(conversa));

    assertEquals(
        conversa,
        service.criar(10, 2, 3));

    verify(gateway).criarOuObter(2, 3);
  }

  @Test
  void naoDeveCriarConversaSemRepresentarAsPartes() {
    when(gateway.buscarLado(10, 2, 3))
        .thenReturn(Optional.empty());

    var erro = assertThrows(
        ChatException.class,
        () -> service.criar(10, 2, 3));

    assertEquals(403, erro.getStatus());

    verify(gateway, never())
        .criarOuObter(anyInt(), anyInt());
  }

  @Test
  void deveBloquearUsuarioQueRepresentaAsDuasPartes() {
    when(gateway.buscarLado(10, 2, 3))
        .thenReturn(Optional.of("AMBIGUO"));

    var erro = assertThrows(
        ChatException.class,
        () -> service.criar(10, 2, 3));

    assertEquals(403, erro.getStatus());

    verify(gateway, never())
        .criarOuObter(anyInt(), anyInt());
  }

  @Test
  void deveEnviarComLadoDoBancoETextoNormalizado() {
    autorizar(true);

    var mensagem = new MensagemConversa(
        100L,
        1,
        10,
        "COOPERATIVA",
        "Olá",
        OffsetDateTime.now(),
        null);

    when(gateway.enviar(1, 10, "COOPERATIVA", "Olá"))
        .thenReturn(mensagem);

    var resultado = service.enviar(
        10,
        1,
        "  Olá  ");

    assertEquals(mensagem, resultado);

    verify(gateway).enviar(
        1,
        10,
        "COOPERATIVA",
        "Olá");
  }

  @Test
  void naoDeveEnviarEmConversaArquivada() {
    autorizar(false);

    var erro = assertThrows(
        ChatException.class,
        () -> service.enviar(10, 1, "Olá"));

    assertEquals(409, erro.getStatus());

    verify(gateway, never()).enviar(
        anyInt(),
        anyInt(),
        anyString(),
        anyString());
  }

  @Test
  void deveRejeitarConteudoInvalidoAntesDeAcessarBanco() {
    String[] invalidos = {
        null,
        "",
        " \n\t ",
        "x".repeat(2001)
    };

    for (String conteudo : invalidos) {
      var erro = assertThrows(
          ChatException.class,
          () -> service.enviar(10, 1, conteudo));

      assertEquals(400, erro.getStatus());
    }

    verifyNoInteractions(gateway);
  }

  @Test
  void naoDeveLerNemArquivarConversaDeOutroParticipante() {
    when(gateway.buscar(1, 10, true))
        .thenReturn(Optional.of(conversa(true)));

    when(gateway.buscarLado(10, 2, 3))
        .thenReturn(Optional.empty());

    assertThrows(
        ChatException.class,
        () -> service.ler(10, 1, null));

    assertThrows(
        ChatException.class,
        () -> service.arquivar(10, 1, true));

    verify(gateway, never()).marcarLeitura(
        anyInt(),
        anyString(),
        any());

    verify(gateway, never()).definirAtiva(
        anyInt(),
        anyBoolean());
  }

  @Test
  void deveMarcarRecebidasAteMensagemInformada() {
    autorizar(true);

    when(gateway.marcarLeitura(1, "COOPERATIVA", 99L))
        .thenReturn(2);

    assertEquals(
        2,
        service.ler(10, 1, 99L));

    verify(gateway).marcarLeitura(
        1,
        "COOPERATIVA",
        99L);
  }

  @Test
  void devePermitirLeituraDoHistoricoArquivado() {
    autorizar(false);

    service.ler(10, 1, null);

    verify(gateway).marcarLeitura(
        1,
        "COOPERATIVA",
        null);
  }

  @Test
  void deveArquivarEDesarquivarConversa() {
    autorizar(true);

    service.arquivar(10, 1, true);
    service.arquivar(10, 1, false);

    verify(gateway).definirAtiva(1, false);
    verify(gateway).definirAtiva(1, true);
  }

  @Test
  void devePreservarTotalNaPaginaVaziaAlemDoFinal() {
    when(gateway.contarConversas(10, false))
        .thenReturn(3L);

    when(gateway.listarConversas(10, false, 198L, 2))
        .thenReturn(List.of());

    var resultado = service.listar(
        10,
        false,
        99,
        2);

    assertEquals(3L, resultado.total());
    assertEquals(2L, resultado.totalPaginas());
    assertTrue(resultado.itens().isEmpty());

    verify(gateway).listarConversas(
        10,
        false,
        198L,
        2);
  }

  @Test
  void deveInformarConversaInexistente() {
    when(gateway.buscar(999, 10, false))
        .thenReturn(Optional.empty());

    var erro = assertThrows(
        ChatException.class,
        () -> service.mensagens(10, 999, 0, 20));

    assertEquals(404, erro.getStatus());

    verify(gateway, never())
        .contarMensagens(anyInt());
  }

  @ParameterizedTest
  @CsvSource({
      "-1,20",
      "0,0",
      "0,-1",
      "0,101"
  })
  void deveRejeitarPaginacaoInvalida(
      int pagina,
      int tamanho) {
    var erro = assertThrows(
        ChatException.class,
        () -> service.listar(10, false, pagina, tamanho));

    assertEquals(400, erro.getStatus());
    verifyNoInteractions(gateway);
  }

  @Test
  void deveRejeitarUsuarioAusenteEIdsInvalidos() {
    assertThrows(
        ChatException.class,
        () -> service.listar(null, false, 0, 20));

    assertThrows(
        ChatException.class,
        () -> service.criar(10, 0, 3));

    assertThrows(
        ChatException.class,
        () -> service.ler(10, 1, 0L));

    verifyNoInteractions(gateway);
  }

  private void autorizar(boolean ativa) {
    when(gateway.buscar(1, 10, true))
        .thenReturn(Optional.of(conversa(ativa)));

    when(gateway.buscarLado(10, 2, 3))
        .thenReturn(Optional.of("COOPERATIVA"));
  }

  private Conversa conversa(boolean ativa) {
    return new Conversa(
        1,
        2,
        3,
        "Condomínio",
        "Cooperativa",
        OffsetDateTime.now(),
        null,
        ativa,
        0);
  }
}
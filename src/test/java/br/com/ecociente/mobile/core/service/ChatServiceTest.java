package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import br.com.ecociente.mobile.core.domain.Conversa;
import br.com.ecociente.mobile.core.exception.ChatException;
import br.com.ecociente.mobile.core.gateway.ChatGateway;

class ChatServiceTest {
  private final ChatGateway gateway = mock(ChatGateway.class);
  private final ChatService service = new ChatService(gateway);
  private Conversa conversa(boolean ativa) {
    return new Conversa(1, 2, 3, "Condomínio", "Cooperativa", OffsetDateTime.now(), null, ativa, 0);
  }
  private void autorizar(boolean ativa) {
    when(gateway.buscar(1, 10, true)).thenReturn(Optional.of(conversa(ativa)));
    when(gateway.buscarLado(10, 2, 3)).thenReturn(Optional.of("COOPERATIVA"));
  }
  @Test
  void naoDeveCriarConversaSemRepresentarAsPartes() {
    when(gateway.buscarLado(10, 2, 3)).thenReturn(Optional.empty());
    assertEquals(403, assertThrows(ChatException.class, () -> service.criar(10, 2, 3)).getStatus());
    verify(gateway, never()).criarOuObter(anyInt(), anyInt());
  }
  @Test
  void deveBloquearIdentidadeAmbigua() {
    when(gateway.buscarLado(10, 2, 3)).thenReturn(Optional.of("AMBIGUO"));
    assertThrows(ChatException.class, () -> service.criar(10, 2, 3));
    verify(gateway, never()).criarOuObter(anyInt(), anyInt());
  }
  @Test
  void naoDeveEnviarNemAtualizarConversaArquivada() {
    autorizar(false);
    assertEquals(409, assertThrows(ChatException.class, () -> service.enviar(10, 1, "Olá")).getStatus());
    verify(gateway, never()).enviar(anyInt(), anyInt(), anyString(), anyString());
  }
  @Test
  void deveEnviarComLadoDeterminadoPeloBancoEConteudoNormalizado() {
    autorizar(true);
    service.enviar(10, 1, "  Olá  ");
    verify(gateway).enviar(1, 10, "COOPERATIVA", "Olá");
  }
  @Test
  void naoDeveMarcarLeituraNemArquivarConversaAlheia() {
    when(gateway.buscar(1, 10, true)).thenReturn(Optional.of(conversa(true)));
    when(gateway.buscarLado(10, 2, 3)).thenReturn(Optional.empty());
    assertThrows(ChatException.class, () -> service.ler(10, 1, null));
    assertThrows(ChatException.class, () -> service.arquivar(10, 1, true));
    verify(gateway, never()).marcarLeitura(anyInt(), anyString(), any());
    verify(gateway, never()).definirAtiva(anyInt(), anyBoolean());
  }
  @Test
  void devePassarLimiteDeLeituraParaMensagensRecebidas() {
    autorizar(true);
    service.ler(10, 1, 99L);
    verify(gateway).marcarLeitura(1, "COOPERATIVA", 99L);
  }
  @Test
  void deveRecusarMensagemVaziaEGrandeAntesDeEscrever() {
    for (String conteudo : new String[]{null, "", " \n\t ", "x".repeat(2001)}) {
      assertEquals(400, assertThrows(ChatException.class, () -> service.enviar(10, 1, conteudo)).getStatus());
    }
    verifyNoInteractions(gateway);
  }
  @Test
  void deveValidarUsuarioPaginacaoELimiteDeLeitura() {
    assertThrows(ChatException.class, () -> service.listar(null, false, 0, 20));
    assertThrows(ChatException.class, () -> service.listar(10, false, -1, 20));
    assertThrows(ChatException.class, () -> service.listar(10, false, 0, 101));
    assertThrows(ChatException.class, () -> service.ler(10, 1, 0L));
    verifyNoInteractions(gateway);
  }
}

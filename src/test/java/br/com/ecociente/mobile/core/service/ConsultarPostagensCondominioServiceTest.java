package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import br.com.ecociente.mobile.core.domain.ContextoCondominialQuiz;
import br.com.ecociente.mobile.core.domain.Postagem;
import br.com.ecociente.mobile.core.exception.PostagemException;
import br.com.ecociente.mobile.core.exception.PostagemException.Tipo;
import br.com.ecociente.mobile.core.gateway.PostagemGateway;

class ConsultarPostagensCondominioServiceTest {

  private PostagemGateway gateway;
  private ConsultarPostagensCondominioService service;

  @BeforeEach
  void configurar() {
    gateway = mock(PostagemGateway.class);
    service = new ConsultarPostagensCondominioService(gateway);
  }

  private static Postagem postagem(Integer id) {
    return new Postagem(
        id, 10, "Ana", 5, null, 2, "Plástico", "https://x/f.jpg",
        OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now(),
        "em_analise", 0, OffsetDateTime.now().plusHours(24), null);
  }

  @Test
  void deveListarOFeedComPaginacao() {
    when(gateway.buscarVinculoAprovado(10, 5))
        .thenReturn(Optional.of(new ContextoCondominialQuiz(5, null)));
    when(gateway.contarDoCondominio(5)).thenReturn(45L);
    when(gateway.listarDoCondominio(5, 20L, 10)).thenReturn(List.of(postagem(1)));

    var pagina = service.executar(10, 5, 2, 10);

    assertEquals(45L, pagina.total());
    assertEquals(2, pagina.pagina());
    assertEquals(10, pagina.tamanho());
    assertEquals(5L, pagina.totalPaginas());
    assertEquals(1, pagina.postagens().size());
  }

  @Test
  void naoDeveListarOFeedDeOutroCondominio() {
    when(gateway.buscarVinculoAprovado(10, 9)).thenReturn(Optional.empty());

    var erro = assertThrows(
        PostagemException.class,
        () -> service.executar(10, 9, 0, 10));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    verify(gateway, never()).listarDoCondominio(anyInt(), anyLong(), anyInt());
  }

  @Test
  void deveRejeitarPaginacaoEIdsInvalidos() {
    assertEquals(
        "PAGINACAO_INVALIDA",
        assertThrows(PostagemException.class, () -> service.executar(10, 5, -1, 10)).getCodigo());
    assertEquals(
        "PAGINACAO_INVALIDA",
        assertThrows(PostagemException.class, () -> service.executar(10, 5, 0, 101)).getCodigo());
    assertEquals(
        "CONDOMINIO_ID_INVALIDO",
        assertThrows(PostagemException.class, () -> service.executar(10, 0, 0, 10)).getCodigo());
    assertEquals(
        "USUARIO_INVALIDO",
        assertThrows(PostagemException.class, () -> service.executar(null, 5, 0, 10)).getCodigo());
    verifyNoInteractions(gateway);
  }
}

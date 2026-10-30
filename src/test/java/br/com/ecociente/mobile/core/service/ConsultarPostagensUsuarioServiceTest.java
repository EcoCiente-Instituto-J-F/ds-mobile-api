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
import org.springframework.dao.DuplicateKeyException;

import br.com.ecociente.mobile.core.domain.educacao.ContextoCondominialQuiz;
import br.com.ecociente.mobile.core.domain.Postagem;
import br.com.ecociente.mobile.core.exception.PostagemException;
import br.com.ecociente.mobile.core.exception.PostagemException.Tipo;
import br.com.ecociente.mobile.core.gateway.PostagemGateway;

class ConsultarPostagensUsuarioServiceTest {

  private PostagemGateway gateway;
  private ConsultarPostagensUsuarioService service;

  @BeforeEach
  void configurar() {
    gateway = mock(PostagemGateway.class);
    service = new ConsultarPostagensUsuarioService(gateway);
  }

  private static Postagem postagem(Integer id) {
    return new Postagem(
        id, 10, "Ana", 5, null, 2, "Plástico", "https://x/f.jpg",
        OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now(),
        "em_analise", 0, OffsetDateTime.now().plusHours(24), null);
  }

  @Test
  void deveListarAsPostagensDoProprioUsuario() {
    when(gateway.contarDoUsuario(10)).thenReturn(2L);
    when(gateway.listarDoUsuario(10, 0L, 10)).thenReturn(List.of(postagem(1), postagem(2)));

    var pagina = service.executar(10, 0, 10);

    assertEquals(2L, pagina.total());
    assertEquals(1L, pagina.totalPaginas());
    assertEquals(2, pagina.postagens().size());
  }

  @Test
  void semPostagensDevolvePaginaVazia() {
    when(gateway.contarDoUsuario(10)).thenReturn(0L);
    when(gateway.listarDoUsuario(10, 0L, 10)).thenReturn(List.of());

    var pagina = service.executar(10, 0, 10);

    assertEquals(0L, pagina.total());
    assertEquals(0L, pagina.totalPaginas());
    assertTrue(pagina.postagens().isEmpty());
  }

  @ParameterizedTest
  @CsvSource({
      "-1,10",
      "0,0",
      "0,-1",
      "0,101"
  })
  void deveRejeitarPaginacaoInvalida(
      int pagina,
      int tamanho) {
    var erro = assertThrows(
        PostagemException.class,
        () -> service.executar(10, pagina, tamanho));

    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    assertEquals("PAGINACAO_INVALIDA", erro.getCodigo());
    verifyNoInteractions(gateway);
  }

  @Test
  void deveRejeitarUsuarioAusente() {
    var erro = assertThrows(
        PostagemException.class,
        () -> service.executar(null, 0, 10));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    assertEquals("USUARIO_INVALIDO", erro.getCodigo());
    verifyNoInteractions(gateway);
  }
}

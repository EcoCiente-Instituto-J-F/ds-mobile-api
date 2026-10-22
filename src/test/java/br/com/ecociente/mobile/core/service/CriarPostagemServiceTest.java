package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import br.com.ecociente.mobile.core.domain.ContextoCondominialQuiz;
import br.com.ecociente.mobile.core.domain.Postagem;
import br.com.ecociente.mobile.core.exception.PostagemException;
import br.com.ecociente.mobile.core.exception.PostagemException.Tipo;
import br.com.ecociente.mobile.core.gateway.PostagemGateway;

class CriarPostagemServiceTest {

  private static final String URL = "https://res.cloudinary.com/x/foto.jpg";

  private PostagemGateway gateway;
  private CriarPostagemService service;

  @BeforeEach
  void configurar() {
    gateway = mock(PostagemGateway.class);
    service = new CriarPostagemService(gateway);
  }

  private static Postagem postagem(Integer id) {
    return new Postagem(
        id, 10, "Ana", 5, 3, 2, "Plástico", URL,
        OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now(),
        "em_analise", 0, OffsetDateTime.now().plusHours(24), null);
  }

  private void vinculoOk() {
    when(gateway.buscarVinculoAprovado(10, 5))
        .thenReturn(Optional.of(new ContextoCondominialQuiz(5, 3)));
    when(gateway.categoriaExiste(2)).thenReturn(true);
    when(gateway.hashFotoJaUsado("abc")).thenReturn(false);
  }

  private static OffsetDateTime captura() {
    return OffsetDateTime.now().minusMinutes(2);
  }

  @Test
  void deveCriarPostagemComATorreDoMorador() {
    vinculoOk();

    var captura = captura();

    when(gateway.inserir(10, 5, 3, 2, URL, "abc", captura)).thenReturn(77);
    when(gateway.buscarPorId(77)).thenReturn(Optional.of(postagem(77)));

    var criada = service.executar(10, 5, 2, URL, "abc", captura);

    assertEquals(77, criada.id());
    verify(gateway).inserir(10, 5, 3, 2, URL, "abc", captura);
  }

  @Test
  void naoDeveCriarSemVinculoAprovado() {
    when(gateway.buscarVinculoAprovado(10, 5)).thenReturn(Optional.empty());

    var erro = assertThrows(
        PostagemException.class,
        () -> service.executar(10, 5, 2, URL, "abc", captura()));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    assertEquals("SEM_VINCULO_APROVADO", erro.getCodigo());
    verify(gateway, never()).inserir(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void naoDeveCriarComCapturaNoFuturo() {
    var erro = assertThrows(
        PostagemException.class,
        () -> service.executar(10, 5, 2, URL, "abc", OffsetDateTime.now().plusMinutes(5)));

    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    assertEquals("CAPTURA_INVALIDA", erro.getCodigo());
    verifyNoInteractions(gateway);
  }

  @Test
  void naoDeveCriarComUrlQueNaoSejaHttp() {
    var erro = assertThrows(
        PostagemException.class,
        () -> service.executar(10, 5, 2, "ftp://x/foto.jpg", "abc", captura()));

    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    assertEquals("URL_FOTO_INVALIDA", erro.getCodigo());
  }

  @Test
  void deveRejeitarUsuarioEIdsInvalidos() {
    assertEquals(
        "USUARIO_INVALIDO",
        assertThrows(PostagemException.class,
            () -> service.executar(null, 5, 2, URL, "abc", captura())).getCodigo());
    assertEquals(
        "CONDOMINIO_ID_INVALIDO",
        assertThrows(PostagemException.class,
            () -> service.executar(10, 0, 2, URL, "abc", captura())).getCodigo());
    assertEquals(
        "CATEGORIA_ID_INVALIDA",
        assertThrows(PostagemException.class,
            () -> service.executar(10, 5, null, URL, "abc", captura())).getCodigo());
    verifyNoInteractions(gateway);
  }

  @Test
  void deveInformarCategoriaNaoEncontrada() {
    when(gateway.buscarVinculoAprovado(10, 5))
        .thenReturn(Optional.of(new ContextoCondominialQuiz(5, null)));
    when(gateway.categoriaExiste(2)).thenReturn(false);

    var erro = assertThrows(
        PostagemException.class,
        () -> service.executar(10, 5, 2, URL, "abc", captura()));

    assertEquals(Tipo.NAO_ENCONTRADO, erro.getTipo());
  }

  @Test
  void deveRejeitarFotoJaUtilizada() {
    vinculoOk();
    when(gateway.hashFotoJaUsado("abc")).thenReturn(true);

    var erro = assertThrows(
        PostagemException.class,
        () -> service.executar(10, 5, 2, URL, "abc", captura()));

    assertEquals(Tipo.CONFLITO, erro.getTipo());
    assertEquals("FOTO_JA_UTILIZADA", erro.getCodigo());
    verify(gateway, never()).inserir(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void deveTratarCorridaDeHashRepetidoComoConflito() {
    vinculoOk();
    when(gateway.inserir(any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new DuplicateKeyException("violates constraint uq_postagens_hash_foto"));

    var erro = assertThrows(
        PostagemException.class,
        () -> service.executar(10, 5, 2, URL, "abc", captura()));

    assertEquals(Tipo.CONFLITO, erro.getTipo());
  }

  @Test
  void deveTratarRestricaoDeCapturaDoBancoComoDadoInvalido() {
    vinculoOk();
    when(gateway.inserir(any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new DataIntegrityViolationException(
            "violates check constraint ck_postagens_capturada_em"));

    var erro = assertThrows(
        PostagemException.class,
        () -> service.executar(10, 5, 2, URL, "abc", captura()));

    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    assertEquals("CAPTURA_INVALIDA", erro.getCodigo());
  }

  @Test
  void naoDeveEsconderOutraViolacaoDeIntegridade() {
    vinculoOk();
    when(gateway.inserir(any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new DataIntegrityViolationException("violates foreign key"));

    assertThrows(
        DataIntegrityViolationException.class,
        () -> service.executar(10, 5, 2, URL, "abc", captura()));
  }

  @Test
  void naoDeveTratarOutraChaveDuplicadaComoFotoRepetida() {
    vinculoOk();
    when(gateway.inserir(any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new DuplicateKeyException("violates constraint tb_postagens_pkey"));

    assertThrows(
        DuplicateKeyException.class,
        () -> service.executar(10, 5, 2, URL, "abc", captura()));
  }
}

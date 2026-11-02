package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import br.com.ecociente.mobile.core.domain.Postagem;
import br.com.ecociente.mobile.core.gateway.PostagemGateway;
import br.com.ecociente.mobile.core.service.validador.TriagemPostagemService;

class CriarPostagemComTriagemServiceTest {

  @Test
  @SuppressWarnings("unchecked")
  void comValidadorDesligadoACriacaoSegueSemTriagem() {
    var criar = mock(CriarPostagemService.class);
    var triagem = (ObjectProvider<TriagemPostagemService>) mock(ObjectProvider.class);
    var gateway = mock(PostagemGateway.class);
    var postagem = mock(Postagem.class);

    when(criar.executar(any(), any(), any(), any(), any(), any())).thenReturn(postagem);
    when(triagem.getIfAvailable()).thenReturn(null);

    var service = new CriarPostagemComTriagemService(criar, triagem, gateway);

    var resultado = service.executar(1, 2, 3, "https://exemplo/foto.jpg", "hash", OffsetDateTime.now());

    assertSame(postagem, resultado);
    verify(gateway, never()).buscarPorId(any());
  }
}

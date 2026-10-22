package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.ecociente.mobile.core.domain.CategoriaResiduo;
import br.com.ecociente.mobile.core.gateway.PostagemGateway;

class ConsultarCategoriasResiduoServiceTest {

  @Test
  void deveListarAsCategoriasDoGateway() {
    var gateway = mock(PostagemGateway.class);
    var categorias = List.of(
        new CategoriaResiduo(1, "Papel", "Papel limpo", 10),
        new CategoriaResiduo(2, "Plástico", null, 10));

    when(gateway.listarCategorias()).thenReturn(categorias);

    var resultado = new ConsultarCategoriasResiduoService(gateway).executar();

    assertEquals(categorias, resultado);
  }
}

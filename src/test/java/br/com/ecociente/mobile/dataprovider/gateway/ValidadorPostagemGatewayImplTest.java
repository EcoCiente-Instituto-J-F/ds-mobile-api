package br.com.ecociente.mobile.dataprovider.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.CannotCreateTransactionException;

import br.com.ecociente.mobile.core.domain.ResultadoVoto;
import br.com.ecociente.mobile.core.exception.PostagemException;
import br.com.ecociente.mobile.core.exception.validador.NotFoundException;
import br.com.ecociente.mobile.core.exception.validador.PermissaoNegadaException;
import br.com.ecociente.mobile.core.service.validador.ValidadorPostagemValidacaoService;

@ExtendWith(MockitoExtension.class)
class ValidadorPostagemGatewayImplTest {

  @Mock
  private ValidadorPostagemValidacaoService service;

  private ValidadorPostagemGatewayImpl gateway;

  @BeforeEach
  void preparar() {
    gateway = new ValidadorPostagemGatewayImpl(service);
  }

  @Test
  void deveRegistrarAprovacaoPeloServicoLocal() {
    when(service.votar(10, 20, "aprovar", null, null))
        .thenReturn(
            new br.com.ecociente.mobile.core.domain.validador.ResultadoVoto(
                3, true));

    var resultado = gateway.votar(
        10, 20, "aprovar", null, null);

    assertEquals(new ResultadoVoto(3, true), resultado);

    verify(service).votar(
        10, 20, "aprovar", null, null);
  }

  @Test
  void deveEncaminharMotivoEComentarioDaDenuncia() {
    when(service.votar(
        10, 20, "denunciar", 2, "Foto inadequada"))
        .thenReturn(
            new br.com.ecociente.mobile.core.domain.validador.ResultadoVoto(
                -5, false));

    var resultado = gateway.votar(
        10, 20, "denunciar", 2, "Foto inadequada");

    assertEquals(new ResultadoVoto(-5, false), resultado);

    verify(service).votar(
        10, 20, "denunciar", 2, "Foto inadequada");
  }

  @Test
  void deveTraduzirPostagemInexistente() {
    when(service.votar(10, 20, "aprovar", null, null))
        .thenThrow(
            new NotFoundException("Postagem não encontrada."));

    var erro = assertThrows(
        PostagemException.class,
        () -> gateway.votar(
            10, 20, "aprovar", null, null));

    assertEquals("Postagem não encontrada.", erro.getMessage());
  }

  @Test
  void deveTraduzirVotoNaoPermitido() {
    when(service.votar(10, 20, "aprovar", null, null))
        .thenThrow(
            new PermissaoNegadaException(
                "O autor não pode votar na própria postagem."));

    var erro = assertThrows(
        PostagemException.class,
        () -> gateway.votar(
            10, 20, "aprovar", null, null));

    assertEquals(
        "O autor não pode votar na própria postagem.",
        erro.getMessage());
  }

  @Test
  void deveTraduzirVotoDuplicado() {
    when(service.votar(10, 20, "aprovar", null, null))
        .thenThrow(
            new DuplicateKeyException(
                "Voto duplicado",
                new SQLException("Duplicado", "23505")));

    var erro = assertThrows(
        PostagemException.class,
        () -> gateway.votar(
            10, 20, "aprovar", null, null));

    assertEquals(
        "Você já votou nesta postagem",
        erro.getMessage());
  }

  @Test
  void deveTraduzirReferenciaInvalida() {
    when(service.votar(10, 20, "denunciar", 999, null))
        .thenThrow(
            new DataIntegrityViolationException(
                "Referência inválida",
                new SQLException("FK inválida", "23503")));

    var erro = assertThrows(
        PostagemException.class,
        () -> gateway.votar(
            10, 20, "denunciar", 999, null));

    assertEquals(
        "Referência inválida para o voto",
        erro.getMessage());
  }

  @Test
  void deveTraduzirJanelaEncerrada() {
    when(service.votar(10, 20, "aprovar", null, null))
        .thenThrow(
            new DataIntegrityViolationException(
                "Regra recusada",
                new SQLException(
                    "A postagem nao aceita novos votos",
                    "P0001")));

    var erro = assertThrows(
        PostagemException.class,
        () -> gateway.votar(
            10, 20, "aprovar", null, null));

    assertEquals(
        "A votação desta postagem já foi encerrada",
        erro.getMessage());
  }

  @Test
  void deveTraduzirMotivoObrigatorio() {
    when(service.votar(10, 20, "denunciar", null, null))
        .thenThrow(
            new DataIntegrityViolationException(
                "Regra recusada",
                new SQLException(
                    "motivo_denuncia_id e obrigatorio",
                    "P0001")));

    var erro = assertThrows(
        PostagemException.class,
        () -> gateway.votar(
            10, 20, "denunciar", null, null));

    assertEquals(
        "Informe o motivo da denúncia",
        erro.getMessage());
  }

  @Test
  void deveTraduzirAusenciaDeVinculoAprovado() {
    when(service.votar(10, 20, "aprovar", null, null))
        .thenThrow(
            new DataIntegrityViolationException(
                "Regra recusada",
                new SQLException(
                    "Usuario nao possui vinculo aprovado",
                    "P0001")));

    var erro = assertThrows(
        PostagemException.class,
        () -> gateway.votar(
            10, 20, "aprovar", null, null));

    assertEquals(
        "Você não tem vínculo aprovado com este condomínio",
        erro.getMessage());
  }

  @Test
  void deveTraduzirFalhaDeConexao() {
    when(service.votar(10, 20, "aprovar", null, null))
        .thenThrow(
            new DataAccessResourceFailureException(
                "Banco indisponível"));

    assertIndisponibilidade();
  }

  @Test
  void deveTraduzirFalhaAoAbrirTransacao() {
    when(service.votar(10, 20, "aprovar", null, null))
        .thenThrow(
            new CannotCreateTransactionException(
                "Não foi possível abrir a transação"));

    assertIndisponibilidade();
  }

  private void assertIndisponibilidade() {
    var erro = assertThrows(
        PostagemException.class,
        () -> gateway.votar(
            10, 20, "aprovar", null, null));

    assertEquals(
        "Não foi possível registrar o voto agora. Tente novamente.",
        erro.getMessage());
  }
}
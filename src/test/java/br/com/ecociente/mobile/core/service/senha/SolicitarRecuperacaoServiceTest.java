package br.com.ecociente.mobile.core.service.senha;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ecociente.mobile.core.domain.senha.RecuperacaoSenha;
import br.com.ecociente.mobile.core.domain.senha.Usuario;
import br.com.ecociente.mobile.core.gateway.senha.EmailGateway;
import br.com.ecociente.mobile.core.gateway.senha.FirebaseSenhaGateway;
import br.com.ecociente.mobile.core.gateway.senha.RecuperacaoSenhaGateway;
import br.com.ecociente.mobile.core.gateway.senha.SenhaEncoderGateway;
import br.com.ecociente.mobile.core.gateway.senha.UsuarioGateway;

@ExtendWith(MockitoExtension.class)
class SolicitarRecuperacaoServiceTest {

    private static final Integer USUARIO_ID = 10;
    private static final String EMAIL = "morador@teste.com";

    @Mock
    private UsuarioGateway usuarioGateway;

    @Mock
    private RecuperacaoSenhaGateway recuperacaoSenhaGateway;

    @Mock
    private SenhaEncoderGateway senhaEncoderGateway;

    @Mock
    private EmailGateway emailGateway;

    @Mock
    private FirebaseSenhaGateway firebaseSenhaGateway;

    @InjectMocks
    private SenhaService service;

    @Test
    void deveResponderSemEnviarEmailQuandoUsuarioNaoExiste() {
        when(usuarioGateway.buscarPorEmail(EMAIL))
                .thenReturn(Optional.empty());

        assertNotNull(service.solicitarRecuperacao(EMAIL));

        verifyNoInteractions(
                recuperacaoSenhaGateway,
                senhaEncoderGateway,
                emailGateway,
                firebaseSenhaGateway);
    }

    @Test
    void devePersistirHashEEnviarCodigoComQuatroDigitos() {
        prepararUsuario();

        when(senhaEncoderGateway.gerarHash(anyString()))
                .thenReturn("hash-do-codigo");

        Timestamp antes = new Timestamp(System.currentTimeMillis());

        assertNotNull(service.solicitarRecuperacao(EMAIL));

        Timestamp depois = new Timestamp(System.currentTimeMillis());

        var codigoCaptor = ArgumentCaptor.forClass(String.class);
        verify(senhaEncoderGateway).gerarHash(codigoCaptor.capture());

        String codigo = codigoCaptor.getValue();

        assertTrue(codigo.matches("[0-9]{4}"));

        var recuperacaoCaptor =
                ArgumentCaptor.forClass(RecuperacaoSenha.class);

        verify(recuperacaoSenhaGateway)
                .salvar(recuperacaoCaptor.capture());

        var recuperacao = recuperacaoCaptor.getValue();

        assertEquals(USUARIO_ID, recuperacao.getUsuarioId());
        assertEquals("hash-do-codigo", recuperacao.getTokenHash());
        assertNotEquals(codigo, recuperacao.getTokenHash());
        assertNull(recuperacao.getUtilizadoEm());

        long validade = 15L * 60 * 1000;

        assertTrue(
                recuperacao.getExpiraEm().getTime()
                        >= antes.getTime() + validade);

        assertTrue(
                recuperacao.getExpiraEm().getTime()
                        <= depois.getTime() + validade);

        verify(emailGateway).enviarRecuperacao(EMAIL, codigo);
        verifyNoInteractions(firebaseSenhaGateway);
    }

    @Test
    void deveInvalidarCodigosAnterioresAntesDeSalvarEEnviar() {
        prepararUsuario();

        when(senhaEncoderGateway.gerarHash(anyString()))
                .thenReturn("hash-do-codigo");

        service.solicitarRecuperacao(EMAIL);

        var ordem = inOrder(
                recuperacaoSenhaGateway,
                emailGateway);

        ordem.verify(recuperacaoSenhaGateway)
                .invalidarPendentes(eq(USUARIO_ID), any());

        ordem.verify(recuperacaoSenhaGateway)
                .salvar(any(RecuperacaoSenha.class));

        ordem.verify(emailGateway)
                .enviarRecuperacao(eq(EMAIL), matches("[0-9]{4}"));
    }

    @Test
    void devePropagarFalhaDeEnvioDoEmail() {
        prepararUsuario();

        when(senhaEncoderGateway.gerarHash(anyString()))
                .thenReturn("hash-do-codigo");

        var falha = new IllegalStateException(
                "SMTP indisponível");

        doThrow(falha).when(emailGateway)
                .enviarRecuperacao(eq(EMAIL), anyString());

        var recebida = assertThrows(
                IllegalStateException.class,
                () -> service.solicitarRecuperacao(EMAIL));

        assertSame(falha, recebida);
        verifyNoInteractions(firebaseSenhaGateway);
    }

    @Test
    void naoDeveEnviarEmailQuandoPersistenciaFalha() {
        prepararUsuario();

        when(senhaEncoderGateway.gerarHash(anyString()))
                .thenReturn("hash-do-codigo");

        var falha = new IllegalStateException(
                "Banco indisponível");

        doThrow(falha).when(recuperacaoSenhaGateway)
                .salvar(any(RecuperacaoSenha.class));

        var recebida = assertThrows(
                IllegalStateException.class,
                () -> service.solicitarRecuperacao(EMAIL));

        assertSame(falha, recebida);

        verifyNoInteractions(
                emailGateway,
                firebaseSenhaGateway);
    }

    private void prepararUsuario() {
        var usuario = Usuario.builder()
                .id(USUARIO_ID)
                .email(EMAIL)
                .senha("hash-antigo")
                .build();

        when(usuarioGateway.buscarPorEmail(EMAIL))
                .thenReturn(Optional.of(usuario));
    }
}
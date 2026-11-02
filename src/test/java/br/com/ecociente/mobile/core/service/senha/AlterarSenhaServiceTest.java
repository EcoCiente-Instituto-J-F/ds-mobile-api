package br.com.ecociente.mobile.core.service.senha;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ecociente.mobile.core.domain.senha.Usuario;
import br.com.ecociente.mobile.core.exception.senha.RecursoNaoEncontradoException;
import br.com.ecociente.mobile.core.exception.senha.RegraNegocioException;
import br.com.ecociente.mobile.core.exception.senha.SincronizacaoSenhaException;
import br.com.ecociente.mobile.core.gateway.senha.EmailGateway;
import br.com.ecociente.mobile.core.gateway.senha.FirebaseSenhaGateway;
import br.com.ecociente.mobile.core.gateway.senha.RecuperacaoSenhaGateway;
import br.com.ecociente.mobile.core.gateway.senha.SenhaEncoderGateway;
import br.com.ecociente.mobile.core.gateway.senha.UsuarioGateway;

@ExtendWith(MockitoExtension.class)
class AlterarSenhaServiceTest {

    private static final Integer USUARIO_ID = 10;
    private static final String EMAIL = "morador@teste.com";
    private static final String SENHA_ATUAL = "SenhaAtual@2026";
    private static final String NOVA_SENHA = "NovaSenha@2026";

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
    void deveAlterarSenhaSincronizarFirebaseEInvalidarCodigos() {
        prepararUsuario();

        when(senhaEncoderGateway.corresponde(SENHA_ATUAL, "hash-antigo"))
                .thenReturn(true);

        when(senhaEncoderGateway.gerarHash(NOVA_SENHA))
                .thenReturn("hash-novo");

        var processadoEm = service.alterarSenha(
                USUARIO_ID, SENHA_ATUAL, NOVA_SENHA, NOVA_SENHA);

        assertNotNull(processadoEm);

        var ordem = inOrder(
                usuarioGateway,
                firebaseSenhaGateway,
                recuperacaoSenhaGateway);

        ordem.verify(usuarioGateway).buscarAtivoPorId(USUARIO_ID);
        ordem.verify(usuarioGateway).atualizarSenha(USUARIO_ID, "hash-novo");
        ordem.verify(firebaseSenhaGateway).atualizarSenha(EMAIL, NOVA_SENHA);
        ordem.verify(recuperacaoSenhaGateway)
                .invalidarPendentes(eq(USUARIO_ID), any());

        verifyNoInteractions(emailGateway);
    }

    @Test
    void deveRecusarUsuarioInexistenteOuInativo() {
        when(usuarioGateway.buscarAtivoPorId(USUARIO_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.alterarSenha(
                        USUARIO_ID, SENHA_ATUAL, NOVA_SENHA, NOVA_SENHA));

        verify(usuarioGateway, never()).atualizarSenha(anyInt(), anyString());

        verifyNoInteractions(
                senhaEncoderGateway,
                firebaseSenhaGateway,
                recuperacaoSenhaGateway,
                emailGateway);
    }

    @Test
    void deveRecusarSenhaAtualIncorreta() {
        prepararUsuario();

        when(senhaEncoderGateway.corresponde(SENHA_ATUAL, "hash-antigo"))
                .thenReturn(false);

        assertThrows(
                RegraNegocioException.class,
                () -> service.alterarSenha(
                        USUARIO_ID, SENHA_ATUAL, NOVA_SENHA, NOVA_SENHA));

        verificarSemAlteracoes();
        verify(senhaEncoderGateway, never()).gerarHash(anyString());
    }

    @Test
    void deveRecusarConfirmacaoDiferente() {
        prepararUsuario();

        assertThrows(
                RegraNegocioException.class,
                () -> service.alterarSenha(
                        USUARIO_ID,
                        SENHA_ATUAL,
                        NOVA_SENHA,
                        "OutraSenha@2026"));

        verificarSemAlteracoes();
        verifyNoInteractions(senhaEncoderGateway);
    }

    @Test
    void deveRecusarNovaSenhaForaDoPadrao() {
        prepararUsuario();

        assertThrows(
                RegraNegocioException.class,
                () -> service.alterarSenha(
                        USUARIO_ID, SENHA_ATUAL, "123", "123"));

        verificarSemAlteracoes();
        verifyNoInteractions(senhaEncoderGateway);
    }

    @Test
    void devePropagarFalhaFirebaseSemInvalidarCodigos() {
        prepararUsuario();

        when(senhaEncoderGateway.corresponde(SENHA_ATUAL, "hash-antigo"))
                .thenReturn(true);

        when(senhaEncoderGateway.gerarHash(NOVA_SENHA))
                .thenReturn("hash-novo");

        var falha = new SincronizacaoSenhaException(
                "Falha na sincronização",
                new IllegalStateException("Firebase indisponível"));

        doThrow(falha).when(firebaseSenhaGateway)
                .atualizarSenha(EMAIL, NOVA_SENHA);

        var recebida = assertThrows(
                SincronizacaoSenhaException.class,
                () -> service.alterarSenha(
                        USUARIO_ID, SENHA_ATUAL, NOVA_SENHA, NOVA_SENHA));

        assertSame(falha, recebida);

        verify(usuarioGateway).atualizarSenha(USUARIO_ID, "hash-novo");
        verifyNoInteractions(recuperacaoSenhaGateway, emailGateway);
    }

    private void prepararUsuario() {
        var usuario = Usuario.builder()
                .id(USUARIO_ID)
                .email(EMAIL)
                .senha("hash-antigo")
                .build();

        when(usuarioGateway.buscarAtivoPorId(USUARIO_ID))
                .thenReturn(Optional.of(usuario));
    }

    private void verificarSemAlteracoes() {
        verify(usuarioGateway, never()).atualizarSenha(anyInt(), anyString());

        verifyNoInteractions(
                firebaseSenhaGateway,
                recuperacaoSenhaGateway,
                emailGateway);
    }
}
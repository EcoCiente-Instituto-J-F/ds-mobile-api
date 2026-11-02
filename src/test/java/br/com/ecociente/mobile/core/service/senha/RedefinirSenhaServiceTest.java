package br.com.ecociente.mobile.core.service.senha;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ecociente.mobile.core.domain.senha.RecuperacaoSenha;
import br.com.ecociente.mobile.core.domain.senha.Usuario;
import br.com.ecociente.mobile.core.exception.senha.RegraNegocioException;
import br.com.ecociente.mobile.core.exception.senha.SincronizacaoSenhaException;
import br.com.ecociente.mobile.core.gateway.senha.EmailGateway;
import br.com.ecociente.mobile.core.gateway.senha.FirebaseSenhaGateway;
import br.com.ecociente.mobile.core.gateway.senha.RecuperacaoSenhaGateway;
import br.com.ecociente.mobile.core.gateway.senha.SenhaEncoderGateway;
import br.com.ecociente.mobile.core.gateway.senha.UsuarioGateway;

@ExtendWith(MockitoExtension.class)
class RedefinirSenhaServiceTest {

    private static final Integer USUARIO_ID = 10;
    private static final String EMAIL = "morador@teste.com";
    private static final String TOKEN = "1234";
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
    void deveRedefinirSenhaSincronizarFirebaseEConsumirCodigo() {
        var recuperacao = prepararRecuperacao(
                Instant.now().plus(1, ChronoUnit.HOURS),
                null);

        when(senhaEncoderGateway.corresponde(TOKEN, "hash-token"))
                .thenReturn(true);

        when(senhaEncoderGateway.gerarHash(NOVA_SENHA))
                .thenReturn("hash-novo");

        var processadoEm = redefinir(TOKEN);

        assertNotNull(processadoEm);
        assertNotNull(recuperacao.getUtilizadoEm());

        var ordem = inOrder(
                usuarioGateway,
                firebaseSenhaGateway,
                recuperacaoSenhaGateway);

        ordem.verify(usuarioGateway).buscarPorEmail(EMAIL);
        ordem.verify(recuperacaoSenhaGateway)
                .buscarUltimaPorUsuarioId(USUARIO_ID);
        ordem.verify(usuarioGateway)
                .atualizarSenha(USUARIO_ID, "hash-novo");
        ordem.verify(firebaseSenhaGateway)
                .atualizarSenha(EMAIL, NOVA_SENHA);
        ordem.verify(recuperacaoSenhaGateway).salvar(recuperacao);
        ordem.verify(recuperacaoSenhaGateway)
                .invalidarPendentes(eq(USUARIO_ID), any());

        verifyNoInteractions(emailGateway);
    }

    @Test
    void deveRecusarCodigoExpirado() {
        prepararRecuperacao(
                Instant.now().minus(1, ChronoUnit.HOURS),
                null);

        assertThrows(RegraNegocioException.class, () -> redefinir(TOKEN));

        verificarSemAlteracoes();
        verifyNoInteractions(senhaEncoderGateway);
    }

    @Test
    void deveRecusarCodigoJaUtilizado() {
        prepararRecuperacao(
                Instant.now().plus(1, ChronoUnit.HOURS),
                Timestamp.from(Instant.now().minusSeconds(60)));

        assertThrows(RegraNegocioException.class, () -> redefinir(TOKEN));

        verificarSemAlteracoes();
        verifyNoInteractions(senhaEncoderGateway);
    }

    @Test
    void deveRecusarCodigoQueNaoCorrespondeAoHash() {
        prepararRecuperacao(
                Instant.now().plus(1, ChronoUnit.HOURS),
                null);

        when(senhaEncoderGateway.corresponde(TOKEN, "hash-token"))
                .thenReturn(false);

        assertThrows(RegraNegocioException.class, () -> redefinir(TOKEN));

        verificarSemAlteracoes();
        verify(senhaEncoderGateway, never()).gerarHash(anyString());
    }

    @Test
    void deveRecusarFormatoDeCodigoInvalidoSemConsultarBanco() {
        assertThrows(RegraNegocioException.class, () -> redefinir("12ab"));

        verifyNoInteractions(
                usuarioGateway,
                recuperacaoSenhaGateway,
                senhaEncoderGateway,
                firebaseSenhaGateway,
                emailGateway);
    }

    @Test
    void deveRecusarUsuarioSemRecuperacao() {
        prepararUsuario();

        when(recuperacaoSenhaGateway.buscarUltimaPorUsuarioId(USUARIO_ID))
                .thenReturn(Optional.empty());

        assertThrows(RegraNegocioException.class, () -> redefinir(TOKEN));

        verificarSemAlteracoes();
        verifyNoInteractions(senhaEncoderGateway);
    }

    @Test
    void deveRecusarEmailNaoCadastrado() {
        when(usuarioGateway.buscarPorEmail(EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(RegraNegocioException.class, () -> redefinir(TOKEN));

        verifyNoInteractions(
                recuperacaoSenhaGateway,
                senhaEncoderGateway,
                firebaseSenhaGateway,
                emailGateway);

        verify(usuarioGateway, never()).atualizarSenha(anyInt(), anyString());
    }

    @Test
    void deveImpedirSegundoUsoDoMesmoCodigo() {
        prepararRecuperacao(
                Instant.now().plus(1, ChronoUnit.HOURS),
                null);

        when(senhaEncoderGateway.corresponde(TOKEN, "hash-token"))
                .thenReturn(true);

        when(senhaEncoderGateway.gerarHash(NOVA_SENHA))
                .thenReturn("hash-novo");

        redefinir(TOKEN);

        assertThrows(RegraNegocioException.class, () -> redefinir(TOKEN));

        verify(usuarioGateway, times(1))
                .atualizarSenha(USUARIO_ID, "hash-novo");

        verify(firebaseSenhaGateway, times(1))
                .atualizarSenha(EMAIL, NOVA_SENHA);
    }

    @Test
    void devePropagarFalhaFirebaseSemConsumirCodigo() {
        var recuperacao = prepararRecuperacao(
                Instant.now().plus(1, ChronoUnit.HOURS),
                null);

        when(senhaEncoderGateway.corresponde(TOKEN, "hash-token"))
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
                () -> redefinir(TOKEN));

        assertSame(falha, recebida);
        assertNull(recuperacao.getUtilizadoEm());

        verify(recuperacaoSenhaGateway, never()).salvar(any());
        verify(recuperacaoSenhaGateway, never())
                .invalidarPendentes(anyInt(), any());

        verifyNoInteractions(emailGateway);
    }

    private Timestamp redefinir(String token) {
        return service.redefinirSenha(
                EMAIL,
                token,
                NOVA_SENHA,
                NOVA_SENHA);
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

    private RecuperacaoSenha prepararRecuperacao(
            Instant expiracao,
            Timestamp utilizadoEm) {

        prepararUsuario();

        var recuperacao = RecuperacaoSenha.builder()
                .usuarioId(USUARIO_ID)
                .tokenHash("hash-token")
                .expiraEm(Timestamp.from(expiracao))
                .utilizadoEm(utilizadoEm)
                .build();

        when(recuperacaoSenhaGateway.buscarUltimaPorUsuarioId(USUARIO_ID))
                .thenReturn(Optional.of(recuperacao));

        return recuperacao;
    }

    private void verificarSemAlteracoes() {
        verify(usuarioGateway, never()).atualizarSenha(anyInt(), anyString());

        verify(recuperacaoSenhaGateway, never()).salvar(any());
        verify(recuperacaoSenhaGateway, never())
                .invalidarPendentes(anyInt(), any());

        verifyNoInteractions(firebaseSenhaGateway, emailGateway);
    }
}
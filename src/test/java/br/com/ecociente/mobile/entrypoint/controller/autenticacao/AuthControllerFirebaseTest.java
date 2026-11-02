package br.com.ecociente.mobile.entrypoint.controller.autenticacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import br.com.ecociente.mobile.core.domain.autenticacao.PerfilUsuarioType;
import br.com.ecociente.mobile.core.domain.autenticacao.SessaoAutenticada;
import br.com.ecociente.mobile.core.exception.autenticacao.RecursoNaoEncontradoException;
import br.com.ecociente.mobile.core.service.autenticacao.AuthService;
import br.com.ecociente.mobile.entrypoint.mapper.autenticacao.AutenticacaoMapperEntry;

class AuthControllerFirebaseTest {

    private AuthService authService;
    private FirebaseAuth firebaseAuth;
    private AuthController controller;

    @BeforeEach
    void inicializar() {
        authService = mock(AuthService.class);
        firebaseAuth = mock(FirebaseAuth.class);
        controller = new AuthController(authService, new AutenticacaoMapperEntry(), firebaseAuth);
    }

    @Test
    void aceitaTokenVerificadoDeContaVinculada() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token);
        when(token.getEmail()).thenReturn("PESSOA@EXAMPLE.COM");
        when(token.isEmailVerified()).thenReturn(true);
        when(authService.loginFirebase("pessoa@example.com")).thenReturn(
                new SessaoAutenticada("jwt-api", "Bearer", 300, 7,
                        "Pessoa", "pessoa@example.com", PerfilUsuarioType.MORADOR));

        var resposta = controller.loginFirebase(new AuthController.FirebaseLoginRequest("id-token"));

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertEquals(7, resposta.getBody().usuarioId());
        assertEquals("jwt-api", resposta.getBody().token());
        verify(firebaseAuth).verifyIdToken("id-token", true);
    }

    @Test
    void recusaEmailNaoVerificado() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token);
        when(token.getEmail()).thenReturn("pessoa@example.com");
        when(token.isEmailVerified()).thenReturn(false);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> controller.loginFirebase(new AuthController.FirebaseLoginRequest("id-token")));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        verifyNoInteractions(authService);
    }

    @Test
    void recusaTokenInvalidoOuRevogado() throws Exception {
        when(firebaseAuth.verifyIdToken("id-token", true))
                .thenThrow(mock(FirebaseAuthException.class));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> controller.loginFirebase(new AuthController.FirebaseLoginRequest("id-token")));

        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
        verifyNoInteractions(authService);
    }

    @Test
    void recusaContaNaoVinculadaAoPostgres() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token);
        when(token.getEmail()).thenReturn("pessoa@example.com");
        when(token.isEmailVerified()).thenReturn(true);
        when(authService.loginFirebase("pessoa@example.com"))
                .thenThrow(new RecursoNaoEncontradoException("Conta nao vinculada"));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> controller.loginFirebase(new AuthController.FirebaseLoginRequest("id-token")));

        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
    }
}

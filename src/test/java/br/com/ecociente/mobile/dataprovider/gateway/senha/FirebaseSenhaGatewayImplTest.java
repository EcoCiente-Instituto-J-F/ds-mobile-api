package br.com.ecociente.mobile.dataprovider.gateway.senha;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;

import br.com.ecociente.mobile.core.exception.senha.SincronizacaoSenhaException;

@ExtendWith(MockitoExtension.class)
class FirebaseSenhaGatewayImplTest {

    private static final String EMAIL = "morador@teste.com";
    private static final String SENHA = "NovaSenha@2026";

    @Mock
    private FirebaseAuth firebaseAuth;

    @Mock
    private UserRecord usuario;

    @InjectMocks
    private FirebaseSenhaGatewayImpl gateway;

    @Test
    void deveNormalizarEmailEAtualizarUsuarioEncontrado() throws Exception {
        when(firebaseAuth.getUserByEmail(EMAIL)).thenReturn(usuario);
        when(usuario.getUid()).thenReturn("uid-morador");

        gateway.atualizarSenha("  MORADOR@Teste.COM  ", SENHA);

        var ordem = inOrder(firebaseAuth, usuario);

        ordem.verify(firebaseAuth).getUserByEmail(EMAIL);
        ordem.verify(usuario).getUid();
        ordem.verify(firebaseAuth)
                .updateUser(any(UserRecord.UpdateRequest.class));
    }

    @Test
    void deveIgnorarUsuarioNaoCadastradoNoFirebase() throws Exception {
        var falha = mock(FirebaseAuthException.class);

        when(falha.getAuthErrorCode())
                .thenReturn(AuthErrorCode.USER_NOT_FOUND);

        when(firebaseAuth.getUserByEmail(EMAIL))
                .thenThrow(falha);

        assertDoesNotThrow(
                () -> gateway.atualizarSenha(EMAIL, SENHA));

        verify(firebaseAuth, never())
                .updateUser(any(UserRecord.UpdateRequest.class));
    }

    @Test
    void deveTraduzirFalhaNaConsultaPreservandoCausa() throws Exception {
        var falha = mock(FirebaseAuthException.class);

        when(falha.getAuthErrorCode())
                .thenReturn(AuthErrorCode.USER_DISABLED);

        when(firebaseAuth.getUserByEmail(EMAIL))
                .thenThrow(falha);

        var recebida = assertThrows(
                SincronizacaoSenhaException.class,
                () -> gateway.atualizarSenha(EMAIL, SENHA));

        assertSame(falha, recebida.getCause());

        verify(firebaseAuth, never())
                .updateUser(any(UserRecord.UpdateRequest.class));
    }

    @Test
    void deveTraduzirFalhaNaAtualizacaoPreservandoCausa() throws Exception {
        when(firebaseAuth.getUserByEmail(EMAIL)).thenReturn(usuario);
        when(usuario.getUid()).thenReturn("uid-morador");

        var falha = mock(FirebaseAuthException.class);

        when(falha.getAuthErrorCode())
                .thenReturn(AuthErrorCode.USER_DISABLED);

        when(firebaseAuth.updateUser(any(UserRecord.UpdateRequest.class)))
                .thenThrow(falha);

        var recebida = assertThrows(
                SincronizacaoSenhaException.class,
                () -> gateway.atualizarSenha(EMAIL, SENHA));

        assertSame(falha, recebida.getCause());

        verify(firebaseAuth)
                .updateUser(any(UserRecord.UpdateRequest.class));
    }
}
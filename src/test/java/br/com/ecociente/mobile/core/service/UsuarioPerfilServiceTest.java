package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ecociente.mobile.core.domain.UsuarioPerfil;
import br.com.ecociente.mobile.core.gateway.UsuarioPerfilGateway;

@ExtendWith(MockitoExtension.class)
class UsuarioPerfilServiceTest {

    @Mock
    private UsuarioPerfilGateway gateway;

    @InjectMocks
    private UsuarioPerfilService service;

    @Test
    void deveRetornarPerfilDoUsuarioSolicitado() {
        UsuarioPerfil perfil = mock(UsuarioPerfil.class);

        when(gateway.buscarPorUsuarioId(1))
            .thenReturn(Optional.of(perfil));

        assertSame(
            perfil,
            service.buscarMeuPerfil(1).orElseThrow()
        );

        verify(gateway).buscarPorUsuarioId(1);
        verifyNoMoreInteractions(gateway);
    }

    @Test
    void deveRetornarVazioQuandoUsuarioNaoExistir() {
        when(gateway.buscarPorUsuarioId(999))
            .thenReturn(Optional.empty());

        assertTrue(service.buscarMeuPerfil(999).isEmpty());
    }

    @Test
    void devePropagarFalhaDaConsulta() {
        RuntimeException falha = new RuntimeException("Falha de consulta");

        when(gateway.buscarPorUsuarioId(1)).thenThrow(falha);

        assertSame(
            falha,
            assertThrows(
                RuntimeException.class,
                () -> service.buscarMeuPerfil(1)
            )
        );
    }
}
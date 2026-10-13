package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ecociente.mobile.core.domain.Curso;
import br.com.ecociente.mobile.core.gateway.CursoGateway;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock
    private CursoGateway gateway;

    @InjectMocks
    private CursoService service;

    @Test
    void deveRetornarCursosDisponibilizadosPeloGateway() {
        List<Curso> cursos = List.of(
            new Curso(1, "Reciclagem", "Separação de resíduos", true),
            new Curso(2, "Compostagem", null, true)
        );

        when(gateway.buscarCursosAtivos()).thenReturn(cursos);

        assertEquals(cursos, service.listarCursos());

        verify(gateway).buscarCursosAtivos();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoExistiremCursosAtivos() {
        when(gateway.buscarCursosAtivos()).thenReturn(List.of());

        assertTrue(service.listarCursos().isEmpty());
    }

    @Test
    void deveRetornarCursoEncontrado() {
        Curso curso = new Curso(
            1,
            "Reciclagem",
            "Separação de resíduos",
            true
        );

        when(gateway.buscarCursoAtivoPorId(1))
            .thenReturn(Optional.of(curso));

        assertEquals(Optional.of(curso), service.buscarCurso(1));

        verify(gateway).buscarCursoAtivoPorId(1);
    }

    @Test
    void deveRetornarOptionalVazioQuandoCursoNaoEstiverDisponivel() {
        when(gateway.buscarCursoAtivoPorId(999))
            .thenReturn(Optional.empty());

        assertTrue(service.buscarCurso(999).isEmpty());
    }

    @Test
    void devePropagarFalhaDoGatewayNaListagem() {
        RuntimeException falha = new RuntimeException("Falha de consulta");

        when(gateway.buscarCursosAtivos()).thenThrow(falha);

        RuntimeException resultado = assertThrows(
            RuntimeException.class,
            () -> service.listarCursos()
        );

        assertSame(falha, resultado);
    }

    @Test
    void devePropagarFalhaDoGatewayNaBuscaIndividual() {
        RuntimeException falha = new RuntimeException("Falha de consulta");

        when(gateway.buscarCursoAtivoPorId(1)).thenThrow(falha);

        RuntimeException resultado = assertThrows(
            RuntimeException.class,
            () -> service.buscarCurso(1)
        );

        assertSame(falha, resultado);
    }
}
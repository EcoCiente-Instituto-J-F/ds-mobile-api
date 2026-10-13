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

import br.com.ecociente.mobile.core.domain.Aula;
import br.com.ecociente.mobile.core.domain.Curso;
import br.com.ecociente.mobile.core.gateway.AulaGateway;
import br.com.ecociente.mobile.core.gateway.CursoGateway;

@ExtendWith(MockitoExtension.class)
class AulaServiceTest {

    @Mock
    private AulaGateway aulaGateway;

    @Mock
    private CursoGateway cursoGateway;

    @InjectMocks
    private AulaService service;

    @Test
    void deveListarAulasQuandoCursoEstiverAtivo() {
        Curso curso = new Curso(1, "Reciclagem", null, true);
        List<Aula> aulas = List.of(
            new Aula(11, 1, "Introdução", "Por que reciclar?", 1),
            new Aula(10, 1, "Separação", "Separe os resíduos.", 2)
        );

        when(cursoGateway.buscarCursoAtivoPorId(1))
            .thenReturn(Optional.of(curso));
        when(aulaGateway.buscarAulasPorCursoId(1)).thenReturn(aulas);

        assertEquals(Optional.of(aulas), service.listarAulas(1));

        verify(aulaGateway).buscarAulasPorCursoId(1);
    }

    @Test
    void deveRetornarListaVaziaParaCursoAtivoSemAulas() {
        when(cursoGateway.buscarCursoAtivoPorId(4))
            .thenReturn(Optional.of(
                new Curso(4, "Curso sem aulas", null, true)
            ));
        when(aulaGateway.buscarAulasPorCursoId(4)).thenReturn(List.of());

        var resultado = service.listarAulas(4);

        assertTrue(resultado.isPresent());
        assertTrue(resultado.orElseThrow().isEmpty());
    }

    @Test
    void naoDeveConsultarAulasQuandoCursoEstiverIndisponivel() {
        when(cursoGateway.buscarCursoAtivoPorId(3))
            .thenReturn(Optional.empty());

        assertTrue(service.listarAulas(3).isEmpty());

        verifyNoInteractions(aulaGateway);
    }

    @Test
    void deveBuscarAulaDisponivel() {
        Aula aula = new Aula(11, 1, "Introdução", "Por que reciclar?", 1);

        when(aulaGateway.buscarAulaDeCursoAtivoPorId(11))
            .thenReturn(Optional.of(aula));

        assertEquals(Optional.of(aula), service.buscarAula(11));
        verifyNoInteractions(cursoGateway);
    }

    @Test
    void deveRetornarOptionalVazioParaAulaIndisponivel() {
        when(aulaGateway.buscarAulaDeCursoAtivoPorId(999))
            .thenReturn(Optional.empty());

        assertTrue(service.buscarAula(999).isEmpty());
    }

    @Test
    void devePropagarFalhaNaConsultaDasAulas() {
        RuntimeException falha = new RuntimeException("Falha de consulta");

        when(cursoGateway.buscarCursoAtivoPorId(1))
            .thenReturn(Optional.of(
                new Curso(1, "Reciclagem", null, true)
            ));
        when(aulaGateway.buscarAulasPorCursoId(1)).thenThrow(falha);

        assertSame(
            falha,
            assertThrows(RuntimeException.class, () -> service.listarAulas(1))
        );
    }
}
package br.com.ecociente.mobile.core.service.calendario;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ecociente.mobile.core.domain.calendario.Coleta;
import br.com.ecociente.mobile.core.domain.calendario.NovaColeta;
import br.com.ecociente.mobile.core.domain.calendario.StatusType;
import br.com.ecociente.mobile.core.exception.calendario.AgendamentoInvalidoException;
import br.com.ecociente.mobile.core.exception.calendario.OperacaoColetaNaoPermitidaException;
import br.com.ecociente.mobile.core.gateway.calendario.ColetaGateway;

@ExtendWith(MockitoExtension.class)
class ColetaRegrasAdicionaisTest {

    @Mock private ColetaGateway gateway;
    @InjectMocks private ColetaService service;

    @Test
    void deveRecusarUsuarioInativo() {
        assertThrows(
                OperacaoColetaNaoPermitidaException.class,
                () -> service.criar(10, entrada()));

        verify(gateway, never()).criar(anyInt(), anyString(), any());
    }

    @Test
    void deveRecusarDataPassada() {
        when(gateway.usuarioAtivo(10)).thenReturn(true);

        var entrada = new NovaColeta(
                1, 2, OffsetDateTime.now().minusDays(1), null);

        assertThrows(
                AgendamentoInvalidoException.class,
                () -> service.criar(10, entrada));

        verify(gateway, never()).criar(anyInt(), anyString(), any());
    }

    @Test
    void deveCriarSolicitacaoComOrigemCondominio() {
        var entrada = entrada();

        prepararCriacao();
        when(gateway.sindicoResponsavel(10, 1)).thenReturn(true);

        service.criar(10, entrada);

        verify(gateway).criar(10, "CONDOMINIO", entrada);
    }

    @Test
    void deveCriarSolicitacaoComOrigemCooperativa() {
        var entrada = entrada();

        prepararCriacao();
        when(gateway.responsavelCooperativa(10, 2)).thenReturn(true);

        service.criar(10, entrada);

        verify(gateway).criar(10, "COOPERATIVA", entrada);
    }

    @Test
    void deveBloquearUsuarioQueRepresentaAsDuasPartes() {
        prepararCriacao();

        when(gateway.sindicoResponsavel(10, 1)).thenReturn(true);
        when(gateway.responsavelCooperativa(10, 2)).thenReturn(true);

        assertThrows(
                OperacaoColetaNaoPermitidaException.class,
                () -> service.criar(10, entrada()));

        verify(gateway, never()).criar(anyInt(), anyString(), any());
    }

    @Test
    void solicitanteNaoPodeConfirmarPropriaSolicitacao() {
        when(gateway.usuarioAtivo(10)).thenReturn(true);

        var propria = new Coleta(
                5, 1, 2, StatusType.AGENDADO,
                OffsetDateTime.now().plusDays(2),
                null, false, 10, "CONDOMINIO", null, null);

        when(gateway.buscarComBloqueio(5))
                .thenReturn(Optional.of(propria));

        when(gateway.sindicoResponsavel(10, 1)).thenReturn(true);

        assertThrows(
                OperacaoColetaNaoPermitidaException.class,
                () -> service.atualizarStatus(10, 5, StatusType.CONFIRMADO));

        verificarSemMudancaStatus();
    }

    @Test
    void destinatarioPodeConfirmarSolicitacao() {
        prepararStatus(StatusType.AGENDADO);

        when(gateway.responsavelCooperativa(10, 2)).thenReturn(true);

        // O solicitante do registro é 99, não o usuário 10.
        service.atualizarStatus(10, 5, StatusType.CONFIRMADO);

        verify(gateway).atualizarStatus(5, StatusType.CONFIRMADO, 10);
    }

    @Test
    void sindicoNaoPodeMarcarColetaComoRealizada() {
        prepararStatus(StatusType.CONFIRMADO);

        when(gateway.sindicoResponsavel(10, 1)).thenReturn(true);

        assertThrows(
                OperacaoColetaNaoPermitidaException.class,
                () -> service.atualizarStatus(10, 5, StatusType.REALIZADO));

        verificarSemMudancaStatus();
    }

    @Test
    void cooperativaPodeMarcarConfirmadaComoRealizada() {
        prepararStatus(StatusType.CONFIRMADO);

        when(gateway.responsavelCooperativa(10, 2)).thenReturn(true);

        service.atualizarStatus(10, 5, StatusType.REALIZADO);

        verify(gateway).atualizarStatus(5, StatusType.REALIZADO, 88);
    }

    @Test
    void naoDeveReabrirAgendamentoCancelado() {
        prepararStatus(StatusType.CANCELADO);

        when(gateway.responsavelCooperativa(10, 2)).thenReturn(true);

        assertThrows(
                AgendamentoInvalidoException.class,
                () -> service.atualizarStatus(10, 5, StatusType.CONFIRMADO));

        verificarSemMudancaStatus();
    }

    private NovaColeta entrada() {
        return new NovaColeta(
                1, 2, OffsetDateTime.now().plusDays(2), null);
    }

    private void prepararCriacao() {
        when(gateway.usuarioAtivo(10)).thenReturn(true);
        when(gateway.condominioAtivo(1)).thenReturn(true);
        when(gateway.cooperativaAtiva(2)).thenReturn(true);
    }

    private void prepararStatus(StatusType status) {
        when(gateway.usuarioAtivo(10)).thenReturn(true);

        // Para o teste de solicitante, esse ID é ajustado abaixo.
        int solicitante = 99;

        var coleta = new Coleta(
                5, 1, 2, status,
                OffsetDateTime.now().plusDays(2),
                null, false, solicitante,
                "CONDOMINIO", 88, OffsetDateTime.now());

        when(gateway.buscarComBloqueio(5))
                .thenReturn(Optional.of(coleta));
    }

    private void verificarSemMudancaStatus() {
        verify(gateway, never())
                .atualizarStatus(anyInt(), any(), any());
    }
}
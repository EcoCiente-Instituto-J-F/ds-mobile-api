package br.com.ecociente.mobile.config.notificacoes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;

import org.junit.jupiter.api.Test;

class PushDispatchJobTest {
    @Test
    void agrupaNotificacoesPorPreferencia() {
        assertEquals("dicasSustentaveis", PushDispatchJob.grupo("dica"));
        assertEquals("dicasSustentaveis", PushDispatchJob.grupo("motivacional"));
        assertEquals("lembretesAvisos", PushDispatchJob.grupo("coleta_hoje"));
        assertEquals("lembretesAvisos", PushDispatchJob.grupo("mensagem_nova"));
        assertEquals("novasAtividades", PushDispatchJob.grupo("ranking_subiu"));
        assertEquals("novasAtividades", PushDispatchJob.grupo("ecoregistro_aprovado"));
    }
    @Test
    void descartaEntregaComPreferenciasIncompletasSemEnviarAoFirebase() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        FirebaseMessaging firebase = mock(FirebaseMessaging.class);
        br.com.ecociente.mobile.core.service.notificacoes.PushService push =
                mock(br.com.ecociente.mobile.core.service.notificacoes.PushService.class);
        String instalacao = "00000000-0000-4000-8000-000000000010";
        when(jdbc.queryForList(anyString())).thenReturn(List.of(Map.of(
                "id_entrega", 5L, "notificacao_id", 10, "instalacao_id", instalacao)));
        when(jdbc.queryForList(anyString(), eq(10), eq(instalacao))).thenReturn(List.of(Map.of(
                "usuario_id", 10, "tipo_notificacao", "quiz", "titulo_mensagem", "Quiz",
                "corpo_mensagem", "Nova tentativa", "token_fcm", "token-teste",
                "preferencias", "{}", "ativo", true)));
        new PushDispatchJob(jdbc, firebase, new ObjectMapper(), push, true).executar();
        verifyNoInteractions(firebase);
        verify(jdbc).update(org.mockito.ArgumentMatchers.contains("estado='DESCARTADO'"),
                eq("Preferência desabilitada"), eq(5L));
    }

    @Test
    void descartaEntregaQuandoDispositivoPertenceAOutroUsuario() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        FirebaseMessaging firebase = mock(FirebaseMessaging.class);
        br.com.ecociente.mobile.core.service.notificacoes.PushService push =
                mock(br.com.ecociente.mobile.core.service.notificacoes.PushService.class);
        String instalacao = "00000000-0000-4000-8000-000000000020";
        when(jdbc.queryForList(anyString())).thenReturn(List.of(Map.of(
                "id_entrega", 6L, "notificacao_id", 11, "instalacao_id", instalacao)));
        when(jdbc.queryForList(anyString(), eq(11), eq(instalacao))).thenReturn(List.of());
        new PushDispatchJob(jdbc, firebase, new ObjectMapper(), push, true).executar();
        verifyNoInteractions(firebase);
        verify(jdbc).update(org.mockito.ArgumentMatchers.contains("estado='DESCARTADO'"),
                eq("Dispositivo removido"), eq(6L));
    }

}

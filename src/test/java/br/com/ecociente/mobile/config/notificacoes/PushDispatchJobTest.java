package br.com.ecociente.mobile.config.notificacoes;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

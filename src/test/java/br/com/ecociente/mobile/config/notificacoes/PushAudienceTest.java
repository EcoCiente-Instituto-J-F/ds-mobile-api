package br.com.ecociente.mobile.config.notificacoes;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PushAudienceTest {
    @Test
    void desabilitadaPorPadraoNaoAutorizaNinguem() {
        PushAudience audience = new PushAudience("disabled", "");
        assertFalse(audience.permitir(63));
    }

    @Test
    void pilotoAutorizaSomenteUsuariosExplicitamenteListados() {
        PushAudience audience = new PushAudience("pilot", " 63, 71 ");
        assertTrue(audience.permitir(63));
        assertTrue(audience.permitir(71));
        assertFalse(audience.permitir(70));
        assertFalse(audience.permitir(-1));
    }

    @Test
    void modoAllExigeEscolhaExplicita() {
        PushAudience audience = new PushAudience("all", "");
        assertTrue(audience.permitir(63));
        assertFalse(audience.permitir(0));
    }

    @Test
    void recusaModoOuListaPilotoInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new PushAudience("ativado", "63"));
        assertThrows(IllegalArgumentException.class, () -> new PushAudience("pilot", ""));
        assertThrows(IllegalArgumentException.class, () -> new PushAudience("pilot", "63,x"));
        assertThrows(IllegalArgumentException.class, () -> new PushAudience("pilot", "63,0"));
    }
}

package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.mockito.Mockito;

class MensagemMotivacionalServiceTest {

    private final JdbcTemplate jdbc = Mockito.mock(JdbcTemplate.class);
    private final MensagemMotivacionalService service = new MensagemMotivacionalService(jdbc);

    @Test
    void retornaVazioSeNaoExistirMensagem() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);
        assertTrue(service.sortear(null).isEmpty());
    }

    @Test
    void buscaUmaMensagemSemRepetirIdAnterior() throws Exception {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(1L);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(new MensagemMotivacionalService.MensagemMotivacional(7L, "Cuide do planeta")));
        var resposta = service.sortear(5L).orElseThrow();
        assertEquals(7L, resposta.id());
        assertEquals("Cuide do planeta", resposta.mensagem());
    }

    @Test
    void permiteRepeticaoQuandoSoHaUmaMensagem() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L, 1L);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(new MensagemMotivacionalService.MensagemMotivacional(5L, "Recicle")));
        assertEquals(5L, service.sortear(5L).orElseThrow().id());
    }
}

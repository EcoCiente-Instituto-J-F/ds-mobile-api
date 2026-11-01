package br.com.ecociente.mobile.dataprovider.gateway.senha;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class TentativasRecuperacaoSenhaGatewayImplTest {

    private static final String EMAIL = "morador@teste.com";

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valores;

    private TentativasRecuperacaoSenhaGatewayImpl gateway;

    @BeforeEach
    void preparar() {
        when(redis.opsForValue()).thenReturn(valores);
        gateway = new TentativasRecuperacaoSenhaGatewayImpl(redis);
    }

    @Test
    void registrarFalhaCriaAChaveComPrazoAntesDeIncrementar() {
        when(valores.increment(anyString())).thenReturn(1L);

        int total = gateway.registrarFalha(EMAIL, Duration.ofMinutes(15));

        assertEquals(1, total);
        var ordem = inOrder(valores);
        ordem.verify(valores).setIfAbsent(anyString(), eq("0"), eq(Duration.ofMinutes(15)));
        ordem.verify(valores).increment(anyString());
    }

    @Test
    void aChaveNaoGuardaOEmailEmTextoPuro() {
        when(valores.increment(anyString())).thenReturn(1L);

        gateway.registrarFalha(EMAIL, Duration.ofMinutes(15));

        var chave = ArgumentCaptor.forClass(String.class);
        verify(valores).increment(chave.capture());
        assertTrue(chave.getValue().startsWith("senha:recuperacao:falhas:"));
        assertFalse(chave.getValue().contains("morador"));
    }

    @Test
    void falhasDevolveZeroSemChave() {
        when(valores.get(anyString())).thenReturn(null);

        assertEquals(0, gateway.falhas(EMAIL));
    }

    @Test
    void falhasLeOContador() {
        when(valores.get(anyString())).thenReturn("3");

        assertEquals(3, gateway.falhas(EMAIL));
    }
}

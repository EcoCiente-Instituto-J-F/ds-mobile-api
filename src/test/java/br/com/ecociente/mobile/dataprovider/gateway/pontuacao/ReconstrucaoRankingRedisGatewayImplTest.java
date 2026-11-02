package br.com.ecociente.mobile.dataprovider.gateway.pontuacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import br.com.ecociente.mobile.core.domain.pontuacao.SaldoRanking;

@ExtendWith(MockitoExtension.class)
class ReconstrucaoRankingRedisGatewayImplTest {

  private static final String CHAVE = "ranking:teste";

  private final OffsetDateTime expiraEm = OffsetDateTime.parse("2030-01-01T00:00:00Z");

  @Mock
  private StringRedisTemplate redis;

  @Mock
  private ZSetOperations<String, String> zset;

  private ReconstrucaoRankingRedisGatewayImpl gateway;

  @BeforeEach
  void preparar() {
    gateway = new ReconstrucaoRankingRedisGatewayImpl(redis);
  }

  @Test
  void deveExcluirRankingQuandoNaoHaParticipantes() {
    gateway.substituir(CHAVE, List.of(), expiraEm);

    verify(redis).delete(CHAVE);
    verify(redis, never()).opsForZSet();
    verify(redis, never()).rename(anyString(), anyString());
  }

  @Test
  void deveMontarTemporarioAntesDeSubstituirRanking() {
    when(redis.opsForZSet()).thenReturn(zset);
    when(zset.add(anyString(), anyString(), anyDouble()))
        .thenReturn(true);
    when(redis.expireAt(anyString(), eq(expiraEm.toInstant())))
        .thenReturn(true);

    gateway.substituir(
        CHAVE,
        List.of(
            new SaldoRanking(10, 30),
            new SaldoRanking(20, 50)),
        expiraEm);

    var captor = ArgumentCaptor.forClass(String.class);

    verify(redis).rename(captor.capture(), eq(CHAVE));

    String temporaria = captor.getValue();

    assertTrue(temporaria.startsWith(CHAVE + ":tmp:"));

    var ordem = inOrder(zset, redis);

    ordem.verify(zset).add(temporaria, "10", 30.0);
    ordem.verify(redis).expireAt(
        temporaria, expiraEm.toInstant());
    ordem.verify(zset).add(temporaria, "20", 50.0);
    ordem.verify(redis).rename(temporaria, CHAVE);
    ordem.verify(redis).delete(temporaria);

    verify(redis, never()).delete(CHAVE);
  }

  @Test
  void devePreservarRankingQuandoInclusaoFalha() {
    when(redis.opsForZSet()).thenReturn(zset);
    when(zset.add(anyString(), anyString(), anyDouble()))
        .thenReturn(false);

    assertThrows(
        IllegalStateException.class,
        () -> gateway.substituir(
            CHAVE,
            List.of(new SaldoRanking(10, 30)),
            expiraEm));

    verificarFalhaAntesDaSubstituicao();
  }

  @Test
  void devePreservarRankingQuandoExpiracaoFalha() {
    when(redis.opsForZSet()).thenReturn(zset);
    when(zset.add(anyString(), anyString(), anyDouble()))
        .thenReturn(true);
    when(redis.expireAt(anyString(), eq(expiraEm.toInstant())))
        .thenReturn(false);

    assertThrows(
        IllegalStateException.class,
        () -> gateway.substituir(
            CHAVE,
            List.of(new SaldoRanking(10, 30)),
            expiraEm));

    verificarFalhaAntesDaSubstituicao();
  }

  @Test
  void deveLimparTemporarioQuandoRenomeacaoFalha() {
    when(redis.opsForZSet()).thenReturn(zset);
    when(zset.add(anyString(), anyString(), anyDouble()))
        .thenReturn(true);
    when(redis.expireAt(anyString(), eq(expiraEm.toInstant())))
        .thenReturn(true);

    var falha = new IllegalStateException("Redis indisponível");

    doThrow(falha)
        .when(redis).rename(anyString(), eq(CHAVE));

    var recebida = assertThrows(
        IllegalStateException.class,
        () -> gateway.substituir(
            CHAVE,
            List.of(new SaldoRanking(10, 30)),
            expiraEm));

    assertSame(falha, recebida);

    var captor = ArgumentCaptor.forClass(String.class);
    verify(redis).rename(captor.capture(), eq(CHAVE));
    verify(redis).delete(captor.getValue());
    verify(redis, never()).delete(CHAVE);
  }

  @Test
  void devePreservarFalhaOriginalQuandoLimpezaTambemFalha() {
    when(redis.opsForZSet()).thenReturn(zset);

    var falhaOriginal = new IllegalStateException("Falha de escrita");
    var falhaLimpeza = new IllegalStateException("Falha de limpeza");

    when(zset.add(anyString(), anyString(), anyDouble()))
        .thenThrow(falhaOriginal);

    doThrow(falhaLimpeza).when(redis).delete(anyString());

    var recebida = assertThrows(
        IllegalStateException.class,
        () -> gateway.substituir(
            CHAVE,
            List.of(new SaldoRanking(10, 30)),
            expiraEm));

    assertSame(falhaOriginal, recebida);
    assertEquals(1, recebida.getSuppressed().length);
    assertSame(falhaLimpeza, recebida.getSuppressed()[0]);

    verify(redis, never()).rename(anyString(), anyString());
  }

  private void verificarFalhaAntesDaSubstituicao() {
    verify(redis, never()).rename(anyString(), anyString());
    verify(redis, never()).delete(CHAVE);

    var captor = ArgumentCaptor.forClass(String.class);
    verify(redis).delete(captor.capture());

    assertTrue(
        captor.getValue().startsWith(CHAVE + ":tmp:"));
  }
}
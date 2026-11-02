package br.com.ecociente.mobile.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import br.com.ecociente.mobile.core.domain.pontuacao.SaldoRanking;
import br.com.ecociente.mobile.dataprovider.gateway.pontuacao.ReconstrucaoRankingRedisGatewayImpl;

@Tag("integracao")
@Testcontainers
class ReconstrucaoRankingRedisIntegrationTest {

  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>(
      DockerImageName.parse("redis:7.4-alpine"))
      .withExposedPorts(6379);

  private static LettuceConnectionFactory connectionFactory;
  private static StringRedisTemplate redis;

  private ReconstrucaoRankingRedisGatewayImpl gateway;
  private String chave;

  @BeforeAll
  static void conectar() {
    var configuracao = new RedisStandaloneConfiguration(
        REDIS.getHost(),
        REDIS.getMappedPort(6379));

    connectionFactory = new LettuceConnectionFactory(configuracao);
    connectionFactory.afterPropertiesSet();
    connectionFactory.start();

    redis = new StringRedisTemplate(connectionFactory);
    redis.afterPropertiesSet();
  }

  @AfterAll
  static void desconectar() {
    if (connectionFactory != null) {
      connectionFactory.destroy();
    }
  }

  @BeforeEach
  void preparar() {
    gateway = new ReconstrucaoRankingRedisGatewayImpl(redis);
    chave = "teste:ranking:" + UUID.randomUUID();
  }

  @Test
  void deveSubstituirParticipantesESaldosAnteriores() {
    redis.opsForZSet().add(chave, "999", 1000);
    redis.opsForZSet().add(chave, "10", 5);

    gateway.substituir(
        chave,
        List.of(
            new SaldoRanking(10, 30),
            new SaldoRanking(20, 50)),
        OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));

    assertEquals(
        Long.valueOf(2),
        redis.opsForZSet().zCard(chave));

    assertEquals(
        Double.valueOf(30),
        redis.opsForZSet().score(chave, "10"));

    assertEquals(
        Double.valueOf(50),
        redis.opsForZSet().score(chave, "20"));

    assertNull(redis.opsForZSet().score(chave, "999"));

    var participantes = redis.opsForZSet()
        .reverseRange(chave, 0, -1);

    assertNotNull(participantes);
    assertEquals(List.of("20", "10"), List.copyOf(participantes));

    verificarSemTemporarias();
  }

  @Test
  void deveAplicarExpiracaoAoRankingPublicado() {
    var expiraEm = OffsetDateTime.now(ZoneOffset.UTC)
        .plusHours(1);

    gateway.substituir(
        chave,
        List.of(new SaldoRanking(10, 30)),
        expiraEm);

    Long ttl = redis.getExpire(chave, TimeUnit.MILLISECONDS);

    assertNotNull(ttl);
    assertTrue(ttl > 0);

    long restanteEsperado = Duration.between(
        OffsetDateTime.now(ZoneOffset.UTC),
        expiraEm).toMillis();

    assertTrue(
        Math.abs(ttl - restanteEsperado) < 5000,
        "A expiração deve acompanhar a data informada");

    verificarSemTemporarias();
  }

  @Test
  void deveRemoverRankingQuandoListaEstiverVazia() {
    redis.opsForZSet().add(chave, "10", 30);

    gateway.substituir(
        chave,
        List.of(),
        OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));

    assertFalse(Boolean.TRUE.equals(redis.hasKey(chave)));
    verificarSemTemporarias();
  }

  @Test
  void devePermitirReconstruirMesmoRankingNovamente() {
    var expiraEm = OffsetDateTime.now(ZoneOffset.UTC)
        .plusHours(1);

    var saldos = List.of(
        new SaldoRanking(10, 30),
        new SaldoRanking(20, 50));

    gateway.substituir(chave, saldos, expiraEm);
    gateway.substituir(chave, saldos, expiraEm);

    assertEquals(
        Long.valueOf(2),
        redis.opsForZSet().zCard(chave));

    assertEquals(
        Double.valueOf(30),
        redis.opsForZSet().score(chave, "10"));

    assertEquals(
        Double.valueOf(50),
        redis.opsForZSet().score(chave, "20"));

    verificarSemTemporarias();
  }

  private void verificarSemTemporarias() {
    // KEYS é usado somente neste Redis isolado de teste.
    var temporarias = redis.keys(chave + ":tmp:*");

    assertNotNull(temporarias);
    assertTrue(
        temporarias.isEmpty(),
        "Não devem sobrar chaves temporárias");
  }
}
package br.com.ecociente.mobile.core.service.pontuacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ecociente.mobile.core.domain.pontuacao.CicloRanking;
import br.com.ecociente.mobile.core.domain.pontuacao.EscopoReconstrucaoType;
import br.com.ecociente.mobile.core.domain.pontuacao.SaldoRanking;
import br.com.ecociente.mobile.core.domain.pontuacao.TipoRankingType;
import br.com.ecociente.mobile.core.exception.pontuacao.BusinessException;
import br.com.ecociente.mobile.core.exception.pontuacao.InconsistenciaPontuacaoException;
import br.com.ecociente.mobile.core.gateway.pontuacao.ReconstrucaoRankingGateway;
import br.com.ecociente.mobile.core.gateway.pontuacao.ReconstrucaoRankingRedisGateway;

@ExtendWith(MockitoExtension.class)
class ReconstrucaoRankingServiceTest {

  private static final Integer CONDOMINIO_ID = 1;

  private final OffsetDateTime inicio = OffsetDateTime.parse("2026-10-01T00:00:00Z");

  private final OffsetDateTime fim = OffsetDateTime.parse("2026-11-01T00:00:00Z");

  @Mock
  private ReconstrucaoRankingGateway gateway;

  @Mock
  private ReconstrucaoRankingRedisGateway redisGateway;

  @Mock
  private CycleService cycleService;

  @Mock
  private CicloRanking ciclo;

  private ReconstrucaoRankingService service;

  @BeforeEach
  void preparar() {
    Clock clock = Clock.fixed(
        OffsetDateTime.parse("2026-10-15T12:00:00Z").toInstant(),
        ZoneOffset.UTC);

    service = new ReconstrucaoRankingService(
        gateway,
        redisGateway,
        cycleService,
        clock,
        90);
  }

  @Test
  void deveRecusarReconstrucaoQuandoBloqueioEstiverOcupado() {
    when(gateway.tentarBloquearSincronizacao())
        .thenReturn(false);

    assertThrows(
        BusinessException.class,
        () -> service.reconstruir(
            EscopoReconstrucaoType.TODOS,
            CONDOMINIO_ID));

    verifyNoInteractions(cycleService, redisGateway);
  }

  @Test
  void devePublicarSomenteSaldosPositivos() {
    prepararCiclo(TipoRankingType.MORADORES, "ranking:moradores");

    var positivo = new SaldoRanking(10, 30);

    when(gateway.agruparMoradoresNoCiclo(
        CONDOMINIO_ID, inicio, fim))
        .thenReturn(List.of(
            positivo,
            new SaldoRanking(20, 0)));

    var resultado = service.reconstruir(
        EscopoReconstrucaoType.MORADORES,
        CONDOMINIO_ID);

    assertEquals(1, resultado.participantes());
    assertEquals("MORADORES", resultado.tipo());
    assertEquals(CONDOMINIO_ID, resultado.condominioId());

    verify(redisGateway).substituir(
        "ranking:moradores",
        List.of(positivo),
        fim.plusDays(90));

    verify(gateway, never()).agruparTorresNoCiclo(
        anyInt(), any(), any());
  }

  @Test
  void deveRecusarSaldoNegativoSemPublicarRanking() {
    prepararCicloSemChave(TipoRankingType.MORADORES);

    when(gateway.agruparMoradoresNoCiclo(
        CONDOMINIO_ID, inicio, fim))
        .thenReturn(List.of(
            new SaldoRanking(10, 30),
            new SaldoRanking(20, -5)));

    assertThrows(
        InconsistenciaPontuacaoException.class,
        () -> service.reconstruir(
            EscopoReconstrucaoType.MORADORES,
            CONDOMINIO_ID));

    verifyNoInteractions(redisGateway);
  }

  @Test
  void devePublicarListaVaziaParaRemoverRankingSemSaldo() {
    prepararCiclo(TipoRankingType.TORRES, "ranking:torres");

    when(gateway.agruparTorresNoCiclo(
        CONDOMINIO_ID, inicio, fim))
        .thenReturn(List.of(new SaldoRanking(5, 0)));

    var resultado = service.reconstruir(
        EscopoReconstrucaoType.TORRES,
        CONDOMINIO_ID);

    assertEquals(0, resultado.participantes());

    verify(redisGateway).substituir(
        "ranking:torres",
        List.of(),
        fim.plusDays(90));
  }

  @Test
  void deveReconstruirMoradoresETorresQuandoEscopoForTodos() {
    prepararCiclo(TipoRankingType.MORADORES, "ranking:moradores");

    when(cycleService.atual(TipoRankingType.TORRES))
        .thenReturn(ciclo);

    when(cycleService.montarChave(
        TipoRankingType.TORRES, CONDOMINIO_ID, ciclo))
        .thenReturn("ranking:torres");

    var moradores = List.of(new SaldoRanking(10, 30));
    var torres = List.of(new SaldoRanking(5, 50));

    when(gateway.agruparMoradoresNoCiclo(
        CONDOMINIO_ID, inicio, fim))
        .thenReturn(moradores);

    when(gateway.agruparTorresNoCiclo(
        CONDOMINIO_ID, inicio, fim))
        .thenReturn(torres);

    var resultado = service.reconstruir(
        EscopoReconstrucaoType.TODOS,
        CONDOMINIO_ID);

    assertEquals(2, resultado.participantes());

    verify(redisGateway).substituir(
        "ranking:moradores", moradores, fim.plusDays(90));

    verify(redisGateway).substituir(
        "ranking:torres", torres, fim.plusDays(90));
  }

  @Test
  void deveRecusarCicloForaDaRetencao() {
    when(gateway.tentarBloquearSincronizacao()).thenReturn(true);

    when(cycleService.atual(TipoRankingType.MORADORES))
        .thenReturn(ciclo);

    when(ciclo.getFim()).thenReturn(
        OffsetDateTime.parse("2026-01-01T00:00:00Z"));

    assertThrows(
        IllegalStateException.class,
        () -> service.reconstruir(
            EscopoReconstrucaoType.MORADORES,
            CONDOMINIO_ID));

    verifyNoInteractions(redisGateway);

    verify(gateway, never()).agruparMoradoresNoCiclo(
        anyInt(), any(), any());
  }

  private void prepararCicloSemChave(TipoRankingType tipo) {
    when(gateway.tentarBloquearSincronizacao()).thenReturn(true);
    when(cycleService.atual(tipo)).thenReturn(ciclo);
    when(ciclo.getInicio()).thenReturn(inicio);
    when(ciclo.getFim()).thenReturn(fim);
  }

  private void prepararCiclo(
      TipoRankingType tipo,
      String chave) {

    prepararCicloSemChave(tipo);

    when(cycleService.montarChave(
        tipo, CONDOMINIO_ID, ciclo))
        .thenReturn(chave);
  }
}
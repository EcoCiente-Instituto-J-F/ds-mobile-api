package br.com.ecociente.mobile.core.service.pontuacao;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.pontuacao.CicloRanking;
import br.com.ecociente.mobile.core.domain.pontuacao.EscopoReconstrucaoType;
import br.com.ecociente.mobile.core.domain.pontuacao.SaldoRanking;
import br.com.ecociente.mobile.core.domain.pontuacao.TipoRankingType;
import br.com.ecociente.mobile.core.exception.pontuacao.BusinessException;
import br.com.ecociente.mobile.core.exception.pontuacao.InconsistenciaPontuacaoException;
import br.com.ecociente.mobile.core.gateway.pontuacao.ReconstrucaoRankingGateway;
import br.com.ecociente.mobile.core.gateway.pontuacao.ReconstrucaoRankingRedisGateway;

@Service
public class ReconstrucaoRankingService {

    private final ReconstrucaoRankingGateway gateway;
    private final ReconstrucaoRankingRedisGateway redisGateway;
    private final CycleService cycleService;
    private final Clock clock;
    private final int retentionDays;

    public ReconstrucaoRankingService(
            ReconstrucaoRankingGateway gateway,
            ReconstrucaoRankingRedisGateway redisGateway,
            CycleService cycleService,
            Clock clock,
            @Value("${app.ranking.retention-days:90}") int retentionDays) {

        this.gateway = gateway;
        this.redisGateway = redisGateway;
        this.cycleService = cycleService;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResultadoReconstrucao reconstruir(
            EscopoReconstrucaoType tipo,
            Integer condominioId) {

        if (!gateway.tentarBloquearSincronizacao()) {
            throw new BusinessException(
                    "RANKING_EM_PROCESSAMENTO",
                    "Há uma sincronização ou reconstrução em andamento. Tente novamente");
        }

        int participantes = 0;

        if (tipo == EscopoReconstrucaoType.MORADORES
                || tipo == EscopoReconstrucaoType.TODOS) {
            participantes += reconstruirTipo(
                    TipoRankingType.MORADORES,
                    condominioId);
        }

        if (tipo == EscopoReconstrucaoType.TORRES
                || tipo == EscopoReconstrucaoType.TODOS) {
            participantes += reconstruirTipo(
                    TipoRankingType.TORRES,
                    condominioId);
        }

        return new ResultadoReconstrucao(
                tipo.name(),
                condominioId,
                participantes);
    }

    private int reconstruirTipo(
            TipoRankingType tipo,
            Integer condominioId) {

        CicloRanking ciclo = cycleService.atual(tipo);
        OffsetDateTime expiraEm = ciclo.getFim().plusDays(retentionDays);

        if (!expiraEm.isAfter(OffsetDateTime.now(clock))) {
            throw new IllegalStateException(
                    "O ciclo já ultrapassou a retenção");
        }

        List<SaldoRanking> saldos =
                tipo == TipoRankingType.MORADORES
                        ? gateway.agruparMoradoresNoCiclo(
                                condominioId,
                                ciclo.getInicio(),
                                ciclo.getFim())
                        : gateway.agruparTorresNoCiclo(
                                condominioId,
                                ciclo.getInicio(),
                                ciclo.getFim());

        for (SaldoRanking saldo : saldos) {
            if (saldo.pontos() < 0) {
                throw new InconsistenciaPontuacaoException(
                        "INCONSISTENCIA_MOVIMENTACAO",
                        "O ranking possui saldo negativo");
            }
        }

        List<SaldoRanking> positivos = saldos.stream()
                .filter(saldo -> saldo.pontos() > 0)
                .toList();

        String chave = cycleService.montarChave(
                tipo,
                condominioId,
                ciclo);

        redisGateway.substituir(chave, positivos, expiraEm);

        return positivos.size();
    }

    public record ResultadoReconstrucao(
            String tipo,
            Integer condominioId,
            int participantes) {
    }
}
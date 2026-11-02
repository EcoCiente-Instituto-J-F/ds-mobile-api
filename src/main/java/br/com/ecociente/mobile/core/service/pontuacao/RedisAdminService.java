package br.com.ecociente.mobile.core.service.pontuacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import br.com.ecociente.mobile.core.gateway.pontuacao.DiagnosticoRedisGateway;
import org.springframework.stereotype.Service;

import br.com.ecociente.mobile.core.gateway.pontuacao.SincronizacaoPontuacaoGateway;

@Service
public class RedisAdminService {

    private static final Logger log =
        LoggerFactory.getLogger(RedisAdminService.class);

    private final SincronizacaoPontuacaoGateway gateway;
    private final SincronizacaoPontuacaoService service;
    private final DiagnosticoRedisGateway diagnosticoRedisGateway;
    private final int batchSize;

    public RedisAdminService(
    SincronizacaoPontuacaoGateway gateway,
    SincronizacaoPontuacaoService service,
    DiagnosticoRedisGateway diagnosticoRedisGateway,
    @Value("${app.ranking.sync-batch-size:100}") int batchSize
) {
    if (batchSize < 1 || batchSize > 1000) {
        throw new IllegalArgumentException(
            "O lote de sincronização deve estar entre 1 e 1000"
        );
    }

    this.gateway = gateway;
    this.service = service;
    this.diagnosticoRedisGateway = diagnosticoRedisGateway;
    this.batchSize = batchSize;
}

    public ResultadoSincronizacao sincronizarPendentes() {
        var pendentes = gateway.buscarIdsPendentes(batchSize);

        int sincronizadas = 0;
        int falhas = 0;

        for (Long movimentacaoId : pendentes) {
            try {
                if (service.sincronizar(movimentacaoId)) {
                    sincronizadas++;
                }
            } catch (RuntimeException exception) {
                falhas++;

                log.error(
                    "Falha na sincronização manual movimentacaoId={}",
                    movimentacaoId,
                    exception
                );

                registrarFalha(movimentacaoId);
            }
        }

        return new ResultadoSincronizacao(
            pendentes.size(),
            sincronizadas,
            falhas,
            gateway.contarPendentes()
        );
    }

    public DiagnosticoRedis consultarStatus() {
        return new DiagnosticoRedis(
                diagnosticoRedisGateway.estaDisponivel(),
                gateway.contarPendentes());
    }


    private void registrarFalha(Long movimentacaoId) {
        try {
            gateway.registrarFalha(movimentacaoId);
        } catch (RuntimeException exception) {
            log.error(
                "Falha ao registrar tentativa movimentacaoId={}",
                movimentacaoId,
                exception
            );
        }
    }

    public record ResultadoSincronizacao(
        int processadas,
        int sincronizadas,
        int falhas,
        Long pendentesRestantes
    ) {
    }

    public record DiagnosticoRedis(
        boolean redisDisponivel,
        Long movimentacoesPendentes
    ) {
    }
}
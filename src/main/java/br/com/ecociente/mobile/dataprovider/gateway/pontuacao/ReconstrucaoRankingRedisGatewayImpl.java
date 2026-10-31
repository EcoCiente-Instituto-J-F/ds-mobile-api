package br.com.ecociente.mobile.dataprovider.gateway.pontuacao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.pontuacao.SaldoRanking;
import br.com.ecociente.mobile.core.gateway.pontuacao.ReconstrucaoRankingRedisGateway;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReconstrucaoRankingRedisGatewayImpl
    implements ReconstrucaoRankingRedisGateway {

  private final StringRedisTemplate redisTemplate;

  @Override
  public void substituir(
      String chave,
      List<SaldoRanking> saldos,
      OffsetDateTime expiraEm) {

    if (saldos.isEmpty()) {
      redisTemplate.delete(chave);
      return;
    }

    String temporaria = chave + ":tmp:" + UUID.randomUUID();
    RuntimeException falhaPrincipal = null;

    try {
      boolean primeiraInclusao = true;

      for (SaldoRanking saldo : saldos) {
        Boolean adicionado = redisTemplate.opsForZSet().add(
            temporaria,
            saldo.participanteId().toString(),
            saldo.pontos());

        if (!Boolean.TRUE.equals(adicionado)) {
          throw new IllegalStateException(
              "Não foi possível montar o ranking temporário");
        }

        if (primeiraInclusao) {
          Boolean expiracaoDefinida = redisTemplate.expireAt(
              temporaria,
              expiraEm.toInstant());

          if (!Boolean.TRUE.equals(expiracaoDefinida)) {
            throw new IllegalStateException(
                "Não foi possível definir a retenção do ranking");
          }

          primeiraInclusao = false;
        }
      }

      redisTemplate.rename(temporaria, chave);

    } catch (RuntimeException exception) {
      falhaPrincipal = exception;
      throw exception;

    } finally {
      try {
        redisTemplate.delete(temporaria);
      } catch (RuntimeException limpezaException) {
        if (falhaPrincipal != null) {
          falhaPrincipal.addSuppressed(limpezaException);
        } else {
          throw limpezaException;
        }
      }
    }
  }
}
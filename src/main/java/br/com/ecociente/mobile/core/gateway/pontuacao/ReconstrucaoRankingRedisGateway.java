package br.com.ecociente.mobile.core.gateway.pontuacao;

import java.time.OffsetDateTime;
import java.util.List;

import br.com.ecociente.mobile.core.domain.pontuacao.SaldoRanking;

public interface ReconstrucaoRankingRedisGateway {

  void substituir(
      String chave,
      List<SaldoRanking> saldos,
      OffsetDateTime expiraEm);
}
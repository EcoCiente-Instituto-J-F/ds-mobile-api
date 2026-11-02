package br.com.ecociente.mobile.core.gateway.pontuacao;

import java.time.OffsetDateTime;
import java.util.List;

import br.com.ecociente.mobile.core.domain.pontuacao.SaldoRanking;

public interface ReconstrucaoRankingGateway {

    boolean tentarBloquearSincronizacao();

    List<SaldoRanking> agruparMoradoresNoCiclo(
            Integer condominioId,
            OffsetDateTime inicio,
            OffsetDateTime fim);

    List<SaldoRanking> agruparTorresNoCiclo(
            Integer condominioId,
            OffsetDateTime inicio,
            OffsetDateTime fim);
}
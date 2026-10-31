package br.com.ecociente.mobile.dataprovider.gateway.pontuacao;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.pontuacao.SaldoRanking;
import br.com.ecociente.mobile.core.gateway.pontuacao.ReconstrucaoRankingGateway;
import br.com.ecociente.mobile.dataprovider.repository.MovimentacaoPontosRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ReconstrucaoRankingGatewayImpl
        implements ReconstrucaoRankingGateway {

    private final MovimentacaoPontosRepository repository;

    @Override
    public boolean tentarBloquearSincronizacao() {
        return repository.tentarBloquearSincronizacao();
    }

    @Override
    public List<SaldoRanking> agruparMoradoresNoCiclo(
            Integer condominioId,
            OffsetDateTime inicio,
            OffsetDateTime fim) {

        return converter(repository.agruparMoradoresNoCiclo(
                condominioId,
                inicio,
                fim));
    }

    @Override
    public List<SaldoRanking> agruparTorresNoCiclo(
            Integer condominioId,
            OffsetDateTime inicio,
            OffsetDateTime fim) {

        return converter(repository.agruparTorresNoCiclo(
                condominioId,
                inicio,
                fim));
    }

    private List<SaldoRanking> converter(List<Object[]> linhas) {
        return linhas.stream()
                .map(linha -> new SaldoRanking(
                        ((Number) linha[0]).intValue(),
                        ((Number) linha[1]).longValue()))
                .toList();
    }
}
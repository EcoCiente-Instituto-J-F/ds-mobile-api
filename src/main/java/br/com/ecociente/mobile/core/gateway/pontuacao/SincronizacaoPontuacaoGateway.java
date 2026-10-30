package br.com.ecociente.mobile.core.gateway.pontuacao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import br.com.ecociente.mobile.core.domain.pontuacao.CicloRanking;
import br.com.ecociente.mobile.core.domain.pontuacao.MovimentacaoPontos;

public interface SincronizacaoPontuacaoGateway {

        List<Long> buscarIdsPendentes(int limite);

        boolean tentarBloquearSincronizacao();

        Optional<MovimentacaoPontos> buscarPorId(Long movimentacaoId);

        Long calcularSaldoMorador(
                        Integer usuarioId,
                        Integer condominioId,
                        CicloRanking ciclo);

        Long calcularSaldoTorre(
                        Integer torreId,
                        Integer condominioId,
                        CicloRanking ciclo);

        void marcarSincronizada(
                        Long movimentacaoId,
                        OffsetDateTime sincronizadoEm);

        void registrarFalha(Long movimentacaoId);

        Long contarPendentes();
}
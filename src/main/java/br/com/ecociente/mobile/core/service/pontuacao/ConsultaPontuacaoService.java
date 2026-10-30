package br.com.ecociente.mobile.core.service.pontuacao;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.pontuacao.MovimentacaoPontos;
import br.com.ecociente.mobile.core.domain.pontuacao.Pagina;
import br.com.ecociente.mobile.core.exception.pontuacao.InconsistenciaPontuacaoException;
import br.com.ecociente.mobile.core.gateway.pontuacao.MovimentacaoPontosGateway;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaPontuacaoService {

  private final MovimentacaoPontosGateway movimentacaoGateway;

  public Long consultarSaldo(Integer usuarioId) {
    Long saldo = movimentacaoGateway.calcularSaldoUsuario(usuarioId);

    if (saldo < 0) {
      throw new InconsistenciaPontuacaoException(
          "INCONSISTENCIA_MOVIMENTACAO",
          "O histórico do usuário possui saldo negativo");
    }

    return saldo;
  }

  public Pagina<MovimentacaoPontos> listarMovimentacoes(
      Integer usuarioId,
      int pagina,
      int tamanho) {
    return movimentacaoGateway.listarPorUsuario(
        usuarioId,
        pagina,
        tamanho);
  }
}
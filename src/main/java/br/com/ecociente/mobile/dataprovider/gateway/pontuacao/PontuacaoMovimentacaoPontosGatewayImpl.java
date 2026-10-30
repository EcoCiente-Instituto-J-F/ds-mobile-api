package br.com.ecociente.mobile.dataprovider.gateway.pontuacao;

import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.pontuacao.MovimentacaoPontos;
import br.com.ecociente.mobile.core.domain.pontuacao.Pagina;
import br.com.ecociente.mobile.core.domain.pontuacao.TipoMovimentacaoType;
import br.com.ecociente.mobile.core.gateway.pontuacao.MovimentacaoPontosGateway;
import br.com.ecociente.mobile.dataprovider.mapper.pontuacao.PontuacaoMovimentacaoPontosMapper;
import br.com.ecociente.mobile.dataprovider.repository.MovimentacaoPontosRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PontuacaoMovimentacaoPontosGatewayImpl
    implements MovimentacaoPontosGateway {

  private final MovimentacaoPontosRepository repository;
  private final PontuacaoMovimentacaoPontosMapper mapper;

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public MovimentacaoPontos criar(MovimentacaoPontos movimentacao) {
    if (movimentacao.getId() != null) {
      throw new IllegalArgumentException(
          "Uma nova movimentação não deve possuir ID");
    }

    var entity = mapper.toEntity(movimentacao);
    var salva = repository.saveAndFlush(entity);

    return mapper.toDomain(salva);
  }

  @Override
  public Optional<MovimentacaoPontos> buscarPorIdempotencyKey(
      String idempotencyKey) {
    return repository.findByIdempotencyKey(idempotencyKey)
        .map(mapper::toDomain);
  }

  @Override
  public Optional<MovimentacaoPontos> buscarCreditoPostagem(
      Integer postagemId) {
    return repository.findByPostagemIdAndTipoMovimentacao(
        postagemId,
        TipoMovimentacaoType.CREDITO)
        .map(mapper::toDomain);
  }

  @Override
  public Optional<MovimentacaoPontos> buscarCreditoTentativa(
      Integer tentativaId) {
    return repository.findByTentativaQuizIdAndTipoMovimentacao(
        tentativaId,
        TipoMovimentacaoType.CREDITO)
        .map(mapper::toDomain);
  }

  @Override
  public Optional<MovimentacaoPontos> buscarUltimaMovimentacaoPostagem(
      Integer postagemId) {
    return repository.findFirstByPostagemIdOrderByIdDesc(postagemId)
        .map(mapper::toDomain);
  }

  @Override
  public Long calcularSaldoUsuario(Integer usuarioId) {
    return repository.calcularSaldoUsuario(usuarioId);
  }

  @Override
  public Long calcularSaldoPostagem(Integer postagemId) {
    return repository.calcularSaldoPostagem(postagemId);
  }

  @Override
  public Pagina<MovimentacaoPontos> listarPorUsuario(
      Integer usuarioId,
      int pagina,
      int tamanho) {
    if (pagina < 0 || tamanho < 1 || tamanho > 100) {
      throw new IllegalArgumentException(
          "A página deve ser >= 0 e o tamanho deve estar entre 1 e 100");
    }

    var ordenacao = Sort.by(
        Sort.Order.desc("ocorridoEm"),
        Sort.Order.desc("id"));

    var pageable = PageRequest.of(pagina, tamanho, ordenacao);

    var resultado = repository.findByUsuarioId(usuarioId, pageable);

    var conteudo = resultado.getContent()
        .stream()
        .map(mapper::toDomain)
        .toList();

    return new Pagina<>(
        conteudo,
        resultado.getNumber(),
        resultado.getSize(),
        resultado.getTotalElements(),
        resultado.getTotalPages());
  }
}
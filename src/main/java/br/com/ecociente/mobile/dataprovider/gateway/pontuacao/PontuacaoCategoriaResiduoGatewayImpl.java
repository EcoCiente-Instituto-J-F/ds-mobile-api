package br.com.ecociente.mobile.dataprovider.gateway.pontuacao;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.pontuacao.CategoriaResiduo;
import br.com.ecociente.mobile.core.gateway.pontuacao.CategoriaResiduoGateway;
import br.com.ecociente.mobile.dataprovider.mapper.pontuacao.PontuacaoCategoriaResiduoMapper;
import br.com.ecociente.mobile.dataprovider.repository.CategoriaResiduoRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PontuacaoCategoriaResiduoGatewayImpl implements CategoriaResiduoGateway {

  private final CategoriaResiduoRepository repository;
  private final PontuacaoCategoriaResiduoMapper mapper;

  @Override
  public Optional<CategoriaResiduo> buscarPorId(Integer categoriaId) {
    return repository.findById(categoriaId)
        .map(mapper::toDomain);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Integer calcularPontosDisponiveis(
      Integer usuarioId,
      Integer categoriaId,
      OffsetDateTime dataReferencia,
      String timezone) {
    return repository.calcularPontosDisponiveis(
        usuarioId,
        categoriaId,
        dataReferencia,
        timezone);
  }
}
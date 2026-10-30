package br.com.ecociente.mobile.dataprovider.gateway.pontuacao;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.pontuacao.Postagem;
import br.com.ecociente.mobile.core.gateway.pontuacao.PostagemGateway;
import br.com.ecociente.mobile.dataprovider.mapper.pontuacao.PontuacaoPostagemMapper;
import br.com.ecociente.mobile.dataprovider.repository.PostagemRepository;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PontuacaoPostagemGatewayImpl implements PostagemGateway {

  private final PostagemRepository repository;
  private final PontuacaoPostagemMapper mapper;
  private final EntityManager entityManager;

  @Override
  public Optional<Integer> buscarUsuarioId(Integer postagemId) {
    return repository.buscarUsuarioId(postagemId);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public boolean bloquearUsuario(Integer usuarioId) {
    return repository.bloquearUsuario(usuarioId).isPresent();
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<Postagem> buscarComBloqueio(Integer postagemId) {
    return repository.buscarComBloqueio(postagemId)
        .map(entity -> {
          entityManager.refresh(entity);
          return mapper.toDomain(entity);
        });
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void marcarReconciliacaoConcluida(Integer postagemId) {
    var entity = repository.buscarComBloqueio(postagemId)
        .orElseThrow(() -> new IllegalStateException(
            "Postagem não encontrada ao concluir reconciliação: "
                + postagemId));

    entity.setPontuacaoReconciliacaoPendente(false);

    repository.saveAndFlush(entity);
  }

  @Override
  public Optional<Integer> buscarCondominioId(
      Integer postagemId) {

    return repository.buscarCondominioId(postagemId);
  }
}
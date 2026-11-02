package br.com.ecociente.mobile.core.service.validador;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.validador.ResultadoVoto;
import br.com.ecociente.mobile.core.exception.validador.NotFoundException;
import br.com.ecociente.mobile.core.exception.validador.PermissaoNegadaException;
import br.com.ecociente.mobile.core.service.pontuacao.PostagemValidacaoService;
import br.com.ecociente.mobile.core.gateway.validador.PostagemValidacaoGateway;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ValidadorPostagemValidacaoService {

  private final PostagemValidacaoGateway gateway;
  private final PostagemValidacaoService pontuacao;

  @Transactional
  public ResultadoVoto votar(
      int postagemId,
      int usuarioId,
      String tipo,
      Integer motivoDenunciaId,
      String comentario) {

    int autor = gateway.buscarUsuarioAutor(postagemId)
        .orElseThrow(() -> new NotFoundException("Postagem não encontrada."));

    if (autor == usuarioId) {
      throw new PermissaoNegadaException(
          "O autor não pode votar na própria postagem.");
    }

    int autorBloqueado = pontuacao.prepararVoto(postagemId);

    if (autorBloqueado == usuarioId) {
      throw new PermissaoNegadaException(
          "O autor não pode votar na própria postagem.");
    }

    var resultado = gateway.registrarVoto(
        postagemId,
        usuarioId,
        tipo,
        motivoDenunciaId,
        comentario);

    pontuacao.reconciliarVoto(postagemId);

    return resultado;
  }

  @Transactional
  public void decidir(
      int postagemId,
      int usuarioId,
      boolean aprovar) {

    if (!gateway.existePostagem(postagemId)) {
      throw new NotFoundException("Postagem não encontrada.");
    }

    if (!Objects.equals(
        gateway.buscarUsuarioSindico(postagemId),
        usuarioId)) {
      throw new PermissaoNegadaException(
          "Só o síndico do condomínio decide postagens em análise.");
    }

    pontuacao.decidir(postagemId, aprovar);
  }

  @Transactional
  public void encerrarJanela(int postagemId) {
    pontuacao.encerrar(postagemId);
  }
}
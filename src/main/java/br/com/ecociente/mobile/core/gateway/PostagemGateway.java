package br.com.ecociente.mobile.core.gateway;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import br.com.ecociente.mobile.core.domain.CategoriaResiduo;
import br.com.ecociente.mobile.core.domain.Postagem;
import br.com.ecociente.mobile.core.domain.educacao.ContextoCondominialQuiz;

public interface PostagemGateway {

  Optional<ContextoCondominialQuiz> buscarVinculoAprovado(
      Integer usuarioId,
      Integer condominioId);

  boolean categoriaExiste(Integer categoriaId);

  List<CategoriaResiduo> listarCategorias();

  boolean hashFotoJaUsado(String hashFoto);

  Integer inserir(
      Integer usuarioId,
      Integer condominioId,
      Integer torreId,
      Integer categoriaId,
      String urlFoto,
      String hashFoto,
      OffsetDateTime capturadaEm);

  Optional<Postagem> buscarPorId(Integer postagemId);

  long contarDoCondominio(Integer condominioId);

  List<Postagem> listarDoCondominio(
      Integer condominioId,
      long deslocamento,
      int limite);

  long contarDoUsuario(Integer usuarioId);

  List<Postagem> listarDoUsuario(
      Integer usuarioId,
      long deslocamento,
      int limite);
}

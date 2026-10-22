package br.com.ecociente.mobile.core.gateway;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import br.com.ecociente.mobile.core.domain.CategoriaResiduo;
import br.com.ecociente.mobile.core.domain.ContextoCondominialQuiz;
import br.com.ecociente.mobile.core.domain.Postagem;

public interface PostagemGateway {

  /** Vínculo ativo e aprovado do usuário com o condomínio (com a torre dele, se houver). */
  Optional<ContextoCondominialQuiz> buscarVinculoAprovado(
      Integer usuarioId,
      Integer condominioId);

  boolean categoriaExiste(Integer categoriaId);

  /** Categorias que o morador pode escolher ao registrar um descarte. */
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

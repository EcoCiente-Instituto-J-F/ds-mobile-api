package br.com.ecociente.mobile.core.service;

import java.time.OffsetDateTime;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.Postagem;
import br.com.ecociente.mobile.core.exception.PostagemException;
import br.com.ecociente.mobile.core.exception.PostagemException.Tipo;
import br.com.ecociente.mobile.core.gateway.PostagemGateway;

/**
 * Registra a foto de descarte do morador. Os pontos só saem depois, quando a
 * ds-pontuacao-api fecha a validação da postagem.
 */
@Service
public class CriarPostagemService {

  private final PostagemGateway gateway;

  public CriarPostagemService(PostagemGateway gateway) {
    this.gateway = gateway;
  }

  @Transactional
  public Postagem executar(
      Integer usuarioId,
      Integer condominioId,
      Integer categoriaId,
      String urlFoto,
      String hashFoto,
      OffsetDateTime capturadaEm) {
    if (usuarioId == null || usuarioId <= 0) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "USUARIO_INVALIDO",
          "Usuário inválido");
    }

    if (condominioId == null || condominioId <= 0) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "CONDOMINIO_ID_INVALIDO",
          "Informe um condomínio válido");
    }

    if (categoriaId == null || categoriaId <= 0) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "CATEGORIA_ID_INVALIDA",
          "Informe uma categoria válida");
    }

    if (urlFoto == null
        || urlFoto.isBlank()
        || urlFoto.length() > 500
        || !(urlFoto.startsWith("https://") || urlFoto.startsWith("http://"))) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "URL_FOTO_INVALIDA",
          "Informe uma URL de foto http ou https com até 500 caracteres");
    }

    if (hashFoto == null || hashFoto.isBlank() || hashFoto.length() > 128) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "HASH_FOTO_INVALIDO",
          "Informe o hash da foto com até 128 caracteres");
    }

    if (capturadaEm == null || capturadaEm.isAfter(OffsetDateTime.now())) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "CAPTURA_INVALIDA",
          "A data de captura não pode estar no futuro");
    }

    var vinculo = gateway
        .buscarVinculoAprovado(usuarioId, condominioId)
        .orElseThrow(() -> erro(
            Tipo.NAO_PERMITIDO,
            "SEM_VINCULO_APROVADO",
            "Você não tem vínculo aprovado com este condomínio"));

    if (!gateway.categoriaExiste(categoriaId)) {
      throw erro(
          Tipo.NAO_ENCONTRADO,
          "CATEGORIA_NAO_ENCONTRADA",
          "Categoria de resíduo não encontrada");
    }

    if (gateway.hashFotoJaUsado(hashFoto)) {
      throw fotoJaUtilizada();
    }

    Integer id;

    try {
      id = gateway.inserir(
          usuarioId,
          condominioId,
          vinculo.torreId(),
          categoriaId,
          urlFoto,
          hashFoto,
          capturadaEm);
    } catch (DuplicateKeyException exception) {
      // Duas postagens com a mesma foto ao mesmo tempo: a restrição UNIQUE do banco decide.
      // Qualquer outra chave duplicada é erro de verdade e não vira "foto repetida".
      if (exception.getMessage() != null
          && exception.getMessage().contains("uq_postagens_hash_foto")) {
        throw fotoJaUtilizada();
      }

      throw exception;
    }

    return gateway.buscarPorId(id)
        .orElseThrow(() -> new IllegalStateException(
            "Postagem criada não encontrada"));
  }

  private PostagemException fotoJaUtilizada() {
    return erro(
        Tipo.CONFLITO,
        "FOTO_JA_UTILIZADA",
        "Esta foto já foi utilizada em outra postagem");
  }

  private PostagemException erro(
      Tipo tipo,
      String codigo,
      String mensagem) {
    return new PostagemException(
        tipo,
        codigo,
        mensagem);
  }
}

package br.com.ecociente.mobile.entrypoint.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.CategoriaResiduo;
import br.com.ecociente.mobile.core.domain.PaginaPostagens;
import br.com.ecociente.mobile.core.domain.Postagem;
import br.com.ecociente.mobile.entrypoint.dto.response.CategoriaResiduoResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.response.PostagemResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.response.PostagensResponseDto;

@Component
public class PostagemMapper {

  public PostagemResponseDto toResponse(Postagem postagem) {
    return new PostagemResponseDto(
        postagem.id(),
        postagem.usuarioId(),
        postagem.autor(),
        postagem.condominioId(),
        postagem.torreId(),
        postagem.categoriaId(),
        postagem.categoria(),
        postagem.urlFoto(),
        postagem.capturadaEm(),
        postagem.dataPostagem(),
        postagem.statusValidacao(),
        postagem.saldoConfianca(),
        postagem.dataLimiteAnalise(),
        postagem.resolvidoEm());
  }

  public CategoriaResiduoResponseDto toResponse(CategoriaResiduo categoria) {
    return new CategoriaResiduoResponseDto(
        categoria.id(),
        categoria.nome(),
        categoria.descricao(),
        categoria.pontosBase());
  }

  public PostagensResponseDto toResponse(PaginaPostagens pagina) {
    return new PostagensResponseDto(
        pagina.total(),
        pagina.pagina(),
        pagina.tamanho(),
        pagina.totalPaginas(),
        pagina.postagens().stream()
            .map(this::toResponse)
            .toList());
  }
}

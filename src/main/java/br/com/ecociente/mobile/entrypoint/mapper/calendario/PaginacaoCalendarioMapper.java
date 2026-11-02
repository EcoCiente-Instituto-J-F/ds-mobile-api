package br.com.ecociente.mobile.entrypoint.mapper.calendario;

import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import br.com.ecociente.mobile.core.domain.calendario.OrdenacaoCalendario;
import br.com.ecociente.mobile.core.domain.calendario.PaginaCalendario;
import br.com.ecociente.mobile.core.domain.calendario.PaginacaoCalendario;

public final class PaginacaoCalendarioMapper {

  private PaginacaoCalendarioMapper() {
  }

  public static PaginacaoCalendario toDomain(Pageable pageable) {
    var ordenacoes = pageable.getSort().stream()
        .map(ordem -> new OrdenacaoCalendario(
            ordem.getProperty(),
            OrdenacaoCalendario.Direcao.valueOf(
                ordem.getDirection().name()),
            ordem.isIgnoreCase(),
            OrdenacaoCalendario.PosicaoNulos.valueOf(
                ordem.getNullHandling().name())))
        .toList();

    return new PaginacaoCalendario(
        pageable.isPaged() ? pageable.getPageNumber() : 0,
        pageable.isPaged() ? pageable.getPageSize() : 1,
        pageable.isPaged(),
        ordenacoes);
  }

  public static <T, R> Page<R> toResponse(
      PaginaCalendario<T> pagina,
      Pageable pageable,
      Function<T, R> mapper) {

    var itens = pagina.itens().stream()
        .map(mapper)
        .toList();

    return new PageImpl<>(
        itens,
        pageable,
        pagina.totalItens());
  }
}
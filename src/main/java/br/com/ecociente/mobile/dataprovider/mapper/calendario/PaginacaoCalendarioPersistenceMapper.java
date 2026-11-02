package br.com.ecociente.mobile.dataprovider.mapper.calendario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.com.ecociente.mobile.core.domain.calendario.PaginaCalendario;
import br.com.ecociente.mobile.core.domain.calendario.PaginacaoCalendario;

public final class PaginacaoCalendarioPersistenceMapper {

  private PaginacaoCalendarioPersistenceMapper() {
  }

  public static Pageable toPageable(PaginacaoCalendario entrada) {
    var ordens = entrada.ordenacoes().stream()
        .map(ordem -> {
          Sort.Order resultado = new Sort.Order(
              Sort.Direction.valueOf(ordem.direcao().name()),
              ordem.propriedade());

          if (ordem.ignorarMaiusculas()) {
            resultado = resultado.ignoreCase();
          }

          return resultado.with(
              Sort.NullHandling.valueOf(
                  ordem.posicaoNulos().name()));
        })
        .toList();

    Sort sort = Sort.by(ordens);

    return entrada.paginada()
        ? PageRequest.of(
            entrada.pagina(),
            entrada.tamanho(),
            sort)
        : Pageable.unpaged(sort);
  }

  public static <T> PaginaCalendario<T> toDomain(
      Page<T> pagina,
      PaginacaoCalendario entrada) {

    return new PaginaCalendario<>(
        pagina.getContent(),
        pagina.getTotalElements(),
        entrada);
  }
}
package br.com.ecociente.mobile.entrypoint.mapper;

import org.springframework.stereotype.Component;
import br.com.ecociente.mobile.core.domain.PaginaCondominiosAtendidos;
import br.com.ecociente.mobile.entrypoint.dto.response.CondominiosAtendidosResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.response.CondominiosAtendidosResponseDto.CondominioDto;
import br.com.ecociente.mobile.entrypoint.dto.response.CondominiosAtendidosResponseDto.EnderecoDto;

@Component
public class CondominiosAtendidosMapper {
  public CondominiosAtendidosResponseDto toResponse(PaginaCondominiosAtendidos pagina) {
    var itens = pagina.condominios().stream().map(c -> {
      var e = c.endereco();
      return new CondominioDto(c.id(), c.nome(), e == null ? null : new EnderecoDto(
          e.cep(), e.estado(), e.cidade(), e.logradouro(), e.numero(), e.complemento()),
          c.totalTorres(), c.diasColeta());
    }).toList();
    return new CondominiosAtendidosResponseDto(pagina.cooperativaId(), pagina.total(),
        pagina.pagina(), pagina.tamanho(), pagina.totalPaginas(), itens);
  }
}

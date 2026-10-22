package br.com.ecociente.mobile.entrypoint.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.Cooperativa;
import br.com.ecociente.mobile.core.domain.PaginaCooperativas;
import br.com.ecociente.mobile.entrypoint.dto.response.CooperativaResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.response.CooperativasResponseDto;

@Component
public class CooperativaMapper {

  public CooperativaResponseDto toResponse(Cooperativa cooperativa) {
    return new CooperativaResponseDto(
        cooperativa.id(),
        cooperativa.nome(),
        cooperativa.email(),
        cooperativa.telefone(),
        cooperativa.cep(),
        cooperativa.logradouro(),
        cooperativa.numero(),
        cooperativa.complemento(),
        cooperativa.cidade(),
        cooperativa.estado(),
        enderecoCompleto(cooperativa),
        cooperativa.mediaAvaliacoes(),
        cooperativa.totalAvaliacoes());
  }

  public CooperativasResponseDto toResponse(PaginaCooperativas pagina) {
    return new CooperativasResponseDto(
        pagina.total(),
        pagina.pagina(),
        pagina.tamanho(),
        pagina.totalPaginas(),
        pagina.cooperativas().stream()
            .map(this::toResponse)
            .toList());
  }

  /** Endereço em uma linha, no formato que o Google Maps entende como destino. */
  static String enderecoCompleto(Cooperativa cooperativa) {
    List<String> partes = new ArrayList<>();

    String rua = juntar(" ", cooperativa.logradouro(), cooperativa.numero());
    if (!rua.isEmpty()) {
      partes.add(rua);
    }

    String complemento = texto(cooperativa.complemento());
    if (!complemento.isEmpty()) {
      partes.add(complemento);
    }

    String cidade = juntar(" - ", cooperativa.cidade(), cooperativa.estado());
    if (!cidade.isEmpty()) {
      partes.add(cidade);
    }

    String cep = formatarCep(cooperativa.cep());
    if (!cep.isEmpty()) {
      partes.add(cep);
    }

    return String.join(", ", partes);
  }

  private static String juntar(String separador, String... valores) {
    List<String> partes = new ArrayList<>();

    for (String valor : valores) {
      String texto = texto(valor);

      if (!texto.isEmpty()) {
        partes.add(texto);
      }
    }

    return String.join(separador, partes);
  }

  private static String formatarCep(String cep) {
    String digitos = texto(cep).replaceAll("\\D", "");

    return digitos.length() == 8
        ? digitos.substring(0, 5) + "-" + digitos.substring(5)
        : texto(cep);
  }

  private static String texto(String valor) {
    return valor == null ? "" : valor.trim();
  }
}

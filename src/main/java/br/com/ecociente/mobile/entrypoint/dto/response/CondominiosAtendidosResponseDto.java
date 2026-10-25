package br.com.ecociente.mobile.entrypoint.dto.response;

import java.util.List;

public record CondominiosAtendidosResponseDto(Integer cooperativaId, long total,
    int pagina, int tamanho, long totalPaginas, List<CondominioDto> condominios) {
  public record CondominioDto(Integer id, String nome, EnderecoDto endereco,
      long totalTorres, List<String> diasColeta) {}
  public record EnderecoDto(String cep, String estado, String cidade,
      String logradouro, String numero, String complemento) {}
}

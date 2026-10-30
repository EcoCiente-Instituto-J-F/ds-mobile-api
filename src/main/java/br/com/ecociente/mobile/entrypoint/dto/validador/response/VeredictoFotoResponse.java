package br.com.ecociente.mobile.entrypoint.dto.validador.response;

import java.util.List;

import br.com.ecociente.mobile.core.domain.validador.Alternativa;
import br.com.ecociente.mobile.core.domain.validador.VeredictoFoto;

/** Contrato do POST /triagem (snake_case na serialização). */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record VeredictoFotoResponse(
    String status,
    Boolean pertinente,
    String categoriaInformada,
    String categoriaDetectada,
    Double confianca,
    String hashFoto,
    List<String> sinais,
    List<AlternativaResponse> alternativas,
    String mensagem) {

  public static VeredictoFotoResponse de(VeredictoFoto veredito) {
    return new VeredictoFotoResponse(
        veredito.status().getValor(),
        veredito.pertinente(),
        veredito.categoriaInformada(),
        veredito.categoriaDetectada(),
        veredito.confianca(),
        veredito.hashFoto(),
        veredito.sinais(),
        veredito.alternativas().stream().map(AlternativaResponse::de).toList(),
        veredito.mensagem());
  }

  public record AlternativaResponse(String categoria, double confianca) {

    static AlternativaResponse de(Alternativa alternativa) {
      return new AlternativaResponse(alternativa.categoria(), alternativa.confianca());
    }
  }
}

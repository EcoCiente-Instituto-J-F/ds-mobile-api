package br.com.ecociente.mobile.core.domain.cadastro;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Condominio {
  private Integer idCondominio;
  private String nomeCondominio;
  private String cnpj;
  private String codigoAcesso;
  private Boolean ativo;
  private Integer tipoCondominioId;
  private Integer sindicoId;
  private Integer enderecoId;  
}

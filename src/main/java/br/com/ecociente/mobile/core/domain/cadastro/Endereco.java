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
public class Endereco {
  private Integer idEndereco;
  private String cep;
  private String estado;
  private String cidade;
  private String logradouro;
  private String numero;
  private String complemento;  
}

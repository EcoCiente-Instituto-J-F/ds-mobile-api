package br.com.ecociente.mobile.dataprovider.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_enderecos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnderecoEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_endereco")
  private Integer idEndereco;

  @Column(name = "cep", length = 10)
  private String cep;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "estado", length = 2)
  private String estado;

  @Column(name = "cidade", length = 100)
  private String cidade;

  @Column(name = "bairro", length = 100)
  private String bairro;

  @Column(name = "logradouro", length = 150)
  private String logradouro;

  @Column(name = "numero", length = 20)
  private String numero;

  @Column(name = "complemento", length = 100)
  private String complemento;
}
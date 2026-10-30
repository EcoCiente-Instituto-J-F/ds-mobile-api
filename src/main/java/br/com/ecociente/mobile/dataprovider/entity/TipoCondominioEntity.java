package br.com.ecociente.mobile.dataprovider.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_lkp_tipos_condominios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoCondominioEntity {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_tipo_condominio")
  private Integer idTipoCondominio;

  @Column(name = "nome_tipo",nullable = false, length = 50)
  private String nomeTipo;

  @Column(name = "descricao",length = 255)
  private String descricao;

  public static final int RESIDENCIAL = 1;
  public static final int COMERCIAL = 2;
}

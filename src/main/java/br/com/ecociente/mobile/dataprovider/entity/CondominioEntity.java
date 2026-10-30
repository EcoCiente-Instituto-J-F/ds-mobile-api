package br.com.ecociente.mobile.dataprovider.entity;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity

@Table(name = "tb_condominios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CondominioEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_condominio")
  private Integer idCondominio;

  @Column(name = "nome_condominio", nullable = false, length = 255)
  private String nomeCondominio;

  @Column(name = "cnpj", length = 18)
  private String cnpj;

  @Column(name = "codigo_acesso", length = 20, unique = true)
  private String codigoAcesso;

  @Column(name = "ativo", nullable = false)
  @Builder.Default
  private Boolean ativo = true;

  @Column(name = "tipo_condominio_id", nullable = false)
  private Integer tipoCondominioId;

  @Column(name = "endereco_id")
  private Integer enderecoId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "endereco_id", insertable = false, updatable = false)
  private EnderecoEntity endereco;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tipo_condominio_id", insertable = false, updatable = false)
  private TipoCondominioEntity tipoCondominioEntity;

}

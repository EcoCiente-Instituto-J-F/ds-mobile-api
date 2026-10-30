package br.com.ecociente.mobile.dataprovider.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_cooperativas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CooperativaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_cooperativa")
  private Integer id;

  @Column(name = "cnpj_cooperativa", nullable = false, length = 18)
  private String cnpj;

  @Column(name = "nome_cooperativa", nullable = false, length = 255)
  private String nome;

  @Column(name = "email_cooperativa", length = 255)
  private String email;

  @Column(name = "telefone_cooperativa", length = 20)
  private String telefone;

  @Builder.Default
  @Column(name = "data_cadastro", nullable = false, updatable = false)
  private OffsetDateTime dataCadastro = OffsetDateTime.now();

  @Column(name = "usuario_id")
  private Integer usuarioId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "usuario_id", insertable = false, updatable = false)
  private UsuarioEntity usuario;

  @Column(name = "endereco_id")
  private Integer enderecoId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "endereco_id", insertable = false, updatable = false)
  private EnderecoEntity endereco;

  @PrePersist
  private void prepararCadastro() {
    if (dataCadastro == null) {
      dataCadastro = OffsetDateTime.now();
    }
  }
}
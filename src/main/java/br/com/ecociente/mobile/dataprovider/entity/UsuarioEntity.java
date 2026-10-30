package br.com.ecociente.mobile.dataprovider.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_usuario")
  private Integer id;

  @Column(name = "nome_usuario", nullable = false, length = 100)
  private String nomeUsuario;

  @Column(name = "email_usuario", nullable = false, length = 255)
  private String emailUsuario;

  @Column(name = "senha_hash", nullable = false, length = 255)
  private String senhaHash;

  @Column(name = "data_nascimento")
  private LocalDate dataNascimento;

  @Column(name = "cpf", length = 11)
  private String cpf;

  @Column(name = "url_avatar", length = 500)
  private String urlAvatar;

  @Builder.Default
  @Column(name = "ativo", nullable = false)
  private Boolean ativo = true;

  @Builder.Default
  @Column(name = "registro_em", nullable = false, updatable = false)
  private OffsetDateTime registroEm = OffsetDateTime.now();

  @Column(name = "tipo_usuario_id", nullable = false)
  private Integer tipoUsuarioId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tipo_usuario_id", insertable = false, updatable = false)
  private TipoUsuarioEntity tipoUsuario;

  @Column(name = "endereco_id", nullable = false)
  private Integer enderecoId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "endereco_id", insertable = false, updatable = false)
  private EnderecoEntity endereco;

  @PrePersist
  private void prepararCadastro() {
    if (ativo == null) {
      ativo = true;
    }

    if (registroEm == null) {
      registroEm = OffsetDateTime.now();
    }
  }
}
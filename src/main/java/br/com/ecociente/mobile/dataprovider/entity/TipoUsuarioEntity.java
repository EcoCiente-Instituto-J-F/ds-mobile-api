package br.com.ecociente.mobile.dataprovider.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_lkp_tipos_usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoUsuarioEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_tipo_usuario")
  private Integer idTipoUsuario;

  @Column(name = "nome_tipo", nullable = false, length = 50)
  private String nomeTipo;

  @Column(name = "descricao", length = 255)
  private String descricao;
}
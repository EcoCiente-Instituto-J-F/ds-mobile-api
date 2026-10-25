package br.com.ecociente.mobile.dataprovider.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_cooperativas")
@Getter
@NoArgsConstructor
public class CooperativaDetalheEntity {

  @Id
  @Column(name = "id_cooperativa")
  private Integer id;

  @Column(name = "nome_cooperativa")
  private String nome;

  @Column(name = "email_cooperativa")
  private String email;

  @Column(name = "telefone_cooperativa")
  private String telefone;
}
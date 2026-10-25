package br.com.ecociente.mobile.dataprovider.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_postagens")
@Getter
@NoArgsConstructor
public class PostagemAnaliseEntity {

  @Id
  @Column(name = "id_postagem")
  private Integer id;

  @Column(name = "usuario_id")
  private Integer usuarioId;

  @Column(name = "condominio_id")
  private Integer condominioId;

  @Column(name = "torre_id")
  private Integer torreId;

  @Column(name = "categoria_id")
  private Integer categoriaId;

  @Column(name = "url_foto")
  private String urlFoto;

  @Column(name = "capturada_em")
  private OffsetDateTime capturadaEm;

  @Column(name = "data_postagem")
  private OffsetDateTime dataPostagem;

  @Column(name = "status_validacao_id")
  private Integer statusValidacaoId;

  @Column(name = "saldo_confianca")
  private int saldoConfianca;

  @Column(name = "data_limite_analise")
  private OffsetDateTime dataLimiteAnalise;

  @Column(name = "resolvido_em")
  private OffsetDateTime resolvidoEm;
}
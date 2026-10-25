package br.com.ecociente.mobile.dataprovider.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_conversas")
@Getter
@Setter
@NoArgsConstructor
public class ConversaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_conversa")
  private Integer id;

  @Column(name = "condominio_id", nullable = false)
  private Integer condominioId;

  @Column(name = "cooperativa_id", nullable = false)
  private Integer cooperativaId;

  @Column(name = "criada_em", nullable = false, updatable = false)
  private OffsetDateTime criadaEm;

  @Column(name = "ultima_mensagem_em")
  private OffsetDateTime ultimaMensagemEm;

  @Column(name = "ativa", nullable = false)
  private boolean ativa = true;

  @PrePersist
  void prepararCriacao() {
    if (criadaEm == null) {
      criadaEm = OffsetDateTime.now(ZoneOffset.UTC);
    }
  }
}
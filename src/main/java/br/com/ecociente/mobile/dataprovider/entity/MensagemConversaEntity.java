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
@Table(name = "tb_mensagens_conversas")
@Getter
@Setter
@NoArgsConstructor
public class MensagemConversaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_mensagem")
  private Long id;

  @Column(name = "conversa_id", nullable = false, updatable = false)
  private Integer conversaId;

  @Column(name = "remetente_usuario_id", nullable = false, updatable = false)
  private Integer remetenteUsuarioId;

  @Column(name = "remetente_tipo", nullable = false, length = 20, updatable = false)
  private String remetenteTipo;

  @Column(name = "conteudo", nullable = false, columnDefinition = "text", updatable = false)
  private String conteudo;

  @Column(name = "enviada_em", nullable = false, updatable = false)
  private OffsetDateTime enviadaEm;

  @Column(name = "lida_em")
  private OffsetDateTime lidaEm;

  @PrePersist
  void prepararEnvio() {
    if (enviadaEm == null) {
      enviadaEm = OffsetDateTime.now(ZoneOffset.UTC);
    }
  }
}
package br.com.ecociente.mobile.dataprovider.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table (name = "tb_rel_usuarios_condominios",
  uniqueConstraints = @UniqueConstraint (
    name = "usuarios_condominios_index_5",
    columnNames = {"usuario_id","condominio_id"}
  )
) 
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class VinculoUsuarioCondominioEntity {

  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column (name = "id_usuario_condominio")
  private Integer idMorador;

  @Column(name = "usuario_id", nullable = false)
private Integer usuarioId;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(
    name = "usuario_id",
    nullable = false,
    insertable = false,
    updatable = false
)
private UsuarioEntity userEntity;

@Column(name = "condominio_id", nullable = false)
private Integer condominioId;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(
    name = "condominio_id",
    nullable = false,
    insertable = false,
    updatable = false
)
private CondominioEntity condominio;
  @Column ( name = "data_entrada", nullable = false)
  @Builder.Default
  private OffsetDateTime dataEntrada = OffsetDateTime.now();

  @Column (name = "data_saida")
  private OffsetDateTime dataSaida;

  @Column (name = "aprovado", nullable = false)
  @Builder.Default
  private Boolean aprovado = true;

  @ManyToOne (fetch = FetchType.LAZY)
  @JoinColumn (name = "aprovado_por_usuario_id")
  private UsuarioEntity aprovadoPorUser;

  @Column (name = "nivel_confianca_id", nullable = false)
  @Builder.Default
  private Integer nivelConfiancaId = 1;

  @Column (name = "trust_score", nullable = false)
  @Builder.Default
  private BigDecimal trustScore = BigDecimal.ZERO;

  @Column (name = "postagens_validadas_sem_contestacao",nullable = false)
  @Builder.Default
  private Integer postagensValidasSemContestacao = 0;

  @Column (name = "denuncias_realizadas", nullable = false)
  @Builder.Default
  private Integer denunciasRealizadas = 0;

  @Column (name = "denuncias_procedentes", nullable = false)
  @Builder.Default
  private Integer denunciasProcedentes = 0;

  @Column (name = "taxa_acerto_denuncias")
  private BigDecimal taxaAcertoDenuncias;  
  
}

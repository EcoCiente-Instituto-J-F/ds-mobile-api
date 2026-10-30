package br.com.ecociente.mobile.dataprovider.entity.autenticacao;

import java.time.OffsetDateTime;

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
@Table (name = "tb_autenticacoes_api" )
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class AutenticacaoEntity {

  @Id
  @GeneratedValue (strategy = GenerationType.IDENTITY)
  @Column(name = "id_autenticao_api")
  private Integer id;

  @Column (name = "usuario_id", nullable = false)
  private Integer usuarioId;

  @Column (name = "token", nullable = false, columnDefinition = "TEXT")
  private String token;

  @Column (name = "tipo_token",nullable = false, length = 20)
  private String tipoToken;

  @Column (name = "criado_em",nullable = false)
  private OffsetDateTime criadoEm;

  @Column (name ="expira_em",nullable = false)
  private Integer expiradoEm;  
  
}

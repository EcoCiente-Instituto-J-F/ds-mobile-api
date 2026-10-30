package br.com.ecociente.mobile.dataprovider.mapper.calendario;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.calendario.Coleta;
import br.com.ecociente.mobile.core.domain.calendario.StatusType;

@Component
public class ColetaPersistenciaMapper
    implements RowMapper<Coleta> {

  @Override
  public Coleta mapRow(ResultSet rs, int rowNum)
      throws SQLException {

    return new Coleta(
        rs.getInt("id_agendamento_coleta"),
        rs.getInt("condominio_id"),
        rs.getInt("cooperativa_id"),
        StatusType.porId(rs.getInt("status_agendamento_id")),
        rs.getObject("data_inicio", OffsetDateTime.class),
        rs.getObject("data_fim", OffsetDateTime.class),
        rs.getBoolean("possui_recorrencia"),
        rs.getObject("solicitado_por_usuario_id", Integer.class),
        rs.getString("origem_solicitacao"),
        rs.getObject("respondido_por_usuario_id", Integer.class),
        rs.getObject("respondido_em", OffsetDateTime.class));
  }
}
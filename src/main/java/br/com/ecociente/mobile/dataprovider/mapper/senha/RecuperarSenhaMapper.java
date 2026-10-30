package br.com.ecociente.mobile.dataprovider.mapper.senha;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.senha.RecuperacaoSenha;
import br.com.ecociente.mobile.dataprovider.entity.EsqueceuSenhaEntity;

@Component
public class RecuperarSenhaMapper {

    public RecuperacaoSenha toDomain(EsqueceuSenhaEntity entity) {
        return RecuperacaoSenha.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuarioId())
                .tokenHash(entity.getTokenHash())
                .expiraEm(paraTimestamp(entity.getExpiraEm()))
                .utilizadoEm(paraTimestamp(entity.getUtilizadoEm()))
                .build();
    }

    public EsqueceuSenhaEntity toEntity(RecuperacaoSenha domain) {
        return EsqueceuSenhaEntity.builder()
                .id(domain.getId())
                .usuarioId(domain.getUsuarioId())
                .tokenHash(domain.getTokenHash())
                .expiraEm(paraOffset(domain.getExpiraEm()))
                .utilizadoEm(paraOffset(domain.getUtilizadoEm()))
                .build();
    }

    private Timestamp paraTimestamp(OffsetDateTime valor) {
        return valor == null
                ? null
                : Timestamp.from(valor.toInstant());
    }

    private OffsetDateTime paraOffset(Timestamp valor) {
        return valor == null
                ? null
                : valor.toInstant().atOffset(ZoneOffset.UTC);
    }
}
package br.com.ecociente.mobile.dataprovider.mapper.senha;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import br.com.ecociente.mobile.core.domain.senha.RecuperacaoSenha;
import br.com.ecociente.mobile.dataprovider.entity.EsqueceuSenhaEntity;

class RecuperarSenhaMapperTest {

    private final RecuperarSenhaMapper mapper = new RecuperarSenhaMapper();

    @Test
    void devePreservarInstanteAoConverterFusoDoBanco() {
        var expiracao = OffsetDateTime.parse(
                "2026-10-31T10:15:00-03:00");

        var utilizacao = OffsetDateTime.parse(
                "2026-10-31T10:00:00-03:00");

        var entity = EsqueceuSenhaEntity.builder()
                .id(1)
                .usuarioId(10)
                .tokenHash("hash-teste")
                .expiraEm(expiracao)
                .utilizadoEm(utilizacao)
                .build();

        var domain = mapper.toDomain(entity);

        assertEquals(Integer.valueOf(1), domain.getId());
        assertEquals(Integer.valueOf(10), domain.getUsuarioId());
        assertEquals("hash-teste", domain.getTokenHash());

        assertEquals(
                Instant.parse("2026-10-31T13:15:00Z"),
                domain.getExpiraEm().toInstant());

        assertEquals(
                Instant.parse("2026-10-31T13:00:00Z"),
                domain.getUtilizadoEm().toInstant());
    }

    @Test
    void devePreservarDatasEIdentificadoresNaIdaEVolta() {
        var expiracao = Timestamp.from(
                Instant.parse("2026-10-31T13:15:00.123456Z"));

        var utilizacao = Timestamp.from(
                Instant.parse("2026-10-31T13:00:00.654321Z"));

        var original = RecuperacaoSenha.builder()
                .id(2)
                .usuarioId(20)
                .tokenHash("outro-hash")
                .expiraEm(expiracao)
                .utilizadoEm(utilizacao)
                .build();

        var convertido = mapper.toDomain(mapper.toEntity(original));

        assertEquals(original.getId(), convertido.getId());
        assertEquals(original.getUsuarioId(), convertido.getUsuarioId());
        assertEquals(original.getTokenHash(), convertido.getTokenHash());
        assertEquals(expiracao, convertido.getExpiraEm());
        assertEquals(utilizacao, convertido.getUtilizadoEm());
    }

    @Test
    void deveManterUtilizacaoNulaParaCodigoPendente() {
        var original = RecuperacaoSenha.builder()
                .usuarioId(10)
                .tokenHash("hash-pendente")
                .expiraEm(Timestamp.from(
                        Instant.parse("2026-10-31T13:15:00Z")))
                .utilizadoEm(null)
                .build();

        var entity = mapper.toEntity(original);

        assertNull(entity.getId());
        assertNull(entity.getUtilizadoEm());

        var convertido = mapper.toDomain(entity);

        assertNull(convertido.getUtilizadoEm());
        assertEquals(original.getExpiraEm(), convertido.getExpiraEm());
    }
}
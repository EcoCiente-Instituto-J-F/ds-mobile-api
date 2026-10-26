package br.com.ecociente.mobile.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ecociente.mobile.config.security.JwtService;
import br.com.ecociente.mobile.config.security.SecurityConfig;
import br.com.ecociente.mobile.core.service.UsuarioPerfilService;
import br.com.ecociente.mobile.dataprovider.gateway.UsuarioPerfilGatewayImpl;
import br.com.ecociente.mobile.dataprovider.mapper.UsuarioPerfilPersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.UsuarioPerfilRepository;
import br.com.ecociente.mobile.entrypoint.controller.UsuarioPerfilController;
import br.com.ecociente.mobile.entrypoint.mapper.UsuarioPerfilMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Tag("integracao")
@Testcontainers
@SpringBootTest(
    classes = UsuarioPerfilIntegrationTest.Configuracao.class,
    properties = {
        "app.security.jwt.secret=segredo-perfil-integracao-com-mais-de-32-caracteres",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false"
    }
)
@AutoConfigureMockMvc
@Sql("/sql/usuario-perfil-test.sql")
class UsuarioPerfilIntegrationTest {

    private static final String ENDPOINT = "/api/usuarios/me";

    private static final SecretKey KEY = Keys.hmacShaKeyFor(
        "segredo-perfil-integracao-com-mais-de-32-caracteres"
            .getBytes(StandardCharsets.UTF_8)
    );

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mobile_perfil_test")
            .withUsername("teste")
            .withPassword("teste");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token(int usuarioId) {
        return Jwts.builder()
            .subject("email-do-token@teste.com")
            .claim("usuarioId", usuarioId)
            .claim("perfil", "MORADOR")
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(KEY)
            .compact();
    }

    private JsonNode consultar(int usuarioId) throws Exception {
        var resultado = mvc.perform(get(ENDPOINT)
                .header("Authorization", "Bearer " + token(usuarioId)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON
            ))
            .andReturn();

        return objectMapper.readTree(
            resultado.getResponse().getContentAsString(
                StandardCharsets.UTF_8
            )
        );
    }

    @Test
    void deveRetornarDadosPessoaisEnderecoETelefonesAtivos()
        throws Exception {

        JsonNode perfil = consultar(1);

        assertEquals(1, perfil.get("id").asInt());
        assertEquals("Moradora Teste", perfil.get("nome").asText());
        assertEquals("moradora@teste.com", perfil.get("email").asText());
        assertEquals("2000-02-03", perfil.get("dataNascimento").asText());
        assertEquals("12345678901", perfil.get("cpf").asText());
        assertEquals(
            "https://teste.com/avatar.png",
            perfil.get("urlAvatar").asText()
        );
        assertTrue(perfil.get("ativo").asBoolean());
        assertEquals(
            Instant.parse("2026-01-01T12:00:00Z"),
            Instant.parse(perfil.get("registroEm").asText())
        );
        assertEquals(2, perfil.get("tipoUsuarioId").asInt());
        assertEquals("Morador", perfil.get("tipoUsuario").asText());
        assertEquals(10, perfil.get("moradorId").asInt());
        assertTrue(perfil.get("sindicoId").isNull());
        assertTrue(perfil.get("cooperativa").isNull());

        JsonNode endereco = perfil.get("endereco");

        assertEquals(1, endereco.get("id").asInt());
        assertEquals("01001000", endereco.get("cep").asText());
        assertEquals("SP", endereco.get("estado").asText());
        assertEquals("São Paulo", endereco.get("cidade").asText());
        assertEquals("Rua 1", endereco.get("logradouro").asText());
        assertEquals("1", endereco.get("numero").asText());
        assertTrue(endereco.get("complemento").isNull());

        JsonNode telefones = perfil.get("telefones");

        assertEquals(2, telefones.size());
        assertEquals(1, telefones.get(0).get("id").asInt());
        assertEquals(
            "11911111111",
            telefones.get(0).get("numero").asText()
        );
        assertEquals("Celular", telefones.get(0).get("tipo").asText());
        assertTrue(telefones.get(0).get("ativo").asBoolean());
        assertEquals(2, telefones.get(1).get("id").asInt());
    }

    @Test
    void deveRetornarTorreDoMorador() throws Exception {
        JsonNode torre = consultar(1).get("torre");

        assertEquals(1001, torre.get("id").asInt());
        assertEquals("Torre A", torre.get("nome").asText());
    }

    @Test
    void deveRetornarTorreNulaParaMoradorSemTorre() throws Exception {
        JsonNode perfil = consultar(6);

        assertTrue(perfil.get("torre").isNull());
        assertFalse(perfil.get("moradorId").isNull());
    }

    @Test
    void deveRetornarCondominiosSemDuplicarEComTorresOrdenadas()
        throws Exception {

        JsonNode condominios = consultar(1).get("condominios");

        assertEquals(3, condominios.size());

        JsonNode alameda = condominios.get(0);

        assertEquals(100, alameda.get("id").asInt());
        assertEquals("Alameda", alameda.get("nome").asText());
        assertEquals("11111111000111", alameda.get("cnpj").asText());
        assertTrue(alameda.get("ativo").asBoolean());
        assertEquals(1, alameda.get("tipoCondominioId").asInt());
        assertEquals(
            "Residencial",
            alameda.get("tipoCondominio").asText()
        );
        assertTrue(alameda.get("cadastroMorador").asBoolean());
        assertFalse(alameda.get("sindicoResponsavel").asBoolean());
        assertEquals(6, alameda.get("endereco").get("id").asInt());

        JsonNode torres = alameda.get("torres");

        assertEquals(2, torres.size());
        assertEquals(1001, torres.get(0).get("id").asInt());
        assertEquals("Torre A", torres.get(0).get("nome").asText());
        assertEquals(1002, torres.get(1).get("id").asInt());

        assertEquals(200, condominios.get(1).get("id").asInt());
        assertEquals(300, condominios.get(2).get("id").asInt());
        assertEquals(0, condominios.get(2).get("torres").size());
        assertTrue(condominios.get(2).get("endereco").isNull());
    }

    @Test
    void deveRetornarEstadoEIndicadoresDosVinculos() throws Exception {
        JsonNode condominios = consultar(1).get("condominios");
        JsonNode aprovado = condominios.get(0).get("vinculo");

        assertEquals(1, aprovado.get("id").asInt());
        assertTrue(aprovado.get("aprovado").asBoolean());
        assertTrue(aprovado.get("dataSaida").isNull());
        assertEquals(2, aprovado.get("nivelConfiancaId").asInt());
        assertEquals(
            "pessoa_confiavel",
            aprovado.get("nivelConfianca").asText()
        );
        assertEquals(85.5, aprovado.get("trustScore").asDouble());
        assertEquals(
            12,
            aprovado.get("postagensValidadasSemContestacao").asInt()
        );
        assertEquals(4, aprovado.get("denunciasRealizadas").asInt());
        assertEquals(3, aprovado.get("denunciasProcedentes").asInt());
        assertEquals(75.0, aprovado.get("taxaAcertoDenuncias").asDouble());

        JsonNode pendente = condominios.get(1).get("vinculo");

        assertFalse(pendente.get("aprovado").asBoolean());
        assertTrue(pendente.get("taxaAcertoDenuncias").isNull());

        JsonNode encerrado = condominios.get(2).get("vinculo");

        assertFalse(condominios.get(2).get("ativo").asBoolean());
        assertEquals(
            Instant.parse("2026-03-01T12:00:00Z"),
            Instant.parse(encerrado.get("dataSaida").asText())
        );
    }

    @Test
    void deveRetornarCondominiosDoSindicoSemExigirVinculoRelacional()
        throws Exception {

        JsonNode perfil = consultar(2);

        assertEquals(20, perfil.get("sindicoId").asInt());
        assertTrue(perfil.get("moradorId").isNull());
        assertTrue(perfil.get("torre").isNull());
        assertEquals(2, perfil.get("condominios").size());

        for (JsonNode condominio : perfil.get("condominios")) {
            assertTrue(condominio.get("sindicoResponsavel").asBoolean());
            assertFalse(condominio.get("cadastroMorador").asBoolean());
            assertTrue(condominio.get("vinculo").isNull());
        }
    }

    @Test
    void deveRetornarCooperativaComSeuProprioEndereco() throws Exception {
        JsonNode perfil = consultar(3);
        JsonNode cooperativa = perfil.get("cooperativa");

        assertEquals(30, cooperativa.get("id").asInt());
        assertEquals("EcoCoop", cooperativa.get("nome").asText());
        assertEquals("34238864000168", cooperativa.get("cnpj").asText());
        assertEquals(
            "empresa@teste.com",
            cooperativa.get("email").asText()
        );
        assertEquals(
            "1133334444",
            cooperativa.get("telefone").asText()
        );
        assertEquals(
            Instant.parse("2026-01-02T12:00:00Z"),
            Instant.parse(cooperativa.get("dataCadastro").asText())
        );
        assertEquals(
            8,
            cooperativa.get("endereco").get("id").asInt()
        );
        assertEquals(3, perfil.get("endereco").get("id").asInt());
        assertEquals(0, perfil.get("condominios").size());
    }

    @Test
    void deveRetornarListasVaziasECamposOpcionaisNulos()
        throws Exception {

        JsonNode perfil = consultar(4);

        assertEquals(4, perfil.get("id").asInt());
        assertEquals(0, perfil.get("telefones").size());
        assertEquals(0, perfil.get("condominios").size());
        assertTrue(perfil.get("cooperativa").isNull());
        assertTrue(perfil.get("moradorId").isNull());
        assertTrue(perfil.get("torre").isNull());
        assertTrue(perfil.get("sindicoId").isNull());
        assertTrue(perfil.get("cpf").isNull());
        assertTrue(perfil.get("dataNascimento").isNull());
        assertTrue(perfil.get("urlAvatar").isNull());
    }

    @Test
    void deveRetornarCadastroMoradorMesmoSemRegistroNaTabelaDeVinculos()
        throws Exception {

        jdbc.update(
            "DELETE FROM tb_rel_usuarios_condominios WHERE usuario_id = ?",
            1
        );

        JsonNode condominios = consultar(1).get("condominios");

        assertEquals(1, condominios.size());
        assertEquals(100, condominios.get(0).get("id").asInt());
        assertTrue(condominios.get(0).get("vinculo").isNull());
        assertTrue(condominios.get(0).get("cadastroMorador").asBoolean());
    }

    @Test
    void parametrosDaUrlNaoDevemTrocarUsuarioIdentificadoPeloToken()
        throws Exception {

        mvc.perform(get(ENDPOINT)
                .param("usuarioId", "2")
                .param("id", "3")
                .header("Authorization", "Bearer " + token(1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nome").value("Moradora Teste"))
            .andExpect(jsonPath("$.cooperativa").doesNotExist());
    }

    @Test
    void deveIsolarDadosEntreUsuariosEmRequisicoesConsecutivas()
        throws Exception {

        JsonNode moradora = consultar(1);
        JsonNode comum = consultar(4);

        assertEquals(1, moradora.get("id").asInt());
        assertEquals(4, comum.get("id").asInt());
        assertEquals("comum@teste.com", comum.get("email").asText());
        assertEquals(4, comum.get("endereco").get("id").asInt());
        assertEquals(0, comum.get("telefones").size());
        assertEquals(0, comum.get("condominios").size());
    }

    @Test
    void naoDeveExporCredenciaisOuCodigoDeAcesso()
        throws Exception {

        JsonNode perfil = consultar(1);

        for (String campo : List.of(
            "senha",
            "senhaHash",
            "senha_hash",
            "token",
            "codigoAcesso",
            "codigo_acesso"
        )) {
            assertTrue(
                perfil.findValues(campo).isEmpty(),
                "Campo indevido na resposta: " + campo
            );
        }

        String json = perfil.toString();

        assertFalse(json.contains("hash-nao-expor"));
        assertFalse(json.contains("SEGREDO100"));
    }

    @Test
    void deveRetornarUsuarioInativoComStatusFalse() throws Exception {
        JsonNode perfil = consultar(5);

        assertEquals(5, perfil.get("id").asInt());
        assertFalse(perfil.get("ativo").asBoolean());
    }

    @Test
    void deveRetornar404ParaUsuarioInexistente() throws Exception {
        mvc.perform(get(ENDPOINT)
                .header("Authorization", "Bearer " + token(999)))
            .andExpect(status().isNotFound());
    }

    @Test
    void deveExigirAutenticacao() throws Exception {
        mvc.perform(get(ENDPOINT))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Bearer invalido",
        "Bearer ",
        "Basic abc"
    })
    void deveRejeitarCredenciaisInvalidas(String authorization)
        throws Exception {

        mvc.perform(get(ENDPOINT)
                .header("Authorization", authorization))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarTokenExpirado() throws Exception {
        String expirado = Jwts.builder()
            .subject("moradora@teste.com")
            .claim("usuarioId", 1)
            .expiration(Date.from(Instant.now().minusSeconds(60)))
            .signWith(KEY)
            .compact();

        mvc.perform(get(ENDPOINT)
                .header("Authorization", "Bearer " + expirado))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarTokenAssinadoComOutraChave() throws Exception {
        SecretKey outraChave = Keys.hmacShaKeyFor(
            "outro-segredo-perfil-com-mais-de-32-caracteres"
                .getBytes(StandardCharsets.UTF_8)
        );

        String invalido = Jwts.builder()
            .subject("moradora@teste.com")
            .claim("usuarioId", 1)
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(outraChave)
            .compact();

        mvc.perform(get(ENDPOINT)
                .header("Authorization", "Bearer " + invalido))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarTokenSemUsuarioId() throws Exception {
        String invalido = Jwts.builder()
            .subject("moradora@teste.com")
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(KEY)
            .compact();

        mvc.perform(get(ENDPOINT)
                .header("Authorization", "Bearer " + invalido))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void consultaNaoDeveModificarDados() throws Exception {
        var antes = estadoBanco();

        consultar(1);
        consultar(2);
        consultar(3);

        assertEquals(antes, estadoBanco());
    }

    private Map<String, List<Map<String, Object>>> estadoBanco() {
        Map<String, List<Map<String, Object>>> estado =
            new LinkedHashMap<>();

        for (String tabela : List.of(
            "tb_usuarios",
            "tb_enderecos",
            "tb_telefones",
            "tb_sindicos",
            "tb_moradores",
            "tb_cooperativas",
            "tb_condominios",
            "tb_torres",
            "tb_rel_usuarios_condominios"
        )) {
            estado.put(
                tabela,
                jdbc.queryForList("SELECT * FROM " + tabela + " ORDER BY 1")
            );
        }

        return estado;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class
    })
    @Import({
        SecurityConfig.class,
        JwtService.class,
        UsuarioPerfilController.class,
        UsuarioPerfilService.class,
        UsuarioPerfilMapper.class,
        UsuarioPerfilGatewayImpl.class,
        UsuarioPerfilRepository.class,
        UsuarioPerfilPersistenceMapper.class
    })
    static class Configuracao {
    }
}
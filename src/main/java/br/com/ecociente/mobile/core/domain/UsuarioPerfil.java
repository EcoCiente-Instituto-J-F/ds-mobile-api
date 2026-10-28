package br.com.ecociente.mobile.core.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record UsuarioPerfil(
    Integer id,
    String nome,
    String email,
    LocalDate dataNascimento,
    String cpf,
    String urlAvatar,
    boolean ativo,
    OffsetDateTime registroEm,
    Integer tipoUsuarioId,
    String tipoUsuario,
    Integer moradorId,
    Torre torre,
    Integer sindicoId,
    Endereco endereco,
    List<Telefone> telefones,
    Cooperativa cooperativa,
    List<Condominio> condominios
) {

    public record Endereco(
        Integer id,
        String cep,
        String estado,
        String cidade,
        String logradouro,
        String numero,
        String complemento
    ) {
    }

    public record Telefone(
        Integer id,
        String numero,
        String tipo,
        boolean ativo
    ) {
    }

    public record Cooperativa(
        Integer id,
        String nome,
        String cnpj,
        String email,
        String telefone,
        OffsetDateTime dataCadastro,
        Endereco endereco
    ) {
    }

    public record Condominio(
        Integer id,
        String nome,
        String cnpj,
        boolean ativo,
        Integer tipoCondominioId,
        String tipoCondominio,
        String urlFoto,
        boolean sindicoResponsavel,
        boolean cadastroMorador,
        Endereco endereco,
        Vinculo vinculo,
        List<Torre> torres
    ) {
    }

    public record Vinculo(
        Integer id,
        boolean aprovado,
        OffsetDateTime dataEntrada,
        OffsetDateTime dataSaida,
        Integer nivelConfiancaId,
        String nivelConfianca,
        BigDecimal trustScore,
        Integer postagensValidadasSemContestacao,
        Integer denunciasRealizadas,
        Integer denunciasProcedentes,
        BigDecimal taxaAcertoDenuncias
    ) {
    }

    public record Torre(
        Integer id,
        String nome
    ) {
    }
}
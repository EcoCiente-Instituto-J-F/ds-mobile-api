package br.com.ecociente.mobile.entrypoint.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record UsuarioPerfilResponseDto(
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
    TorreDto torre,
    Integer sindicoId,
    EnderecoDto endereco,
    List<TelefoneDto> telefones,
    CooperativaDto cooperativa,
    List<CondominioDto> condominios
) {

    public record EnderecoDto(
        Integer id,
        String cep,
        String estado,
        String cidade,
        String logradouro,
        String numero,
        String complemento
    ) {
    }

    public record TelefoneDto(
        Integer id,
        String numero,
        String tipo,
        boolean ativo
    ) {
    }

    public record CooperativaDto(
        Integer id,
        String nome,
        String cnpj,
        String email,
        String telefone,
        OffsetDateTime dataCadastro,
        EnderecoDto endereco
    ) {
    }

    public record CondominioDto(
        Integer id,
        String nome,
        String cnpj,
        boolean ativo,
        Integer tipoCondominioId,
        String tipoCondominio,
        String urlFoto,
        boolean sindicoResponsavel,
        boolean cadastroMorador,
        EnderecoDto endereco,
        VinculoDto vinculo,
        List<TorreDto> torres
    ) {
    }

    public record VinculoDto(
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

    public record TorreDto(
        Integer id,
        String nome
    ) {
    }
}
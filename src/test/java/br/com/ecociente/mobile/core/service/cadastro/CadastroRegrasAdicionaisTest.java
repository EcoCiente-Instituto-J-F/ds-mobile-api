package br.com.ecociente.mobile.core.service.cadastro;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ecociente.mobile.core.exception.cadastro.AlreadyExistsException;
import br.com.ecociente.mobile.core.exception.cadastro.BusinessException;
import br.com.ecociente.mobile.core.gateway.cadastro.*;

@ExtendWith(MockitoExtension.class)
class CadastroRegrasAdicionaisTest {

    @Mock private UsuarioGateway usuarioGateway;
    @Mock private EnderecoGateway enderecoGateway;
    @Mock private CepGateway cepGateway;
    @Mock private CodificadorSenhaGateway codificadorSenhaGateway;
    @Mock private TiposCadastroGateway tiposCadastro;
    @Mock private ConsultaDocumentoGateway consultaDocumentoGateway;

    @InjectMocks
    private CadastrarUsuarioComumService service;

    @Test
    void deveBloquearEmailDuplicadoAntesDePersistir() {
        when(usuarioGateway.existePorEmail("pessoa@teste.com"))
                .thenReturn(true);

        assertThrows(
                AlreadyExistsException.class,
                () -> cadastrar(LocalDate.now().minusYears(25)));

        verificarSemPersistencia();
    }

    @Test
    void deveBloquearCpfDuplicadoAntesDePersistir() {
        when(usuarioGateway.existePorCpf("12345678901"))
                .thenReturn(true);

        assertThrows(
                AlreadyExistsException.class,
                () -> cadastrar(LocalDate.now().minusYears(25)));

        verificarSemPersistencia();
    }

    @Test
    void deveNormalizarEmailECpfNaVerificacaoDeDuplicidade() {
        when(usuarioGateway.existePorCpf("12345678901"))
                .thenReturn(true);

        assertThrows(
                AlreadyExistsException.class,
                () -> cadastrar(LocalDate.now().minusYears(25)));

        verify(usuarioGateway).existePorEmail("pessoa@teste.com");
        verify(usuarioGateway).existePorCpf("12345678901");
    }

    @Test
    void deveRecusarNascimentoNoFuturo() {
        assertThrows(
                BusinessException.class,
                () -> cadastrar(LocalDate.now().plusDays(1)));

        verifyNoInteractions(usuarioGateway);
        verificarSemPersistencia();
    }

    @Test
    void deveRecusarCriancaAbaixoDaIdadeMinima() {
        assertThrows(
                BusinessException.class,
                () -> cadastrar(LocalDate.now().minusYears(5)));

        verifyNoInteractions(usuarioGateway);
        verificarSemPersistencia();
    }

    @Test
    void naoDevePersistirQuandoConsultaCepFalha() {
        var falha = new IllegalStateException("CEP indisponível");

        when(cepGateway.buscarEnderecoPorCep("01001000"))
                .thenThrow(falha);

        var recebida = assertThrows(
                IllegalStateException.class,
                () -> cadastrar(LocalDate.now().minusYears(25)));

        assertSame(falha, recebida);
        verify(usuarioGateway, never()).salvar(any());
        verifyNoInteractions(enderecoGateway, codificadorSenhaGateway);
    }

    private void cadastrar(LocalDate nascimento) {
        service.executar(
                "Pessoa Teste",
                "  PESSOA@Teste.COM  ",
                "Senha@2026",
                nascimento,
                "123.456.789-01",
                "01001000",
                "10",
                null);
    }

    private void verificarSemPersistencia() {
        verify(usuarioGateway, never()).salvar(any());

        verifyNoInteractions(
                enderecoGateway,
                cepGateway,
                codificadorSenhaGateway,
                tiposCadastro);
    }
}
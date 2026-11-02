package br.com.ecociente.mobile.dataprovider.gateway.senha;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Properties;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import br.com.ecociente.mobile.core.exception.senha.EnvioEmailException;

@ExtendWith(MockitoExtension.class)
class EmailGatewayImplTest {

    private static final String REMETENTE = "no-reply@ecociente.test";
    private static final String DESTINATARIO = "morador@teste.com";
    private static final String TOKEN = "0123";

    @Mock
    private JavaMailSender mailSender;

    private EmailGatewayImpl gateway;

    @BeforeEach
    void preparar() {
        gateway = new EmailGatewayImpl(mailSender, REMETENTE);
    }

    @Test
    void deveEnviarMensagemComDestinatarioRemetenteECodigo() throws Exception {
        MimeMessage mensagem = prepararMensagem();

        gateway.enviarRecuperacao(DESTINATARIO, TOKEN);
        mensagem.saveChanges();

        assertEquals(
                REMETENTE,
                ((InternetAddress) mensagem.getFrom()[0]).getAddress());

        assertEquals(1, mensagem.getAllRecipients().length);

        assertEquals(
                DESTINATARIO,
                ((InternetAddress) mensagem.getAllRecipients()[0])
                        .getAddress());

        assertEquals(
                "Seu código de recuperação — Ecociente",
                mensagem.getSubject());

        String texto = extrairConteudo(mensagem, "text/plain");
        String html = extrairConteudo(mensagem, "text/html");

        assertFalse(texto.isBlank());
        assertFalse(html.isBlank());

        assertTrue(texto.contains(TOKEN));
        assertTrue(html.contains(TOKEN));

        assertTrue(texto.contains("15 minutos"));
        assertTrue(html.contains("15 minutos"));

        verify(mailSender).send(same(mensagem));
    }

    @Test
    void deveTraduzirFalhaDeEnvioPreservandoCausa() {
        MimeMessage mensagem = prepararMensagem();

        var falha = new MailSendException("SMTP indisponível");

        doThrow(falha).when(mailSender).send(same(mensagem));

        var recebida = assertThrows(
                EnvioEmailException.class,
                () -> gateway.enviarRecuperacao(DESTINATARIO, TOKEN));

        assertSame(falha, recebida.getCause());
    }

    @Test
    void deveTraduzirFalhaNaCriacaoDaMensagem() {
        var falha = new MailSendException(
                "Não foi possível criar a mensagem");

        when(mailSender.createMimeMessage()).thenThrow(falha);

        var recebida = assertThrows(
                EnvioEmailException.class,
                () -> gateway.enviarRecuperacao(DESTINATARIO, TOKEN));

        assertSame(falha, recebida.getCause());

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private MimeMessage prepararMensagem() {
        var sessao = Session.getInstance(new Properties());
        var mensagem = new MimeMessage(sessao);

        when(mailSender.createMimeMessage()).thenReturn(mensagem);

        return mensagem;
    }

    private String extrairConteudo(
            jakarta.mail.Part parte,
            String mimeType) throws Exception {

        if (parte.isMimeType(mimeType)) {
            Object conteudo = parte.getContent();
            return conteudo instanceof String texto ? texto : "";
        }

        if (parte.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) parte.getContent();
            StringBuilder resultado = new StringBuilder();

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart filha = multipart.getBodyPart(i);
                resultado.append(extrairConteudo(filha, mimeType));
            }

            return resultado.toString();
        }

        return "";
    }
}
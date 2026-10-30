package br.com.ecociente.mobile.dataprovider.gateway.senha;

import br.com.ecociente.mobile.core.exception.senha.EnvioEmailException;
import br.com.ecociente.mobile.core.gateway.senha.EmailGateway;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailGatewayImpl implements EmailGateway {

    private final JavaMailSender mailSender;
    private final String remetente;

    public EmailGatewayImpl(
            JavaMailSender mailSender,
            @Value("${app.mail.remetente}") String remetente
    ) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    @Override
    public void enviarRecuperacao(String destinatario, String token) {
        String texto = """
                Ecociente — Recuperação de senha

                Seu código de recuperação é: %s

                Digite esse código na tela de recuperação de senha.
                Ele vale por 15 minutos e pode ser utilizado uma única vez.

                Se você solicitou outro código, use o mais recente.
                Não compartilhe este código.

                Se você não solicitou a recuperação, ignore este e-mail.
                """.formatted(token);

        String html = """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:0;background-color:#DFEADE;
                             font-family:Arial,Helvetica,sans-serif;">
                    <table role="presentation" width="100%%"
                           cellspacing="0" cellpadding="0"
                           style="background-color:#DFEADE;">
                        <tr>
                            <td align="center" style="padding:36px 16px;">
                                <table role="presentation" width="100%%"
                                       cellspacing="0" cellpadding="0"
                                       style="max-width:520px;
                                              background-color:#FFFFFF;
                                              border-radius:18px;">
                                    <tr>
                                        <td style="padding:32px;
                                                   background-color:#064E3B;
                                                   border-radius:18px 18px 0 0;">
                                            <div style="font-size:30px;
                                                        font-weight:bold;
                                                        color:#FFFFFF;">
                                                ecociente
                                            </div>
                                            <div style="margin-top:8px;
                                                        font-size:14px;
                                                        color:#DFEADE;">
                                                Dados que despertam consciência
                                            </div>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td style="padding:32px;
                                                   text-align:center;">
                                            <h1 style="margin:0 0 16px;
                                                       font-size:24px;
                                                       color:#064E3B;">
                                                Recupere seu acesso
                                            </h1>

                                            <p style="margin:0;font-size:16px;
                                                      line-height:24px;
                                                      color:#4B5563;">
                                                Use o código abaixo para
                                                cadastrar sua nova senha.
                                            </p>

                                            <div style="margin:28px 0;
                                                        padding:22px;
                                                        background-color:#F0F6EF;
                                                        border:1px solid #DFEADE;
                                                        border-radius:12px;">
                                                <span style="font-family:monospace;
                                                             font-size:42px;
                                                             font-weight:bold;
                                                             letter-spacing:10px;
                                                             color:#064E3B;">
                                                    %s
                                                </span>
                                            </div>

                                            <p style="margin:0;font-size:14px;
                                                      line-height:22px;
                                                      color:#4B5563;">
                                                Válido por <strong>15 minutos</strong>.
                                                <br>
                                                Pode ser utilizado uma única vez.
                                            </p>

                                            <p style="margin:20px 0 0;
                                                      font-size:14px;
                                                      line-height:22px;
                                                      color:#4B5563;">
                                                Solicitou outro código?
                                                Use o mais recente.
                                                <br>
                                                Não compartilhe seu código.
                                            </p>

                                            <p style="margin:24px 0 0;
                                                      font-size:13px;
                                                      line-height:20px;
                                                      color:#6B7280;">
                                                Se você não solicitou essa
                                                recuperação, ignore este e-mail.
                                            </p>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td style="padding:20px 32px;
                                                   border-top:1px solid #DFEADE;
                                                   text-align:center;">
                                            <span style="font-size:13px;
                                                         font-weight:bold;
                                                         color:#D64573;">
                                                Equipe Ecociente
                                            </span>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(token);

        try {
            MimeMessage mensagem = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    mensagem,
                    true,
                    "UTF-8"
            );

            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject("Seu código de recuperação — Ecociente");
            helper.setText(texto, html);

            mailSender.send(mensagem);
        } catch (MessagingException | MailException exception) {
            throw new EnvioEmailException(
                    "Não foi possível enviar o e-mail de recuperação.",
                    exception
            );
        }
    }
}
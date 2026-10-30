package br.com.ecociente.mobile.dataprovider.gateway.senha;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;

import br.com.ecociente.mobile.core.exception.senha.SincronizacaoSenhaException;
import br.com.ecociente.mobile.core.gateway.senha.FirebaseSenhaGateway;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FirebaseSenhaGatewayImpl implements FirebaseSenhaGateway {

  private final FirebaseAuth firebaseAuth;

  @Override
  public void atualizarSenha(String email, String novaSenha) {
    try {
      var usuario = firebaseAuth.getUserByEmail(
          email.trim().toLowerCase(Locale.ROOT));

      firebaseAuth.updateUser(
          new UserRecord.UpdateRequest(usuario.getUid())
              .setPassword(novaSenha));

    } catch (FirebaseAuthException exception) {
      if (exception.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
        return;
      }

      throw new SincronizacaoSenhaException(
          "Não foi possível atualizar a senha no Firebase.",
          exception);
    }
  }
}
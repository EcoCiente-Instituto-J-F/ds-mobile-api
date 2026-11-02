package br.com.ecociente.mobile.config.notificacoes;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class PushAudience {
    private final String modo;
    private final Set<Integer> usuariosPiloto;

    public PushAudience(@Value("${app.push.audience:disabled}") String modo,
            @Value("${app.push.pilot-user-ids:}") String idsPiloto) {
        String normalizado = modo == null ? "disabled" : modo.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("disabled", "pilot", "all").contains(normalizado)) {
            throw new IllegalArgumentException("APP_PUSH_AUDIENCE deve ser disabled, pilot ou all");
        }
        this.modo = normalizado;

        if (!"pilot".equals(normalizado)) {
            this.usuariosPiloto = Set.of();
            return;
        }
        if (idsPiloto == null || idsPiloto.isBlank()) {
            throw new IllegalArgumentException("APP_PUSH_PILOT_USER_IDS obrigatorio no modo pilot");
        }

        Set<Integer> ids = new HashSet<>();
        for (String parte : idsPiloto.split(",", -1)) {
            String texto = parte.trim();
            int id;
            try {
                id = Integer.parseInt(texto);
            } catch (NumberFormatException erro) {
                throw new IllegalArgumentException("APP_PUSH_PILOT_USER_IDS possui ID invalido", erro);
            }
            if (id <= 0) {
                throw new IllegalArgumentException("APP_PUSH_PILOT_USER_IDS exige IDs positivos");
            }
            ids.add(id);
        }
        this.usuariosPiloto = Set.copyOf(ids);
    }

    public boolean permitir(int usuarioId) {
        return usuarioId > 0
                && ("all".equals(modo)
                    || ("pilot".equals(modo) && usuariosPiloto.contains(usuarioId)));
    }
}

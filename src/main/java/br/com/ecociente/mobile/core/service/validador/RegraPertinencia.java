package br.com.ecociente.mobile.core.service.validador;

import java.util.List;
import br.com.ecociente.mobile.core.domain.validador.Alternativa;
import br.com.ecociente.mobile.core.domain.validador.Categorias;
import br.com.ecociente.mobile.core.domain.validador.ClasseProbabilidade;
import br.com.ecociente.mobile.core.domain.validador.StatusTriagem;
import br.com.ecociente.mobile.core.domain.validador.VeredictoFoto;

public final class RegraPertinencia {

    static final int ALTERNATIVAS_MAX = 2;

    private RegraPertinencia() {
    }

    public static VeredictoFoto decidir(String categoriaInformada, List<ClasseProbabilidade> ranking, double limiar, Double limiarRevisao) {
        ClasseProbabilidade topo = ranking.get(0);
        String detectada = topo.classe();
        double probabilidade = topo.probabilidade();
        String informada = Categorias.categoria(categoriaInformada).nome();
        Boolean pertinente;
        String mensagem;
        if (Categorias.NAO_RESIDUO.slug().equals(detectada)) {
            pertinente = false;
            mensagem = "A foto não parece mostrar um resíduo.";
        } else if (!detectada.equals(categoriaInformada)) {
            pertinente = false;
            mensagem = "A foto parece conter " + Categorias.categoria(detectada).nome() + ", não " + informada + ".";
        } else if (probabilidade >= limiar) {
            pertinente = true;
            mensagem = "Foto compatível com " + informada + ".";
        } else if (limiarRevisao != null && probabilidade >= limiarRevisao) {
            pertinente = null;
            mensagem = "Não deu para confirmar que a foto é de " + informada + "; ela vai para revisão.";
        } else {
            pertinente = false;
            mensagem = "Não deu para confirmar que a foto é de " + informada + ". Tente de mais perto e com boa luz.";
        }
        List<Alternativa> alternativas = ranking.subList(1, Math.min(ranking.size(), 1 + ALTERNATIVAS_MAX)).stream().map(c -> new Alternativa(c.classe(), arredondar(c.probabilidade() * 100))).toList();
        return new VeredictoFoto(StatusTriagem.OK, pertinente, categoriaInformada, detectada, arredondar(probabilidade * 100), null, List.of(), alternativas, mensagem);
    }

    static double arredondar(double valor) {
        return Math.round(valor * 100) / 100.0;
    }
}

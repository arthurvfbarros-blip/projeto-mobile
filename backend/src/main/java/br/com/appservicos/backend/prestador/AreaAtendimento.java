package br.com.appservicos.backend.prestador;

import br.com.appservicos.backend.endereco.EnderecoCliente;

import java.text.Normalizer;
import java.util.Locale;

public final class AreaAtendimento {
    private AreaAtendimento() {}

    public static String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .strip()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    public static boolean atende(Prestador prestador, EnderecoCliente endereco) {
        return !normalizar(prestador.getCidade()).isEmpty()
                && normalizar(prestador.getCidade()).equals(normalizar(endereco.getCidade()))
                && !normalizar(prestador.getUf()).isEmpty()
                && normalizar(prestador.getUf()).equals(normalizar(endereco.getUf()))
                && (prestador.getBairros().isEmpty()
                        || prestador.getBairros().stream()
                                .anyMatch(bairro ->
                                        normalizar(bairro)
                                                .equals(normalizar(endereco.getBairro()))));
    }
}

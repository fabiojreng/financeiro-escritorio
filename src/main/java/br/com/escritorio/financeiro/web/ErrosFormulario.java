package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.service.RegraNegocioException;
import org.springframework.validation.BindingResult;

final class ErrosFormulario {

    private ErrosFormulario() {
    }

    static void rejeitar(BindingResult resultado, RegraNegocioException erro) {
        if (erro.getCampo() != null) {
            resultado.rejectValue(erro.getCampo(), "regra", erro.getMessage());
        } else {
            resultado.reject("regra", erro.getMessage());
        }
    }
}

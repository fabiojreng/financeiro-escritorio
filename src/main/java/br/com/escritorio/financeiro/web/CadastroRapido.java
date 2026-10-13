package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.service.RegraNegocioException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.util.function.Supplier;

/**
 * Resposta JSON do cadastro feito dentro de outro formulário (janela "Novo" ao lado de uma lista),
 * sem sair da tela. Em caso de erro, só {@code erro} vem preenchido.
 */
public record CadastroRapido(Long id, String nome, String erro) {

    static CadastroRapido criado(Long id, String nome) {
        return new CadastroRapido(id, nome, null);
    }

    static ResponseEntity<CadastroRapido> executar(BindingResult resultado, Supplier<CadastroRapido> cadastro) {
        if (resultado.hasErrors()) {
            return erro(resultado.getAllErrors().getFirst().getDefaultMessage());
        }
        try {
            return ResponseEntity.ok(cadastro.get());
        } catch (RegraNegocioException e) {
            return erro(e.getMessage());
        }
    }

    private static ResponseEntity<CadastroRapido> erro(String mensagem) {
        return ResponseEntity.badRequest().body(new CadastroRapido(null, null, mensagem));
    }
}

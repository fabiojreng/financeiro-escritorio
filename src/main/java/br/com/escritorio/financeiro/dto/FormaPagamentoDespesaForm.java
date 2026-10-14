package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.FormaPagamentoDespesa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FormaPagamentoDespesaForm {

    private Long id;

    @NotBlank(message = "Informe a sigla da forma de pagamento.")
    @Size(max = 30, message = "A sigla pode ter no máximo 30 caracteres.")
    private String nome;

    @Size(max = 200, message = "A descrição pode ter no máximo 200 caracteres.")
    private String descricao;

    public static FormaPagamentoDespesaForm de(FormaPagamentoDespesa forma) {
        FormaPagamentoDespesaForm form = new FormaPagamentoDespesaForm();
        form.id = forma.getId();
        form.nome = forma.getNome();
        form.descricao = forma.getDescricao();
        return form;
    }

    public boolean isNovo() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}

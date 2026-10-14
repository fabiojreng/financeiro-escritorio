package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.CategoriaDespesa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoriaDespesaForm {

    private Long id;

    @NotBlank(message = "Informe o nome da categoria.")
    @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
    private String nome;

    @Size(max = 200, message = "O histórico padrão pode ter no máximo 200 caracteres.")
    private String historicoPadrao;

    public static CategoriaDespesaForm de(CategoriaDespesa categoria) {
        CategoriaDespesaForm form = new CategoriaDespesaForm();
        form.id = categoria.getId();
        form.nome = categoria.getNome();
        form.historicoPadrao = categoria.getHistoricoPadrao();
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

    public String getHistoricoPadrao() {
        return historicoPadrao;
    }

    public void setHistoricoPadrao(String historicoPadrao) {
        this.historicoPadrao = historicoPadrao;
    }
}

package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Responsavel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResponsavelForm {

    private Long id;

    @NotBlank(message = "Informe o nome do responsável.")
    @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.")
    private String nome;

    @Size(max = 1000, message = "As observações podem ter no máximo 1000 caracteres.")
    private String observacoes;

    public static ResponsavelForm de(Responsavel responsavel) {
        ResponsavelForm form = new ResponsavelForm();
        form.id = responsavel.getId();
        form.nome = responsavel.getNome();
        form.observacoes = responsavel.getObservacoes();
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

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}

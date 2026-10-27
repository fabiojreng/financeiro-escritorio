package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.TipoPericia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TipoPericiaForm {

    private Long id;

    @NotBlank(message = "Informe o nome do tipo de perícia.")
    @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
    private String nome;

    @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres.")
    private String descricao;

    public static TipoPericiaForm de(TipoPericia tipo) {
        TipoPericiaForm form = new TipoPericiaForm();
        form.id = tipo.getId();
        form.nome = tipo.getNome();
        form.descricao = tipo.getDescricao();
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

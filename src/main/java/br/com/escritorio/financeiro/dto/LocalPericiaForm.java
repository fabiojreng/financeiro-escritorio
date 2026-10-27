package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.LocalPericia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LocalPericiaForm {

    private Long id;

    @NotBlank(message = "Informe o nome do local.")
    @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
    private String nome;

    @Size(max = 300, message = "O endereço pode ter no máximo 300 caracteres.")
    private String endereco;

    public static LocalPericiaForm de(LocalPericia local) {
        LocalPericiaForm form = new LocalPericiaForm();
        form.id = local.getId();
        form.nome = local.getNome();
        form.endereco = local.getEndereco();
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

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }
}

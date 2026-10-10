package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Cliente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ClienteForm {

    private Long id;

    @NotBlank(message = "Informe o nome do cliente.")
    @Size(max = 150, message = "O nome pode ter no máximo 150 caracteres.")
    private String nome;

    @Size(max = 20, message = "CPF inválido. Confira os números.")
    private String cpf;

    @Size(max = 30, message = "O telefone pode ter no máximo 30 caracteres.")
    private String telefone;

    @Size(max = 2000, message = "As observações podem ter no máximo 2000 caracteres.")
    private String observacoes;

    @NotNull(message = "Informe a data de cadastro.")
    @PastOrPresent(message = "A data de cadastro não pode estar no futuro.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataCadastro = LocalDate.now();

    public static ClienteForm de(Cliente cliente) {
        ClienteForm form = new ClienteForm();
        form.id = cliente.getId();
        form.nome = cliente.getNome();
        form.cpf = cliente.getCpfFormatado();
        form.telefone = cliente.getTelefone();
        form.observacoes = cliente.getObservacoes();
        form.dataCadastro = cliente.getDataCadastro();
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

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDate getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDate dataCadastro) {
        this.dataCadastro = dataCadastro;
    }
}

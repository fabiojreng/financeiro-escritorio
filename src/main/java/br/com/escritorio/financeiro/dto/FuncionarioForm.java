package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Funcionario;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FuncionarioForm {

    private Long id;

    @NotBlank(message = "Informe o nome.")
    @Size(max = 150, message = "O nome pode ter no máximo 150 caracteres.")
    private String nome;

    @Size(max = 14, message = "CPF inválido. Confira os números.")
    private String cpf;

    @Size(max = 100, message = "O cargo pode ter no máximo 100 caracteres.")
    private String cargo;

    @NotNull(message = "Informe a data de admissão.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataAdmissao;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataDesligamento;

    @NotNull(message = "Informe o salário base.")
    @DecimalMin(value = "0.01", message = "O salário deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal salarioBase;

    @NotNull(message = "Informe o dia de pagamento.")
    @Min(value = 1, message = "O dia de pagamento deve estar entre 1 e 31.")
    @Max(value = 31, message = "O dia de pagamento deve estar entre 1 e 31.")
    private Integer diaPagamento;

    @Size(max = 100, message = "O banco pode ter no máximo 100 caracteres.")
    private String banco;

    @Size(max = 20, message = "A agência pode ter no máximo 20 caracteres.")
    private String agencia;

    @Size(max = 30, message = "A conta pode ter no máximo 30 caracteres.")
    private String conta;

    @Size(max = 150, message = "A chave PIX pode ter no máximo 150 caracteres.")
    private String chavePix;

    @Size(max = 2000, message = "As observações podem ter no máximo 2000 caracteres.")
    private String observacoes;

    public static FuncionarioForm de(Funcionario funcionario) {
        FuncionarioForm form = new FuncionarioForm();
        form.id = funcionario.getId();
        form.nome = funcionario.getNome();
        form.cpf = funcionario.getCpfFormatado();
        form.cargo = funcionario.getCargo();
        form.dataAdmissao = funcionario.getDataAdmissao();
        form.dataDesligamento = funcionario.getDataDesligamento();
        form.salarioBase = funcionario.getSalarioBase();
        form.diaPagamento = funcionario.getDiaPagamento();
        form.banco = funcionario.getBanco();
        form.agencia = funcionario.getAgencia();
        form.conta = funcionario.getConta();
        form.chavePix = funcionario.getChavePix();
        form.observacoes = funcionario.getObservacoes();
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

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public LocalDate getDataAdmissao() {
        return dataAdmissao;
    }

    public void setDataAdmissao(LocalDate dataAdmissao) {
        this.dataAdmissao = dataAdmissao;
    }

    public LocalDate getDataDesligamento() {
        return dataDesligamento;
    }

    public void setDataDesligamento(LocalDate dataDesligamento) {
        this.dataDesligamento = dataDesligamento;
    }

    public BigDecimal getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(BigDecimal salarioBase) {
        this.salarioBase = salarioBase;
    }

    public Integer getDiaPagamento() {
        return diaPagamento;
    }

    public void setDiaPagamento(Integer diaPagamento) {
        this.diaPagamento = diaPagamento;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getAgencia() {
        return agencia;
    }

    public void setAgencia(String agencia) {
        this.agencia = agencia;
    }

    public String getConta() {
        return conta;
    }

    public void setConta(String conta) {
        this.conta = conta;
    }

    public String getChavePix() {
        return chavePix;
    }

    public void setChavePix(String chavePix) {
        this.chavePix = chavePix;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}

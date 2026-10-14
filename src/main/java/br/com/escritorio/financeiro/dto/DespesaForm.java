package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Despesa;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DespesaForm {

    private Long id;

    @NotNull(message = "Informe a data do pagamento.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataPagamento;

    @NotNull(message = "Selecione a categoria.")
    private Long categoriaId;

    @NotNull(message = "Selecione a forma de pagamento.")
    private Long formaPagamentoId;

    @NotBlank(message = "Informe o histórico.")
    @Size(max = 200, message = "O histórico pode ter no máximo 200 caracteres.")
    private String historico;

    @Size(max = 100, message = "O número da nota ou referência pode ter no máximo 100 caracteres.")
    private String numeroDocumento;

    @NotNull(message = "Informe o valor pago.")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valor;

    @NotBlank(message = "Informe o fornecedor.")
    @Size(max = 200, message = "O fornecedor pode ter no máximo 200 caracteres.")
    private String fornecedor;

    @Size(max = 2000, message = "As observações podem ter no máximo 2000 caracteres.")
    private String observacoes;

    public static DespesaForm de(Despesa despesa) {
        DespesaForm form = new DespesaForm();
        form.id = despesa.getId();
        form.dataPagamento = despesa.getDataPagamento();
        form.categoriaId = despesa.getCategoria().getId();
        form.formaPagamentoId = despesa.getFormaPagamento().getId();
        form.historico = despesa.getHistorico();
        form.numeroDocumento = despesa.getNumeroDocumento();
        form.valor = despesa.getValor();
        form.fornecedor = despesa.getFornecedor();
        form.observacoes = despesa.getObservacoes();
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

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Long getFormaPagamentoId() {
        return formaPagamentoId;
    }

    public void setFormaPagamentoId(Long formaPagamentoId) {
        this.formaPagamentoId = formaPagamentoId;
    }

    public String getHistorico() {
        return historico;
    }

    public void setHistorico(String historico) {
        this.historico = historico;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(String fornecedor) {
        this.fornecedor = fornecedor;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}

package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.FormaPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RecebimentoForm {

    private Long parcelaId;

    @NotNull(message = "Informe a data do recebimento.")
    @PastOrPresent(message = "A data do recebimento não pode estar no futuro.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataRecebimento = LocalDate.now();

    @NotNull(message = "Informe o valor recebido.")
    @DecimalMin(value = "0.01", message = "O valor recebido deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valor;

    @NotNull(message = "Selecione a forma de pagamento.")
    private FormaPagamento formaPagamento;

    @Size(max = 2000, message = "As observações podem ter no máximo 2000 caracteres.")
    private String observacoes;

    public Long getParcelaId() {
        return parcelaId;
    }

    public void setParcelaId(Long parcelaId) {
        this.parcelaId = parcelaId;
    }

    public LocalDate getDataRecebimento() {
        return dataRecebimento;
    }

    public void setDataRecebimento(LocalDate dataRecebimento) {
        this.dataRecebimento = dataRecebimento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}

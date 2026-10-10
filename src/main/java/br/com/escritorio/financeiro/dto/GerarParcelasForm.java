package br.com.escritorio.financeiro.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GerarParcelasForm {

    @NotNull(message = "Informe a quantidade de parcelas.")
    @Min(value = 1, message = "A quantidade de parcelas deve ser pelo menos 1.")
    @Max(value = 120, message = "Gere no máximo 120 parcelas de uma vez.")
    private Integer quantidade;

    @NotNull(message = "Informe o vencimento da primeira parcela.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate primeiroVencimento;

    @NotNull(message = "Informe o valor total a parcelar.")
    @DecimalMin(value = "0.01", message = "O valor total deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valorTotal;

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public LocalDate getPrimeiroVencimento() {
        return primeiroVencimento;
    }

    public void setPrimeiroVencimento(LocalDate primeiroVencimento) {
        this.primeiroVencimento = primeiroVencimento;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }
}

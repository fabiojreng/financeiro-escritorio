package br.com.escritorio.financeiro.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ParcelaNegociacaoItem {

    @NotNull(message = "Informe o vencimento da parcela.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate vencimento;

    @NotNull(message = "Informe o valor da parcela.")
    @DecimalMin(value = "0.00", inclusive = false, message = "O valor da parcela deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valor;

    public LocalDate getVencimento() {
        return vencimento;
    }

    public void setVencimento(LocalDate vencimento) {
        this.vencimento = vencimento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
}

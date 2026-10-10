package br.com.escritorio.financeiro.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ConfirmacaoLiberacaoForm {

    @NotNull(message = "Informe a data efetiva da liberação.")
    @PastOrPresent(message = "A data efetiva não pode estar no futuro.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataEfetiva;

    @NotNull(message = "Informe o valor confirmado.")
    @DecimalMin(value = "0.01", message = "O valor confirmado deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valorConfirmado;

    public ConfirmacaoLiberacaoForm() {
    }

    public ConfirmacaoLiberacaoForm(LocalDate dataEfetiva, BigDecimal valorConfirmado) {
        this.dataEfetiva = dataEfetiva;
        this.valorConfirmado = valorConfirmado;
    }

    public LocalDate getDataEfetiva() {
        return dataEfetiva;
    }

    public void setDataEfetiva(LocalDate dataEfetiva) {
        this.dataEfetiva = dataEfetiva;
    }

    public BigDecimal getValorConfirmado() {
        return valorConfirmado;
    }

    public void setValorConfirmado(BigDecimal valorConfirmado) {
        this.valorConfirmado = valorConfirmado;
    }
}

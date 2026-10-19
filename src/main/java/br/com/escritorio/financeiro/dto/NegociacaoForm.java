package br.com.escritorio.financeiro.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class NegociacaoForm {

    @NotNull(message = "Informe o valor negociado.")
    @DecimalMin(value = "0.00", inclusive = false, message = "O valor negociado deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valorNegociado;

    @Size(max = 2000, message = "O motivo pode ter no máximo 2000 caracteres.")
    private String motivo;

    @NotNull(message = "Informe a quantidade de parcelas.")
    @Min(value = 1, message = "Deve haver pelo menos uma parcela.")
    @Max(value = 60, message = "Informe no máximo 60 parcelas.")
    private Integer quantidade = 1;

    @NotNull(message = "Informe o vencimento da primeira parcela.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate primeiroVencimento;

    @Valid
    private List<ParcelaNegociacaoItem> parcelas = new ArrayList<>();

    public BigDecimal getValorNegociado() {
        return valorNegociado;
    }

    public void setValorNegociado(BigDecimal valorNegociado) {
        this.valorNegociado = valorNegociado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

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

    public List<ParcelaNegociacaoItem> getParcelas() {
        return parcelas;
    }

    public void setParcelas(List<ParcelaNegociacaoItem> parcelas) {
        this.parcelas = parcelas == null ? new ArrayList<>() : parcelas;
    }
}

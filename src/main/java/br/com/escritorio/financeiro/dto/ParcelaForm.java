package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.ParcelaHonorario;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ParcelaForm {

    private Long id;

    @NotNull(message = "Informe o vencimento.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate vencimento;

    @NotNull(message = "Informe o valor da parcela.")
    @DecimalMin(value = "0.01", message = "O valor da parcela deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valor;

    public static ParcelaForm de(ParcelaHonorario parcela) {
        ParcelaForm form = new ParcelaForm();
        form.id = parcela.getId();
        form.vencimento = parcela.getVencimento();
        form.valor = parcela.getValor();
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

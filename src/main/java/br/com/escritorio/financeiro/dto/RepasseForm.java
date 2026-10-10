package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Repasse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RepasseForm {

    private Long id;

    @NotNull(message = "Selecione o destinatário do repasse.")
    private Long destinatarioId;

    @NotBlank(message = "Descreva o motivo do repasse.")
    @Size(max = 500, message = "O motivo pode ter no máximo 500 caracteres.")
    private String descricao;

    @NotNull(message = "Informe o valor previsto.")
    @DecimalMin(value = "0.01", message = "O valor previsto deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valorPrevisto;

    @NotNull(message = "Informe a data prevista para o pagamento.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataPrevista;

    @Size(max = 2000, message = "As observações podem ter no máximo 2000 caracteres.")
    private String observacoes;

    public static RepasseForm de(Repasse repasse) {
        RepasseForm form = new RepasseForm();
        form.id = repasse.getId();
        form.destinatarioId = repasse.getDestinatario().getId();
        form.descricao = repasse.getDescricao();
        form.valorPrevisto = repasse.getValorPrevisto();
        form.dataPrevista = repasse.getDataPrevista();
        form.observacoes = repasse.getObservacoes();
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

    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(Long destinatarioId) {
        this.destinatarioId = destinatarioId;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValorPrevisto() {
        return valorPrevisto;
    }

    public void setValorPrevisto(BigDecimal valorPrevisto) {
        this.valorPrevisto = valorPrevisto;
    }

    public LocalDate getDataPrevista() {
        return dataPrevista;
    }

    public void setDataPrevista(LocalDate dataPrevista) {
        this.dataPrevista = dataPrevista;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}

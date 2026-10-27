package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Pericia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/** Nova data, horário e local de uma perícia remarcada. */
public class RemarcacaoPericiaForm {

    @NotNull(message = "Informe a nova data da perícia.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horario;

    private Long localId;

    @Size(max = 500, message = "O motivo pode ter no máximo 500 caracteres.")
    private String motivo;

    /** Já vem com o local atual, que costuma continuar o mesmo. */
    public static RemarcacaoPericiaForm de(Pericia pericia) {
        RemarcacaoPericiaForm form = new RemarcacaoPericiaForm();
        form.localId = pericia.getLocal() == null ? null : pericia.getLocal().getId();
        return form;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public void setHorario(LocalTime horario) {
        this.horario = horario;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}

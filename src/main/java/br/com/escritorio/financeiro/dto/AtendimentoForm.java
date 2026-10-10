package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.SituacaoAtendimento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class AtendimentoForm {

    private Long id;

    @NotNull(message = "Selecione o cliente.")
    private Long clienteId;

    @NotNull(message = "Selecione o tipo de serviço.")
    private Long tipoServicoId;

    @NotNull(message = "Selecione o responsável.")
    private Long responsavelId;

    @Size(max = 50, message = "O número do processo pode ter no máximo 50 caracteres.")
    private String numeroProcesso;

    @NotNull(message = "Informe a data de abertura.")
    @PastOrPresent(message = "A data de abertura não pode estar no futuro.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataAbertura = LocalDate.now();

    @NotNull(message = "Selecione a situação.")
    private SituacaoAtendimento situacao = SituacaoAtendimento.ABERTO;

    @Size(max = 2000, message = "As observações podem ter no máximo 2000 caracteres.")
    private String observacoes;

    public static AtendimentoForm de(Atendimento atendimento) {
        AtendimentoForm form = new AtendimentoForm();
        form.id = atendimento.getId();
        form.clienteId = atendimento.getCliente().getId();
        form.tipoServicoId = atendimento.getTipoServico().getId();
        form.responsavelId = atendimento.getResponsavel().getId();
        form.numeroProcesso = atendimento.getNumeroProcesso();
        form.dataAbertura = atendimento.getDataAbertura();
        form.situacao = atendimento.getSituacao();
        form.observacoes = atendimento.getObservacoes();
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

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getTipoServicoId() {
        return tipoServicoId;
    }

    public void setTipoServicoId(Long tipoServicoId) {
        this.tipoServicoId = tipoServicoId;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(Long responsavelId) {
        this.responsavelId = responsavelId;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public LocalDate getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDate dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public SituacaoAtendimento getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoAtendimento situacao) {
        this.situacao = situacao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}

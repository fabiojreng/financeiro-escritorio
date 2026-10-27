package br.com.escritorio.financeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Perícia médica de um cliente, como era anotada no caderno do escritório.
 * Data, horário e local ficam em branco enquanto o INSS ainda não marcou a perícia.
 */
@Entity
@Table(name = "pericia")
public class Pericia extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_pericia_id", nullable = false)
    private TipoPericia tipoPericia;

    /** De quem é o cliente (por exemplo o próprio escritório ou um parceiro). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Responsavel responsavel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_pericia_id")
    private LocalPericia local;

    private LocalDate data;

    private LocalTime horario;

    @Column(length = 2000)
    private String observacoes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoPericia situacao = SituacaoPericia.AGENDADA;

    @Column(name = "motivo_situacao", length = 500)
    private String motivoSituacao;

    @Column(name = "situacao_alterada_em")
    private LocalDateTime situacaoAlteradaEm;

    @Column(name = "situacao_alterada_por", length = 100)
    private String situacaoAlteradaPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remarcada_para_id")
    private Pericia remarcadaPara;

    protected Pericia() {
    }

    public Pericia(Cliente cliente, TipoPericia tipoPericia, Responsavel responsavel) {
        this.cliente = cliente;
        this.tipoPericia = tipoPericia;
        this.responsavel = responsavel;
    }

    /** Cópia agendada para a nova data, mantendo cliente, tipo, responsável e observações. */
    public Pericia remarcacao(LocalDate novaData, LocalTime novoHorario, LocalPericia novoLocal) {
        Pericia nova = new Pericia(cliente, tipoPericia, responsavel);
        nova.data = novaData;
        nova.horario = novoHorario;
        nova.local = novoLocal;
        nova.observacoes = observacoes;
        return nova;
    }

    public void marcarRealizada(String usuario, LocalDateTime quando) {
        mudarSituacao(SituacaoPericia.REALIZADA, null, usuario, quando);
    }

    public void marcarFalta(String motivo, String usuario, LocalDateTime quando) {
        mudarSituacao(SituacaoPericia.FALTOU, motivo, usuario, quando);
    }

    public void cancelar(String motivo, String usuario, LocalDateTime quando) {
        mudarSituacao(SituacaoPericia.CANCELADA, motivo, usuario, quando);
    }

    public void marcarRemarcada(Pericia nova, String motivo, String usuario, LocalDateTime quando) {
        mudarSituacao(SituacaoPericia.REMARCADA, motivo, usuario, quando);
        this.remarcadaPara = nova;
    }

    private void mudarSituacao(SituacaoPericia nova, String motivo, String usuario, LocalDateTime quando) {
        if (!isAgendada()) {
            throw new IllegalStateException("Só uma perícia agendada pode mudar de situação.");
        }
        this.situacao = nova;
        this.motivoSituacao = motivo;
        this.situacaoAlteradaPor = usuario;
        this.situacaoAlteradaEm = quando;
    }

    public boolean isAgendada() {
        return situacao == SituacaoPericia.AGENDADA;
    }

    public boolean isSemData() {
        return data == null;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public TipoPericia getTipoPericia() {
        return tipoPericia;
    }

    public void setTipoPericia(TipoPericia tipoPericia) {
        this.tipoPericia = tipoPericia;
    }

    public Responsavel getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(Responsavel responsavel) {
        this.responsavel = responsavel;
    }

    public LocalPericia getLocal() {
        return local;
    }

    public void setLocal(LocalPericia local) {
        this.local = local;
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

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public SituacaoPericia getSituacao() {
        return situacao;
    }

    public String getMotivoSituacao() {
        return motivoSituacao;
    }

    public LocalDateTime getSituacaoAlteradaEm() {
        return situacaoAlteradaEm;
    }

    public String getSituacaoAlteradaPor() {
        return situacaoAlteradaPor;
    }

    public Pericia getRemarcadaPara() {
        return remarcadaPara;
    }
}

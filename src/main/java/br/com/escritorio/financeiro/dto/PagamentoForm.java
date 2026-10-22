package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Cpf;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.StatusPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PagamentoForm {

    private Long id;

    /** Cliente escolhido na busca; em branco quando o cliente é novo e será cadastrado pelo CPF. */
    private Long clienteId;

    @NotBlank(message = "Informe o nome do cliente.")
    @Size(max = 150, message = "O nome pode ter no máximo 150 caracteres.")
    private String nomeCliente;

    @NotBlank(message = "Informe o CPF do cliente.")
    @Size(max = 20, message = "Informe um CPF válido.")
    private String cpf;

    @NotEmpty(message = "Selecione ao menos um responsável.")
    private List<Long> responsavelIds = new ArrayList<>();

    @NotNull(message = "Selecione o benefício.")
    private Long tipoServicoId;

    @NotNull(message = "Informe o dia em que o cliente vai receber.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataRecebimento;

    @NotNull(message = "Informe o valor do benefício.")
    @DecimalMin(value = "0.00", inclusive = false, message = "O valor deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valor;

    @NotBlank(message = "Informe o banco em que o cliente vai receber.")
    @Size(max = 80, message = "O banco pode ter no máximo 80 caracteres.")
    private String banco;

    @NotNull(message = "Selecione o status.")
    private StatusPagamento status = StatusPagamento.AGENDADO;

    @Size(max = 2000, message = "As observações podem ter no máximo 2000 caracteres.")
    private String observacoes;

    /** Honorário do escritório; em branco quando ainda não foi combinado. Só é gravado por quem lança honorários. */
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valorHonorario;

    /** Quanto o cliente já pagou do honorário; só vale na situação "Parcial". */
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valorHonorarioPago;

    /** Quanto ainda falta o cliente pagar no "Parcial"; alternativa a informar o valor já pago. Não é gravado. */
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valorHonorarioFalta;

    private SituacaoHonorarioPagamento situacaoHonorario = SituacaoHonorarioPagamento.A_RECEBER;

    public static PagamentoForm de(PagamentoAgendado pagamento) {
        PagamentoForm form = new PagamentoForm();
        form.id = pagamento.getId();
        form.clienteId = pagamento.getCliente().getId();
        form.nomeCliente = pagamento.getCliente().getNome();
        form.cpf = Cpf.formatar(pagamento.getCliente().getCpf());
        form.responsavelIds = pagamento.getResponsaveis().stream()
                .map(r -> r.getResponsavel().getId())
                .collect(Collectors.toCollection(ArrayList::new));
        form.tipoServicoId = pagamento.getTipoServico().getId();
        form.dataRecebimento = pagamento.getDataRecebimento();
        form.valor = pagamento.getValor();
        form.banco = pagamento.getBanco();
        form.status = pagamento.getStatus();
        form.observacoes = pagamento.getObservacoes();
        form.valorHonorario = pagamento.getValorHonorario();
        form.valorHonorarioPago = pagamento.getValorHonorarioPago();
        if (pagamento.getValorHonorario() != null && pagamento.getValorHonorarioPago() != null) {
            form.valorHonorarioFalta = pagamento.getValorHonorario().subtract(pagamento.getValorHonorarioPago());
        }
        form.situacaoHonorario = pagamento.getSituacaoHonorario();
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

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public List<Long> getResponsavelIds() {
        return responsavelIds;
    }

    public void setResponsavelIds(List<Long> responsavelIds) {
        this.responsavelIds = responsavelIds;
    }

    public Long getTipoServicoId() {
        return tipoServicoId;
    }

    public void setTipoServicoId(Long tipoServicoId) {
        this.tipoServicoId = tipoServicoId;
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

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public StatusPagamento getStatus() {
        return status;
    }

    public void setStatus(StatusPagamento status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public BigDecimal getValorHonorario() {
        return valorHonorario;
    }

    public void setValorHonorario(BigDecimal valorHonorario) {
        this.valorHonorario = valorHonorario;
    }

    public BigDecimal getValorHonorarioPago() {
        return valorHonorarioPago;
    }

    public void setValorHonorarioPago(BigDecimal valorHonorarioPago) {
        this.valorHonorarioPago = valorHonorarioPago;
    }

    public BigDecimal getValorHonorarioFalta() {
        return valorHonorarioFalta;
    }

    public void setValorHonorarioFalta(BigDecimal valorHonorarioFalta) {
        this.valorHonorarioFalta = valorHonorarioFalta;
    }

    public SituacaoHonorarioPagamento getSituacaoHonorario() {
        return situacaoHonorario;
    }

    public void setSituacaoHonorario(SituacaoHonorarioPagamento situacaoHonorario) {
        this.situacaoHonorario = situacaoHonorario;
    }
}

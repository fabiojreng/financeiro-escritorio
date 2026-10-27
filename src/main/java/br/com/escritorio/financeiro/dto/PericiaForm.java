package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.Pericia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class PericiaForm {

    private Long id;

    /** Cliente escolhido na busca; em branco quando o cliente é novo e será cadastrado pelo CPF. */
    private Long clienteId;

    @NotBlank(message = "Informe o nome do cliente.")
    @Size(max = 150, message = "O nome pode ter no máximo 150 caracteres.")
    private String nomeCliente;

    @NotBlank(message = "Informe o CPF do cliente.")
    @Size(max = 20, message = "Informe um CPF válido.")
    private String cpf;

    @Size(max = 150, message = "O nome da mãe pode ter no máximo 150 caracteres.")
    private String nomeMae;

    @NotNull(message = "Selecione o tipo de perícia.")
    private Long tipoPericiaId;

    @NotNull(message = "Selecione de quem é o cliente.")
    private Long responsavelId;

    private Long localId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horario;

    @Size(max = 2000, message = "As observações podem ter no máximo 2000 caracteres.")
    private String observacoes;

    public static PericiaForm de(Pericia pericia) {
        PericiaForm form = new PericiaForm();
        form.id = pericia.getId();
        form.preencherCliente(pericia.getCliente());
        form.tipoPericiaId = pericia.getTipoPericia().getId();
        form.responsavelId = pericia.getResponsavel().getId();
        form.localId = pericia.getLocal() == null ? null : pericia.getLocal().getId();
        form.data = pericia.getData();
        form.horario = pericia.getHorario();
        form.observacoes = pericia.getObservacoes();
        return form;
    }

    public void preencherCliente(Cliente cliente) {
        clienteId = cliente.getId();
        nomeCliente = cliente.getNome();
        cpf = cliente.getCpfFormatado();
        nomeMae = cliente.getNomeMae();
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

    public String getNomeMae() {
        return nomeMae;
    }

    public void setNomeMae(String nomeMae) {
        this.nomeMae = nomeMae;
    }

    public Long getTipoPericiaId() {
        return tipoPericiaId;
    }

    public void setTipoPericiaId(Long tipoPericiaId) {
        this.tipoPericiaId = tipoPericiaId;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(Long responsavelId) {
        this.responsavelId = responsavelId;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
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
}

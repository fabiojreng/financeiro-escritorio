package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.BeneficioConcedido;
import br.com.escritorio.financeiro.domain.Cliente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.YearMonth;

public class BeneficioConcedidoForm {

    private Long id;

    /** Cliente escolhido na busca; em branco quando o cliente é novo. */
    private Long clienteId;

    @NotBlank(message = "Informe o nome do cliente.")
    @Size(max = 150, message = "O nome pode ter no máximo 150 caracteres.")
    private String nomeCliente;

    @Size(max = 20, message = "Informe um CPF válido.")
    private String cpf;

    @NotNull(message = "Informe o mês.")
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth competencia;

    @NotNull(message = "Selecione o tipo de benefício.")
    private Long tipoServicoId;

    /** Opcional: o caderno nem sempre indica quem acompanhou o benefício. */
    private Long responsavelId;

    public static BeneficioConcedidoForm de(BeneficioConcedido beneficio) {
        BeneficioConcedidoForm form = new BeneficioConcedidoForm();
        form.id = beneficio.getId();
        form.preencherCliente(beneficio.getCliente());
        form.competencia = beneficio.getMes();
        form.tipoServicoId = beneficio.getTipoServico().getId();
        form.responsavelId = beneficio.getResponsavel() == null ? null : beneficio.getResponsavel().getId();
        return form;
    }

    public void preencherCliente(Cliente cliente) {
        clienteId = cliente.getId();
        nomeCliente = cliente.getNome();
        cpf = cliente.getCpfFormatado();
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

    public YearMonth getCompetencia() {
        return competencia;
    }

    public void setCompetencia(YearMonth competencia) {
        this.competencia = competencia;
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
}

package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.Cliente;

public class CpfDuplicadoException extends RegraNegocioException {

    private final Long clienteExistenteId;
    private final String clienteExistenteNome;

    public CpfDuplicadoException(Cliente existente) {
        super("cpf", "Já existe um cliente cadastrado com este CPF.");
        this.clienteExistenteId = existente.getId();
        this.clienteExistenteNome = existente.getNome();
    }

    public Long getClienteExistenteId() {
        return clienteExistenteId;
    }

    public String getClienteExistenteNome() {
        return clienteExistenteNome;
    }
}

package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Cliente;

/** Cliente sugerido na busca enquanto se digita o nome (resposta JSON). {@code cpf} vem formatado ou nulo. */
public record SugestaoCliente(Long id, String nome, String cpf, String telefone, boolean ativo) {

    static SugestaoCliente de(Cliente cliente) {
        return new SugestaoCliente(cliente.getId(), cliente.getNome(), cliente.getCpfFormatado(),
                cliente.getTelefone(), cliente.isAtivo());
    }
}

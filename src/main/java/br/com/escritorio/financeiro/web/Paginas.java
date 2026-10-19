package br.com.escritorio.financeiro.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * Divide em páginas uma lista que já foi carregada inteira, para as telas cujos totais somam todos
 * os itens do filtro e não só os da página exibida.
 */
final class Paginas {

    static final int TAMANHO = 20;

    private Paginas() {
    }

    static <T> Page<T> de(List<T> itens, int pagina) {
        int total = itens.size();
        int ultima = Math.max((total - 1) / TAMANHO, 0);
        int numero = Math.min(Math.max(pagina, 0), ultima);
        int inicio = numero * TAMANHO;
        return new PageImpl<>(itens.subList(inicio, Math.min(inicio + TAMANHO, total)),
                PageRequest.of(numero, TAMANHO), total);
    }
}

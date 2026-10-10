package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.AcordoHonorarios;

import java.math.BigDecimal;

/** Linha da listagem de honorários. O saldo é {@code null} enquanto o valor contratado não foi definido. */
public record AcordoComTotais(AcordoHonorarios acordo, BigDecimal recebido, BigDecimal saldo) {

    public boolean isQuitado() {
        return saldo != null && saldo.signum() <= 0;
    }
}

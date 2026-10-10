package br.com.escritorio.financeiro.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfTest {

    @Test
    void normalizaRemovendoPontuacao() {
        assertThat(Cpf.normalizar(" 529.982.247-25 ")).isEqualTo("52998224725");
        assertThat(Cpf.normalizar("")).isNull();
        assertThat(Cpf.normalizar("  ")).isNull();
        assertThat(Cpf.normalizar(null)).isNull();
    }

    @Test
    void aceitaCpfsComDigitosVerificadoresCorretos() {
        assertThat(Cpf.valido("52998224725")).isTrue();
        assertThat(Cpf.valido("11144477735")).isTrue();
        assertThat(Cpf.valido("12345678909")).isTrue();
    }

    @Test
    void rejeitaCpfsInvalidos() {
        assertThat(Cpf.valido("52998224724")).isFalse();
        assertThat(Cpf.valido("11111111111")).isFalse();
        assertThat(Cpf.valido("1234567890")).isFalse();
        assertThat(Cpf.valido("123456789012")).isFalse();
        assertThat(Cpf.valido(null)).isFalse();
    }

    @Test
    void formataComPontosEHifen() {
        assertThat(Cpf.formatar("52998224725")).isEqualTo("529.982.247-25");
        assertThat(Cpf.formatar(null)).isNull();
    }
}

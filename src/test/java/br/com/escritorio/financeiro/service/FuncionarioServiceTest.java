package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.dto.FuncionarioForm;
import br.com.escritorio.financeiro.repository.FuncionarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {

    private static final String CPF = "52998224725";

    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private AuditoriaService auditoriaService;

    @Test
    void criarGuardaOCpfSoComNumerosESemLevarOCpfParaAAuditoria() {
        FuncionarioService service = new FuncionarioService(funcionarioRepository, auditoriaService);
        when(funcionarioRepository.findByCpf(CPF)).thenReturn(Optional.empty());
        FuncionarioForm form = form();
        form.setCpf("529.982.247-25");
        form.setCargo("  Secretária  ");
        form.setBanco(" ");

        Funcionario funcionario = service.criar(form);

        assertThat(funcionario.getNome()).isEqualTo("Maria Souza");
        assertThat(funcionario.getCpf()).isEqualTo(CPF);
        assertThat(funcionario.getCargo()).isEqualTo("Secretária");
        assertThat(funcionario.getBanco()).isNull();
        assertThat(funcionario.isAtivo()).isTrue();
        verify(funcionarioRepository).save(funcionario);
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(FuncionarioService.ENTIDADE), any(),
                descricao.capture());
        assertThat(descricao.getValue()).contains("Maria Souza", "R$ 2.500,00").doesNotContain(CPF, "529.982");
    }

    @Test
    void cpfJaCadastradoEmOutroFuncionarioEhRecusado() {
        FuncionarioService service = new FuncionarioService(funcionarioRepository, auditoriaService);
        Funcionario existente = funcionario("João Lima", 9L);
        when(funcionarioRepository.findByCpf(CPF)).thenReturn(Optional.of(existente));
        FuncionarioForm form = form();
        form.setCpf(CPF);

        assertThatThrownBy(() -> service.criar(form))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("João Lima")
                .extracting("campo").isEqualTo("cpf");
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    void cpfInvalidoEhRecusado() {
        FuncionarioService service = new FuncionarioService(funcionarioRepository, auditoriaService);
        FuncionarioForm form = form();
        form.setCpf("111.111.111-11");

        assertThatThrownBy(() -> service.criar(form))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("cpf");
    }

    @Test
    void desligamentoAntesDaAdmissaoEhRecusado() {
        FuncionarioService service = new FuncionarioService(funcionarioRepository, auditoriaService);
        FuncionarioForm form = form();
        form.setDataDesligamento(form.getDataAdmissao().minusDays(1));

        assertThatThrownBy(() -> service.criar(form))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("dataDesligamento");
        verifyNoInteractions(auditoriaService);
    }

    @Test
    void atualizarAuditaSoOQueMudou() {
        FuncionarioService service = new FuncionarioService(funcionarioRepository, auditoriaService);
        Funcionario maria = funcionario("Maria Souza", 1L);
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(maria));
        FuncionarioForm form = FuncionarioForm.de(maria);
        form.setSalarioBase(new BigDecimal("2800.00"));
        form.setChavePix("maria@exemplo.com");

        service.atualizar(1L, form);

        assertThat(maria.getSalarioBase()).isEqualByComparingTo("2800.00");
        assertThat(maria.getChavePix()).isEqualTo("maria@exemplo.com");
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(FuncionarioService.ENTIDADE), eq(1L),
                descricao.capture());
        assertThat(descricao.getValue())
                .contains("salário base de R$ 2.500,00 para R$ 2.800,00", "dados para pagamento")
                .doesNotContain("nome", "cargo", "maria@exemplo.com");
    }

    @Test
    void atualizarSemMudancasNaoRegistraAuditoria() {
        FuncionarioService service = new FuncionarioService(funcionarioRepository, auditoriaService);
        Funcionario maria = funcionario("Maria Souza", 1L);
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(maria));

        service.atualizar(1L, FuncionarioForm.de(maria));

        verifyNoInteractions(auditoriaService);
    }

    @Test
    void funcionarioInativoNaoRecebeLancamentos() {
        FuncionarioService service = new FuncionarioService(funcionarioRepository, auditoriaService);
        Funcionario maria = funcionario("Maria Souza", 1L);
        maria.setAtivo(false);
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(maria));

        assertThatThrownBy(() -> service.buscarAtivo(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void inativarRegistraDesativacao() {
        FuncionarioService service = new FuncionarioService(funcionarioRepository, auditoriaService);
        Funcionario maria = funcionario("Maria Souza", 1L);
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(maria));

        service.alterarSituacao(1L, false);

        assertThat(maria.isAtivo()).isFalse();
        verify(auditoriaService).registrar(eq(AcaoAuditoria.DESATIVACAO), eq(FuncionarioService.ENTIDADE), eq(1L),
                any());
    }

    private static FuncionarioForm form() {
        FuncionarioForm form = new FuncionarioForm();
        form.setNome("  Maria Souza ");
        form.setDataAdmissao(LocalDate.of(2025, 3, 10));
        form.setSalarioBase(new BigDecimal("2500.00"));
        form.setDiaPagamento(5);
        return form;
    }

    private static Funcionario funcionario(String nome, Long id) {
        Funcionario funcionario = new Funcionario(nome, LocalDate.of(2025, 3, 10), new BigDecimal("2500.00"), 5);
        ReflectionTestUtils.setField(funcionario, "id", id);
        return funcionario;
    }
}

package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.domain.Cpf;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.dto.FuncionarioForm;
import br.com.escritorio.financeiro.repository.FuncionarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Cadastro dos funcionários. O CPF fica fora da auditoria: as descrições não guardam documentos pessoais.
 */
@Service
public class FuncionarioService {

    public static final String ENTIDADE = "Funcionário";

    private final FuncionarioRepository funcionarioRepository;
    private final AuditoriaService auditoriaService;

    public FuncionarioService(FuncionarioRepository funcionarioRepository, AuditoriaService auditoriaService) {
        this.funcionarioRepository = funcionarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<Funcionario> listar() {
        return funcionarioRepository.findAllByOrderByAtivoDescNomeAsc();
    }

    /** Todos os funcionários por nome, para os filtros dos relatórios. */
    @Transactional(readOnly = true)
    public List<Funcionario> listarPorNome() {
        return funcionarioRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public Funcionario buscar(Long id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Funcionário não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Auditoria> historico(Long id) {
        return auditoriaService.historico(ENTIDADE, id);
    }

    @Transactional
    public Funcionario criar(FuncionarioForm form) {
        String cpf = validarCpf(form.getCpf(), null);
        validarDatas(form);
        Funcionario funcionario = new Funcionario(form.getNome().trim(), form.getDataAdmissao(),
                form.getSalarioBase(), form.getDiaPagamento());
        funcionario.setCpf(cpf);
        funcionario.setCargo(Textos.opcional(form.getCargo()));
        funcionario.setDataDesligamento(form.getDataDesligamento());
        funcionario.setBanco(Textos.opcional(form.getBanco()));
        funcionario.setAgencia(Textos.opcional(form.getAgencia()));
        funcionario.setConta(Textos.opcional(form.getConta()));
        funcionario.setChavePix(Textos.opcional(form.getChavePix()));
        funcionario.setObservacoes(Textos.opcional(form.getObservacoes()));
        funcionarioRepository.save(funcionario);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, funcionario.getId(),
                "Funcionário '" + funcionario.getNome() + "' cadastrado, admitido em "
                        + Textos.data(funcionario.getDataAdmissao()) + ", com salário base de "
                        + Textos.moeda(funcionario.getSalarioBase()) + ".");
        return funcionario;
    }

    @Transactional
    public Funcionario atualizar(Long id, FuncionarioForm form) {
        Funcionario funcionario = buscar(id);
        String cpf = validarCpf(form.getCpf(), id);
        validarDatas(form);

        List<String> alteracoes = new ArrayList<>();
        String nome = form.getNome().trim();
        if (!funcionario.getNome().equals(nome)) {
            alteracoes.add("nome de '" + funcionario.getNome() + "' para '" + nome + "'");
            funcionario.setNome(nome);
        }
        if (!Objects.equals(funcionario.getCpf(), cpf)) {
            alteracoes.add("CPF");
            funcionario.setCpf(cpf);
        }
        String cargo = Textos.opcional(form.getCargo());
        if (!Objects.equals(funcionario.getCargo(), cargo)) {
            alteracoes.add("cargo");
            funcionario.setCargo(cargo);
        }
        if (!funcionario.getDataAdmissao().equals(form.getDataAdmissao())) {
            alteracoes.add("admissão de " + Textos.data(funcionario.getDataAdmissao()) + " para "
                    + Textos.data(form.getDataAdmissao()));
            funcionario.setDataAdmissao(form.getDataAdmissao());
        }
        if (!Objects.equals(funcionario.getDataDesligamento(), form.getDataDesligamento())) {
            alteracoes.add("desligamento de " + Textos.data(funcionario.getDataDesligamento()) + " para "
                    + Textos.data(form.getDataDesligamento()));
            funcionario.setDataDesligamento(form.getDataDesligamento());
        }
        if (funcionario.getSalarioBase().compareTo(form.getSalarioBase()) != 0) {
            alteracoes.add("salário base de " + Textos.moeda(funcionario.getSalarioBase()) + " para "
                    + Textos.moeda(form.getSalarioBase()));
            funcionario.setSalarioBase(form.getSalarioBase());
        }
        if (!funcionario.getDiaPagamento().equals(form.getDiaPagamento())) {
            alteracoes.add("dia de pagamento de " + funcionario.getDiaPagamento() + " para " + form.getDiaPagamento());
            funcionario.setDiaPagamento(form.getDiaPagamento());
        }
        if (alterouDadosBancarios(funcionario, form)) {
            alteracoes.add("dados para pagamento");
            funcionario.setBanco(Textos.opcional(form.getBanco()));
            funcionario.setAgencia(Textos.opcional(form.getAgencia()));
            funcionario.setConta(Textos.opcional(form.getConta()));
            funcionario.setChavePix(Textos.opcional(form.getChavePix()));
        }
        String observacoes = Textos.opcional(form.getObservacoes());
        if (!Objects.equals(funcionario.getObservacoes(), observacoes)) {
            alteracoes.add("observações");
            funcionario.setObservacoes(observacoes);
        }
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, funcionario.getId(),
                    "Funcionário '" + funcionario.getNome() + "' alterado: " + String.join(", ", alteracoes) + ".");
        }
        return funcionario;
    }

    @Transactional
    public void alterarSituacao(Long id, boolean ativo) {
        Funcionario funcionario = buscar(id);
        if (funcionario.isAtivo() == ativo) {
            return;
        }
        funcionario.setAtivo(ativo);
        auditoriaService.registrar(ativo ? AcaoAuditoria.ATIVACAO : AcaoAuditoria.DESATIVACAO, ENTIDADE,
                funcionario.getId(),
                "Funcionário '" + funcionario.getNome() + "' " + (ativo ? "reativado." : "inativado."));
    }

    /** Novos lançamentos só para funcionários ativos. */
    Funcionario buscarAtivo(Long id) {
        Funcionario funcionario = buscar(id);
        if (!funcionario.isAtivo()) {
            throw new RegraNegocioException("O funcionário '" + funcionario.getNome()
                    + "' está inativo. Reative o cadastro antes de lançar.");
        }
        return funcionario;
    }

    private String validarCpf(String valor, Long idAtual) {
        String cpf = Cpf.normalizar(valor);
        if (cpf == null) {
            return null;
        }
        if (!Cpf.valido(cpf)) {
            throw new RegraNegocioException("cpf", "CPF inválido. Confira os números.");
        }
        funcionarioRepository.findByCpf(cpf)
                .filter(existente -> !existente.getId().equals(idAtual))
                .ifPresent(existente -> {
                    throw new RegraNegocioException("cpf",
                            "Este CPF já está no cadastro de '" + existente.getNome() + "'.");
                });
        return cpf;
    }

    private static void validarDatas(FuncionarioForm form) {
        LocalDate desligamento = form.getDataDesligamento();
        if (desligamento != null && desligamento.isBefore(form.getDataAdmissao())) {
            throw new RegraNegocioException("dataDesligamento",
                    "O desligamento não pode ser antes da admissão.");
        }
    }

    private static boolean alterouDadosBancarios(Funcionario funcionario, FuncionarioForm form) {
        return !Objects.equals(funcionario.getBanco(), Textos.opcional(form.getBanco()))
                || !Objects.equals(funcionario.getAgencia(), Textos.opcional(form.getAgencia()))
                || !Objects.equals(funcionario.getConta(), Textos.opcional(form.getConta()))
                || !Objects.equals(funcionario.getChavePix(), Textos.opcional(form.getChavePix()));
    }
}

package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.Cpf;
import br.com.escritorio.financeiro.dto.ClienteForm;
import br.com.escritorio.financeiro.dto.FiltroSituacao;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ClienteService {

    public static final String ENTIDADE = "Cliente";

    static final int TAMANHO_PAGINA = 20;
    static final int LIMITE_SUGESTOES = 8;
    static final int CARACTERES_MINIMOS_SUGESTAO = 2;
    private static final int DIGITOS_MINIMOS_BUSCA_CPF = 3;

    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    public ClienteService(ClienteRepository clienteRepository, AuditoriaService auditoriaService) {
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
    }

    /**
     * Pesquisa por parte do nome (sem diferenciar acentos e maiúsculas) ou por parte do CPF.
     */
    @Transactional(readOnly = true)
    public Page<Cliente> pesquisar(String busca, FiltroSituacao situacao, int pagina) {
        return pesquisar(busca, null, situacao, pagina);
    }

    /** Como {@link #pesquisar(String, FiltroSituacao, int)}, filtrando também por parte das observações. */
    @Transactional(readOnly = true)
    public Page<Cliente> pesquisar(String busca, String observacao, FiltroSituacao situacao, int pagina) {
        FiltroSituacao filtro = situacao == null ? FiltroSituacao.ATIVOS : situacao;
        return pesquisar(busca, observacao, filtro, PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA));
    }

    /**
     * Clientes ativos para a busca enquanto se digita (nome ou CPF). Termos muito curtos não trazem nada,
     * para a lista não abrir com o cadastro inteiro.
     */
    @Transactional(readOnly = true)
    public List<Cliente> sugerir(String busca) {
        return sugerir(busca, FiltroSituacao.ATIVOS);
    }

    /** Como {@link #sugerir(String)}, escolhendo a situação: os filtros das listas também acham clientes inativos. */
    @Transactional(readOnly = true)
    public List<Cliente> sugerir(String busca, FiltroSituacao situacao) {
        if (busca == null || busca.trim().length() < CARACTERES_MINIMOS_SUGESTAO) {
            return List.of();
        }
        return pesquisar(busca, null, situacao, PageRequest.of(0, LIMITE_SUGESTOES)).getContent();
    }

    private Page<Cliente> pesquisar(String busca, String observacao, FiltroSituacao filtro, PageRequest pagina) {
        String termo = busca == null ? "" : busca.trim();
        String padraoNome = Textos.padraoBusca(termo);

        String digitos = termo.replaceAll("[\\s.\\-]", "");
        String padraoCpf = digitos.matches("\\d{" + DIGITOS_MINIMOS_BUSCA_CPF + ",11}") ? "%" + digitos + "%" : "";

        return clienteRepository.pesquisar(padraoNome, padraoCpf, Textos.padraoBuscaOpcional(observacao),
                filtro.getSituacoes(), pagina);
    }

    @Transactional(readOnly = true)
    public Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Cliente não encontrado."));
    }

    @Transactional
    public Cliente criar(ClienteForm form) {
        String cpf = validarCpf(form.getCpf(), null);
        Cliente cliente = new Cliente(form.getNome().trim(), form.getDataCadastro());
        cliente.setCpf(cpf);
        cliente.setNomeMae(textoOpcional(form.getNomeMae()));
        cliente.setTelefone(textoOpcional(form.getTelefone()));
        cliente.setObservacoes(textoOpcional(form.getObservacoes()));
        clienteRepository.save(cliente);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, cliente.getId(),
                "Cliente '" + cliente.getNome() + "' cadastrado.");
        return cliente;
    }

    /**
     * Usa o cliente já cadastrado com este CPF ou cadastra um novo só com nome, CPF e nome da mãe. Recusa quando
     * o CPF já pertence a um cliente de outro nome, para que um CPF digitado errado não caia na pessoa errada.
     *
     * @param nomeMae   opcional; completa o cadastro do cliente existente que ainda não tem nome da mãe
     * @param origem    tela que cadastrou o cliente, usada na auditoria (ex.: "agenda de pagamentos")
     * @param campoNome campo do formulário que recebe os erros do nome
     * @param campoCpf  campo do formulário que recebe os erros do CPF
     */
    @Transactional
    public Cliente obterOuCadastrarPorCpf(String nome, String cpfInformado, String nomeMae, LocalDate dataCadastro,
                                          String origem, String campoNome, String campoCpf) {
        String nomeLimpo = Textos.opcional(nome);
        if (nomeLimpo == null) {
            throw new RegraNegocioException(campoNome, "Informe o nome do cliente.");
        }
        String cpf = Cpf.normalizar(cpfInformado);
        if (cpf == null) {
            throw new RegraNegocioException(campoCpf, "Informe o CPF do cliente.");
        }
        if (!Cpf.valido(cpf)) {
            throw new RegraNegocioException(campoCpf, "CPF inválido. Confira os números.");
        }

        Optional<Cliente> existente = clienteRepository.findByCpf(cpf);
        if (existente.isPresent()) {
            Cliente cliente = existente.get();
            if (!Textos.mesmoNome(cliente.getNome(), nomeLimpo)) {
                throw new RegraNegocioException(campoCpf, "Este CPF já pertence a '" + cliente.getNome()
                        + "'. Confira o CPF ou use o nome cadastrado.");
            }
            completarNomeMae(cliente, nomeMae, origem);
            return cliente;
        }

        Cliente cliente = new Cliente(nomeLimpo, dataCadastro);
        cliente.setCpf(cpf);
        cliente.setNomeMae(Textos.opcional(nomeMae));
        clienteRepository.save(cliente);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, cliente.getId(),
                "Cliente '" + cliente.getNome() + "' cadastrado pela " + origem + ".");
        return cliente;
    }

    /**
     * Cliente escolhido na busca do formulário. Se ele ainda não tem CPF, o CPF informado completa o cadastro;
     * se já tem, o CPF digitado é ignorado. O nome da mãe só é gravado se o cliente ainda não tiver.
     *
     * @param nomeMae  opcional
     * @param origem   tela que alterou o cliente, usada na auditoria (ex.: "agenda de pagamentos")
     * @param campoCpf campo do formulário que recebe os erros do CPF
     */
    @Transactional
    public Cliente obterEscolhido(Long id, String cpfInformado, String nomeMae, String origem, String campoCpf) {
        Cliente cliente = buscar(id);
        if (cliente.getCpf() != null) {
            completarNomeMae(cliente, nomeMae, origem);
            return cliente;
        }
        String cpf = Cpf.normalizar(cpfInformado);
        if (cpf == null) {
            throw new RegraNegocioException(campoCpf, "Este cliente ainda não tem CPF. Informe o CPF dele.");
        }
        if (!Cpf.valido(cpf)) {
            throw new RegraNegocioException(campoCpf, "CPF inválido. Confira os números.");
        }
        Optional<Cliente> dono = clienteRepository.findByCpf(cpf);
        if (dono.isPresent()) {
            throw new RegraNegocioException(campoCpf, "Este CPF já pertence a '" + dono.get().getNome()
                    + "'. Confira o CPF ou escolha esse cliente.");
        }
        cliente.setCpf(cpf);
        String mae = cliente.getNomeMae() == null ? Textos.opcional(nomeMae) : null;
        if (mae != null) {
            cliente.setNomeMae(mae);
        }
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, cliente.getId(),
                "Cliente '" + cliente.getNome() + "' alterado: " + (mae != null ? "CPF e nome da mãe informados"
                        : "CPF informado") + " pela " + origem + ".");
        return cliente;
    }

    private void completarNomeMae(Cliente cliente, String nomeMae, String origem) {
        String mae = Textos.opcional(nomeMae);
        if (mae == null || cliente.getNomeMae() != null) {
            return;
        }
        cliente.setNomeMae(mae);
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, cliente.getId(),
                "Cliente '" + cliente.getNome() + "' alterado: nome da mãe informado pela " + origem + ".");
    }

    @Transactional
    public Cliente atualizar(Long id, ClienteForm form) {
        Cliente cliente = buscar(id);
        String cpf = validarCpf(form.getCpf(), id);

        List<String> alteracoes = new ArrayList<>();
        String nome = form.getNome().trim();
        if (!cliente.getNome().equals(nome)) {
            alteracoes.add("nome de '" + cliente.getNome() + "' para '" + nome + "'");
            cliente.setNome(nome);
        }
        if (!Objects.equals(cliente.getCpf(), cpf)) {
            alteracoes.add("CPF");
            cliente.setCpf(cpf);
        }
        String nomeMae = textoOpcional(form.getNomeMae());
        if (!Objects.equals(cliente.getNomeMae(), nomeMae)) {
            alteracoes.add("nome da mãe");
            cliente.setNomeMae(nomeMae);
        }
        String telefone = textoOpcional(form.getTelefone());
        if (!Objects.equals(cliente.getTelefone(), telefone)) {
            alteracoes.add("telefone");
            cliente.setTelefone(telefone);
        }
        String observacoes = textoOpcional(form.getObservacoes());
        if (!Objects.equals(cliente.getObservacoes(), observacoes)) {
            alteracoes.add("observações");
            cliente.setObservacoes(observacoes);
        }
        if (!cliente.getDataCadastro().equals(form.getDataCadastro())) {
            alteracoes.add("data de cadastro");
            cliente.setDataCadastro(form.getDataCadastro());
        }

        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, cliente.getId(),
                    "Cliente '" + cliente.getNome() + "' alterado: " + String.join(", ", alteracoes) + ".");
        }
        return cliente;
    }

    @Transactional
    public void alterarSituacao(Long id, boolean ativo) {
        Cliente cliente = buscar(id);
        if (cliente.isAtivo() == ativo) {
            return;
        }
        cliente.setAtivo(ativo);
        auditoriaService.registrar(ativo ? AcaoAuditoria.ATIVACAO : AcaoAuditoria.DESATIVACAO, ENTIDADE,
                cliente.getId(), "Cliente '" + cliente.getNome() + "' " + (ativo ? "ativado." : "desativado."));
    }

    /**
     * Retorna o CPF só com dígitos, ou {@code null} se não informado.
     */
    private String validarCpf(String valor, Long idAtual) {
        String cpf = Cpf.normalizar(valor);
        if (cpf == null) {
            return null;
        }
        if (!Cpf.valido(cpf)) {
            throw new RegraNegocioException("cpf", "CPF inválido. Confira os números.");
        }
        clienteRepository.findByCpf(cpf)
                .filter(existente -> !existente.getId().equals(idAtual))
                .ifPresent(existente -> {
                    throw new CpfDuplicadoException(existente);
                });
        return cpf;
    }

    private static String textoOpcional(String valor) {
        return Textos.opcional(valor);
    }
}

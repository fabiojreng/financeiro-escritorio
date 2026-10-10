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
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ClienteService {

    public static final String ENTIDADE = "Cliente";

    static final int TAMANHO_PAGINA = 20;
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
        String termo = busca == null ? "" : busca.trim();
        String padraoNome = "%" + semAcentos(termo).toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";

        String digitos = termo.replaceAll("[\\s.\\-]", "");
        String padraoCpf = digitos.matches("\\d{" + DIGITOS_MINIMOS_BUSCA_CPF + ",11}") ? "%" + digitos + "%" : "";

        FiltroSituacao filtro = situacao == null ? FiltroSituacao.ATIVOS : situacao;
        return clienteRepository.pesquisar(padraoNome, padraoCpf, filtro.getSituacoes(),
                PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA));
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
        cliente.setTelefone(textoOpcional(form.getTelefone()));
        cliente.setObservacoes(textoOpcional(form.getObservacoes()));
        clienteRepository.save(cliente);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, cliente.getId(),
                "Cliente '" + cliente.getNome() + "' cadastrado.");
        return cliente;
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
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private static String semAcentos(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}

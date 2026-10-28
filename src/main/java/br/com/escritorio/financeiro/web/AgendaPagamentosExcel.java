package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Cpf;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.dto.ValoresHonorario;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.ALTURA_LINHA;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.LINHA_CABECALHO;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.MOEDA;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.ajustar;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.data;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.texto;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.valor;

/**
 * Planilha Excel da agenda de pagamentos no leiaute de impressão comum ({@link LeiauteImpressaoExcel}).
 */
final class AgendaPagamentosExcel {

    static final String ABA = "Pagamentos";
    static final String TABELA = "Pagamentos";
    static final List<String> CABECALHO = List.of(
            "CLIENTE", "CPF", "RESPONSÁVEL", "DIA RECEB.", "BENEFÍCIO",
            "VALOR", "BANCO", "RECEBEU?", "OBSERVAÇÕES", "HONORÁRIO");
    static final String SEM_HONORARIO = "Sem honorário";
    static final short TAMANHO_FONTE = LeiauteImpressaoExcel.TAMANHO_FONTE;

    private static final int COLUNA_VALOR = 5;
    private static final int COLUNA_HONORARIO = 9;
    /** Acima destas larguras (em caracteres) o texto quebra em mais linhas em vez de ser cortado. */
    private static final int[] LARGURAS_MAXIMAS = {48, 19, 30, 14, 42, 19, 36, 26, 54, 22};

    private AgendaPagamentosExcel() {
    }

    /** @param valores original, recebido e saldo do honorário de cada pagamento, pelo id do pagamento */
    static byte[] gerar(String tituloPeriodo, List<PagamentoAgendado> pagamentos, Map<Long, ValoresHonorario> valores) {
        try (XSSFWorkbook planilha = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            XSSFSheet aba = planilha.createSheet(ABA);
            Estilos estilos = new Estilos(planilha);

            LeiauteImpressaoExcel.escreverCabecalho(aba, CABECALHO, estilos.base.cabecalho);

            int[] larguras = larguras(pagamentos, valores);
            int linha = LINHA_CABECALHO + 1;
            for (PagamentoAgendado pagamento : pagamentos) {
                Row row = aba.createRow(linha++);
                String honorario = situacaoHonorario(pagamento, valores);
                int linhasTexto = Math.max(honorario.split("\n").length,
                        LeiauteImpressaoExcel.linhasNecessarias(larguras, pagamento.getCliente().getNome(), null,
                                pagamento.getNomesResponsaveis(), null, pagamento.getTipoServico().getNome(), null,
                                pagamento.getBanco(), null, pagamento.getObservacoes()));
                row.setHeightInPoints(ALTURA_LINHA * linhasTexto);
                CellStyle texto = estilos.base.texto;
                texto(row, 0, pagamento.getCliente().getNome(), texto);
                texto(row, 1, Cpf.formatar(pagamento.getCliente().getCpf()), texto);
                texto(row, 2, pagamento.getNomesResponsaveis(), texto);
                data(row, 3, pagamento.getDataRecebimento(), estilos.base.data);
                texto(row, 4, pagamento.getTipoServico().getNome(), texto);
                valor(row, COLUNA_VALOR, pagamento.getValor(), estilos.base.valor);
                texto(row, 6, pagamento.getBanco(), texto);
                texto(row, 7, pagamento.getStatus().getRotulo(), texto);
                texto(row, 8, pagamento.getObservacoes(), texto);
                texto(row, COLUNA_HONORARIO, honorario, estilos.honorario(pagamento));
            }

            escreverTotal(aba, estilos.base, linha, pagamentos.size());

            LeiauteImpressaoExcel.aplicarLarguras(aba, larguras);
            aba.createFreezePane(0, 1);
            if (!pagamentos.isEmpty()) {
                LeiauteImpressaoExcel.criarTabela(aba, TABELA, CABECALHO.size(), linha - 1);
            }
            String periodo = tituloPeriodo == null || tituloPeriodo.isBlank()
                    ? "Agenda de pagamentos"
                    : tituloPeriodo;
            LeiauteImpressaoExcel.configurarImpressao(planilha, aba, "Agenda de pagamentos — " + periodo,
                    CABECALHO.size(), linha);
            XSSFFormulaEvaluator.evaluateAllFormulaCells(planilha);

            planilha.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar a agenda de pagamentos.", e);
        }
    }

    /** Largura de cada coluna pelo maior conteúdo, limitada à máxima, mas nunca menor que o cabeçalho. */
    static int[] larguras(List<PagamentoAgendado> pagamentos, Map<Long, ValoresHonorario> valores) {
        int[] larguras = new int[CABECALHO.size()];
        BigDecimal total = BigDecimal.ZERO;
        for (PagamentoAgendado p : pagamentos) {
            ajustar(larguras, 0, p.getCliente().getNome());
            ajustar(larguras, 1, Cpf.formatar(p.getCliente().getCpf()));
            ajustar(larguras, 2, p.getNomesResponsaveis());
            ajustar(larguras, 3, "00/00/0000");
            ajustar(larguras, 4, p.getTipoServico().getNome());
            ajustar(larguras, COLUNA_VALOR, p.getValor() == null ? null : MOEDA.format(p.getValor()));
            ajustar(larguras, 6, p.getBanco());
            ajustar(larguras, 7, p.getStatus().getRotulo());
            ajustar(larguras, 8, p.getObservacoes());
            for (String parte : situacaoHonorario(p, valores).split("\n")) {
                ajustar(larguras, COLUNA_HONORARIO, parte);
            }
            if (p.getValor() != null) {
                total = total.add(p.getValor());
            }
        }
        ajustar(larguras, 0, rotuloTotal(pagamentos.size()));
        ajustar(larguras, COLUNA_VALOR, MOEDA.format(total));
        return LeiauteImpressaoExcel.limitar(larguras, CABECALHO, LARGURAS_MAXIMAS);
    }

    private static void escreverTotal(XSSFSheet aba, LeiauteImpressaoExcel.Estilos estilos, int linha,
                                      int quantidade) {
        Row row = aba.createRow(linha);
        row.setHeightInPoints(ALTURA_LINHA);
        texto(row, 0, rotuloTotal(quantidade), estilos.totalRotulo);
        for (int i = 1; i < COLUNA_VALOR; i++) {
            row.createCell(i).setCellStyle(estilos.totalRotulo);
        }
        Cell celularValor = row.createCell(COLUNA_VALOR);
        celularValor.setCellStyle(estilos.totalValor);
        if (quantidade > 0) {
            celularValor.setCellFormula("SUM(F2:F" + linha + ")");
        } else {
            celularValor.setCellValue(0);
        }
        for (int i = COLUNA_VALOR + 1; i < CABECALHO.size(); i++) {
            row.createCell(i).setCellStyle(estilos.totalRotulo);
        }
    }

    /**
     * Se a cliente já quitou o honorário com o escritório; sem valor lançado, ainda não há o que cobrar.
     * No "Parcial", as linhas seguintes mostram quanto já foi pago e quanto falta.
     */
    static String situacaoHonorario(PagamentoAgendado pagamento, Map<Long, ValoresHonorario> valores) {
        if (pagamento.getValorHonorario() == null) {
            return SEM_HONORARIO;
        }
        String rotulo = pagamento.getSituacaoHonorario().getRotulo();
        if (pagamento.getSituacaoHonorario() != SituacaoHonorarioPagamento.PARCIAL) {
            return rotulo;
        }
        ValoresHonorario v = pagamento.getId() == null ? null : valores.get(pagamento.getId());
        if (v == null) {
            v = ValoresHonorario.de(pagamento, null, BigDecimal.ZERO, 0);
        }
        return rotulo + "\nPago R$ " + MOEDA.format(v.recebido()) + "\nFalta R$ " + MOEDA.format(v.saldo());
    }

    private static String rotuloTotal(int quantidade) {
        return quantidade == 1 ? "Total (1 pagamento)" : "Total (" + quantidade + " pagamentos)";
    }

    private static final class Estilos {

        final LeiauteImpressaoExcel.Estilos base;
        final Map<SituacaoHonorarioPagamento, CellStyle> honorarios = new EnumMap<>(SituacaoHonorarioPagamento.class);
        final CellStyle semHonorario;

        Estilos(XSSFWorkbook planilha) {
            base = new LeiauteImpressaoExcel.Estilos(planilha, "#,##0.00");
            // Cores dos estilos "Bom", "Neutro" e "Ruim" do próprio Excel.
            honorarios.put(SituacaoHonorarioPagamento.RECEBIDO, base.destaque("C6EFCE", "006100"));
            honorarios.put(SituacaoHonorarioPagamento.A_RECEBER, base.destaque("FFEB9C", "9C5700"));
            honorarios.put(SituacaoHonorarioPagamento.EM_ATRASO, base.destaque("FFC7CE", "9C0006"));
            honorarios.put(SituacaoHonorarioPagamento.PARCIAL, base.destaque("DDEBF7", "1F4E78"));
            honorarios.put(SituacaoHonorarioPagamento.NEGOCIADO, base.destaque("E7DEFA", "432874"));
            semHonorario = base.destaque("EDEDED", "595959");
        }

        CellStyle honorario(PagamentoAgendado pagamento) {
            return pagamento.getValorHonorario() == null
                    ? semHonorario
                    : honorarios.get(pagamento.getSituacaoHonorario());
        }
    }
}

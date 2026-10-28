package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Despesa;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.YearMonth;
import java.util.List;

import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.ALTURA_LINHA;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.LINHA_CABECALHO;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.MOEDA;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.ajustar;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.data;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.texto;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.valor;

/**
 * Planilha "Contas Pagas" no mesmo leiaute do modelo enviado pela contabilidade para importar no
 * Sistema Domínio: mesma aba, mesmos cabeçalhos (inclusive espaços), mesma ordem de colunas e a
 * coluna VALOR NEGATIVO oculta com a fórmula {@code =-VALOR}. A impressão segue o leiaute comum
 * ({@link LeiauteImpressaoExcel}); não há linha de total, que seria lida como lançamento na importação.
 */
final class PlanilhaContasPagas {

    static final String ABA = "Contas Pagas";
    static final String TABELA = "Tabela1";
    static final List<String> CABECALHO = List.of("DATA ", "CATEGORIA", "FORMA PAGTO", "HISTORICO",
            "Nº NOTA/ REFERENCIA DO PAGAMENTO", "VALOR", "FORNECEDOR", "VALOR NEGATIVO");
    static final int COLUNA_VALOR_NEGATIVO = 7;

    private static final String FORMATO_VALOR = "\"R$\" #,##0.00";
    private static final int COLUNA_VALOR = 5;
    /** Acima destas larguras (em caracteres) o texto quebra em mais linhas em vez de sair da coluna. */
    private static final int[] LARGURAS_MAXIMAS = {16, 30, 22, 48, 26, 20, 40, 21};

    private PlanilhaContasPagas() {
    }

    static byte[] gerar(YearMonth mes, List<Despesa> despesas) {
        try (XSSFWorkbook planilha = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            XSSFSheet aba = planilha.createSheet(ABA);
            LeiauteImpressaoExcel.Estilos estilos = new LeiauteImpressaoExcel.Estilos(planilha, FORMATO_VALOR);

            LeiauteImpressaoExcel.escreverCabecalho(aba, CABECALHO, estilos.cabecalho);

            int[] larguras = larguras(despesas);
            int linha = LINHA_CABECALHO + 1;
            for (Despesa despesa : despesas) {
                Row row = aba.createRow(linha);
                String categoria = despesa.getCategoria().getNome();
                String forma = despesa.getFormaPagamento().getNome();
                row.setHeightInPoints(ALTURA_LINHA * LeiauteImpressaoExcel.linhasNecessarias(larguras, null,
                        categoria, forma, despesa.getHistorico(), despesa.getNumeroDocumento(), null,
                        despesa.getFornecedor()));
                data(row, 0, despesa.getDataPagamento(), estilos.data);
                texto(row, 1, categoria, estilos.texto);
                texto(row, 2, forma, estilos.texto);
                texto(row, 3, despesa.getHistorico(), estilos.texto);
                texto(row, 4, despesa.getNumeroDocumento(), estilos.texto);
                valor(row, COLUNA_VALOR, despesa.getValor(), estilos.valor);
                texto(row, 6, despesa.getFornecedor(), estilos.texto);
                Cell negativo = row.createCell(COLUNA_VALOR_NEGATIVO);
                negativo.setCellFormula("-F" + (linha + 1));
                negativo.setCellStyle(estilos.valor);
                linha++;
            }

            LeiauteImpressaoExcel.aplicarLarguras(aba, larguras);
            LeiauteImpressaoExcel.ajustarAlturaCabecalho(aba, larguras, CABECALHO);
            aba.setColumnHidden(COLUNA_VALOR_NEGATIVO, true);
            aba.createFreezePane(0, 1);

            if (!despesas.isEmpty()) {
                LeiauteImpressaoExcel.criarTabela(aba, TABELA, CABECALHO.size(), linha - 1);
            }
            String titulo = mes == null ? "Contas pagas" : "Contas pagas — " + TituloPeriodo.mes(mes);
            LeiauteImpressaoExcel.configurarImpressao(planilha, aba, titulo, CABECALHO.size(), linha - 1);
            XSSFFormulaEvaluator.evaluateAllFormulaCells(planilha);

            planilha.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar a planilha de despesas.", e);
        }
    }

    /** Largura de cada coluna pelo maior conteúdo, limitada à máxima; o cabeçalho pode quebrar linha. */
    static int[] larguras(List<Despesa> despesas) {
        int[] larguras = new int[CABECALHO.size()];
        for (Despesa d : despesas) {
            ajustar(larguras, 0, "00/00/0000");
            ajustar(larguras, 1, d.getCategoria().getNome());
            ajustar(larguras, 2, d.getFormaPagamento().getNome());
            ajustar(larguras, 3, d.getHistorico());
            ajustar(larguras, 4, d.getNumeroDocumento());
            ajustar(larguras, COLUNA_VALOR, d.getValor() == null ? null : "R$ " + MOEDA.format(d.getValor()));
            ajustar(larguras, 6, d.getFornecedor());
        }
        return LeiauteImpressaoExcel.limitarQuebrandoCabecalho(larguras, CABECALHO, LARGURAS_MAXIMAS);
    }
}

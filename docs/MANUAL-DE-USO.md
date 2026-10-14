# Manual de uso · Financeiro do Escritório

Este manual explica, passo a passo, como usar o sistema no dia a dia do escritório. Ele foi escrito para quem vai operar o sistema (secretaria, advogados, administração), sem exigir conhecimento técnico. A instalação e a configuração do servidor estão no [`README.md`](../README.md).

---

## 1. Para que serve o sistema

O sistema substitui o **caderno de previsões** do escritório. No caderno eram anotados o nome do cliente, o responsável, a data prevista para o INSS liberar o benefício e o valor que o cliente vai receber. Agora essas informações ficam organizadas por mês, com filtros, alertas de atraso e histórico de tudo o que foi alterado.

Além do caderno, o sistema controla o dinheiro do escritório: os **honorários** que cada cliente paga, os **repasses internos** (comissões) pagos a colaboradores e as **despesas do escritório** (contas pagas), que todo mês viram a planilha enviada à contabilidade.

### 1.1. A regra mais importante: tipos de dinheiro que nunca se misturam

| Tipo de valor | De quem é | Onde aparece |
|---|---|---|
| **Benefício do INSS** | Do cliente | Previsões, Agenda, bloco "Valores do cliente" do painel |
| **Honorários** | Do escritório (o cliente paga) | Honorários, bloco "Valores do escritório" do painel |
| **Repasses internos** | Saem do escritório para colaboradores | Repasses internos, bloco "Valores do escritório" do painel |
| **Despesas do escritório** | Contas pagas pelo escritório (energia, impostos, fornecedores) | Despesas do escritório e planilha da contabilidade |

O sistema **nunca soma** esses valores entre si e **nunca calcula** um a partir do outro. Por exemplo: mesmo que o acordo de honorários seja "30% dos atrasados", o valor dos honorários é digitado por você, e não calculado a partir do benefício. Da mesma forma, nenhuma comissão é calculada automaticamente.

### 1.2. Como as informações se organizam

```
Cliente
 └── Atendimento (um serviço jurídico: ex. "Salário-maternidade", com um responsável)
      ├── Previsões de benefício do INSS (uma ou várias)
      ├── Acordo de honorários (no máximo um por atendimento)
      │    ├── Parcelas (opcional)
      │    └── Recebimentos
      └── Repasses internos (um ou vários)
```

Um mesmo cliente pode ter vários atendimentos (por exemplo, um salário-maternidade em 2025 e uma aposentadoria em 2026). Por isso, previsões, honorários e repasses ficam sempre **dentro do atendimento**, e não direto no cliente.

As **despesas do escritório** não pertencem a nenhum cliente: ficam organizadas por mês, pela data em que foram pagas.

### 1.3. O que o sistema muda sozinho (e o que não muda)

A única mudança automática é a da previsão de benefício pela data:

- Quando a data prevista chega, uma previsão **Previsto** passa sozinha para **Aguardando confirmação**. Isso acontece ao salvar a previsão, ao abrir o sistema e todo dia logo depois da meia-noite.
- Se você editar a data de uma previsão em **Aguardando confirmação** para uma data futura (por exemplo, o INSS remarcou), ela volta sozinha para **Previsto**.
- **Adiado**, **Cancelado** e **Liberado pelo INSS** nunca mudam sozinhos.

Cada mudança automática aparece no histórico da previsão e na auditoria com o usuário **sistema**.

Todo o resto depende de uma ação de um usuário. Uma previsão só passa a "Liberado pelo INSS" quando alguém clica em **Confirmar liberação** e informa a data e o valor reais. Parcelas "Vencidas" e repasses "Atrasados" são apenas avisos calculados na hora pela data; a parcela só fica paga com um recebimento, e o repasse só fica pago quando alguém registra o pagamento.

---

## 2. Primeiro acesso e login

1. Abra o navegador e acesse o endereço do sistema (no computador do escritório, normalmente `http://localhost:8080`).
2. Informe **usuário** e **senha** e clique em entrar.
3. Para sair, use o botão **Sair**, no canto inferior do menu lateral.

Pontos importantes:

- Não existe cadastro público. Quem cria usuários é o administrador, no menu **Administração → Usuários**.
- O primeiro administrador é criado pela configuração do servidor (veja o `README.md`, seção "Primeiro acesso").
- Depois de **60 minutos sem uso**, a sessão expira e é preciso entrar de novo.
- Usuários desativados não conseguem entrar.

### 2.1. Perfis de acesso

Existem dois perfis. O **Operador** faz toda a rotina. O **Administrador** faz tudo o que o operador faz e, além disso, cuida das operações sensíveis.

| Ação | Operador | Administrador |
|---|:---:|:---:|
| Cadastrar e editar clientes, responsáveis, tipos de serviço e atendimentos | Sim | Sim |
| Cadastrar previsões, confirmar e corrigir liberações | Sim | Sim |
| Registrar acordos, parcelas e recebimentos de honorários | Sim | Sim |
| Lançar repasses, registrar pagamento, cancelar repasse **pendente** | Sim | Sim |
| Lançar, editar e cancelar despesas do escritório (mês ainda não enviado) | Sim | Sim |
| Baixar a planilha da contabilidade e marcar o mês como enviado | Sim | Sim |
| Ver agenda, painel, relatórios e exportar CSV | Sim | Sim |
| **Desfazer** uma liberação já confirmada | Não | Sim |
| **Cancelar** parcela ou recebimento de honorários | Não | Sim |
| **Cancelar** repasse já **pago** | Não | Sim |
| **Reabrir** um mês de despesas já enviado à contabilidade | Não | Sim |
| Gerenciar usuários | Não | Sim |
| Ver o histórico geral de operações | Não | Sim |

Quando o operador não tem permissão, o botão simplesmente não aparece na tela.

---

## 3. A tela e o menu

O menu fica à esquerda (no celular, ele abre pelo botão ☰ no topo). Ele está dividido em seções:

- **Início**: painel com os indicadores do mês.
- **Rotina**: *Agenda mensal* (o antigo caderno) e *Atendimentos*.
- **Financeiro do escritório**: *Honorários*, *Repasses internos* e *Despesas do escritório*.
- **Consultas**: *Relatórios*.
- **Cadastros**: *Clientes*, *Responsáveis*, *Tipos de serviço*, *Categorias de despesa* e *Formas de pagamento*.
- **Administração** (só administradores): *Usuários* e *Histórico de operações*.

Depois de cada ação, aparece uma faixa **verde** (deu certo) ou **vermelha** (algo impediu a operação, com a explicação). Em formulários, o campo com problema fica destacado em vermelho com a mensagem logo abaixo.

Os próprios formulários explicam os campos: embaixo de cada um há um texto curto em cinza dizendo para que ele serve. Nas listas de opções (situação, tipo de pagamento, forma de cobrança, perfil), o link **O que significa cada opção?** abre a explicação de cada escolha.

### 3.1. Como digitar valores e datas

- **Valores em reais**: digite no padrão brasileiro, por exemplo `1.500,00`, `1500,00` ou `1500`. O símbolo `R$` pode ser digitado ou não.
- **Datas**: use o seletor de data do navegador. Na tela, as datas aparecem como dia/mês/ano.
- **Mês**: alguns filtros usam um seletor de mês e ano.

---

## 4. Preparando os cadastros básicos

Antes de começar a lançar atendimentos, confira se existem os **responsáveis** e os **tipos de serviço**.

### 4.1. Responsáveis

Menu **Cadastros → Responsáveis**.

O responsável é a pessoa que cuida do atendimento e que pode receber repasses. Ele **não precisa** ter usuário no sistema.

- **Cadastrar**: clique em novo, informe o nome (obrigatório, não pode repetir) e observações (opcional).
- **Editar**: renomear um responsável não apaga nada do histórico; os registros antigos passam a mostrar o nome novo.
- **Desativar**: o responsável desativado continua aparecendo nos registros antigos, mas **não pode ser escolhido** em atendimentos ou repasses novos. Nas listas de filtro ele aparece com a marcação "(inativo)".

### 4.2. Tipos de serviço

Menu **Cadastros → Tipos de serviço**.

O sistema já vem com: *Salário-maternidade*, *Aposentadoria por idade*, *Aposentadoria por incapacidade* e *Outros serviços*. Você pode criar outros, renomear ou desativar, sem precisar de programador. As regras de nome único e de desativação são as mesmas dos responsáveis.

### 4.3. Categorias de despesa e formas de pagamento

Menus **Cadastros → Categorias de despesa** e **Cadastros → Formas de pagamento**. São usados nas despesas do escritório (seção 11).

O sistema já vem com as categorias e as siglas do modelo de planilha enviado pela contabilidade (Sistema Domínio):

- **Categorias**, cada uma com o seu **histórico padrão**: Fornecedor ("Duplicata, ref. NF N."), Energia Eletrica ("Despesa c/ Energia, ref."), SAAE Agua, Telefonia Fixa, Manut Progr Informática, Fretes, Fardamento, Marketing/Publicidade, Honorários Contábil, Imposto Simples Federal, IRenda Retido na Fonte, Icms Dif de Aliquota, FGTS, INSS e Plano de Saude.
- **Formas de pagamento** (sigla da coluna FORMA PAGTO): BB, TRF BB, CX ECO, DEB CX ECO e CAIXA.

Os nomes foram mantidos exatamente como a contabilidade escreve, porque são esses textos que vão para a planilha. Se o escritório usar outro banco ou precisar de uma categoria nova, **combine o nome com a contabilidade** antes de cadastrar. Renomear vale também para as despesas antigas; para deixar de usar, desative. As regras de nome único e de desativação são as mesmas dos responsáveis.

---

## 5. Clientes

Menu **Cadastros → Clientes**.

### 5.1. Pesquisar

Digite parte do **nome** (não importa acento nem maiúscula) ou parte do **CPF** (a partir de 3 dígitos, com ou sem pontos). Use o filtro de situação para ver **Ativos** (padrão), **Inativos** ou **Todos**. A lista mostra 20 clientes por página.

### 5.2. Cadastrar ou editar

Campos:

- **Nome completo** (obrigatório).
- **CPF** (opcional). Se informado, o sistema confere se os dígitos são válidos e **impede CPF repetido**. Se já existir um cliente com aquele CPF, aparece um aviso com o link para o cadastro existente, para você conferir se não é a mesma pessoa.
- **Telefone ou contato** (opcional).
- **Observações** (opcional).
- **Data de cadastro** (não pode ser no futuro).

### 5.3. Ficha do cliente

Ao abrir um cliente, você vê os dados, a lista de **atendimentos** dele e o histórico de alterações do cadastro. Botões disponíveis:

- **Novo atendimento**: abre um atendimento já com este cliente escolhido.
- **Histórico financeiro**: relatório completo do cliente (veja a seção 12.5).
- **Editar**, **Desativar** ou **Ativar**.

Um cliente **desativado** continua com todo o histórico, mas não pode receber atendimentos novos até ser reativado.

---

## 6. Atendimentos

Menu **Rotina → Atendimentos**.

O atendimento representa um serviço jurídico prestado a um cliente. É a "pasta" onde ficam as previsões, os honorários e os repasses.

### 6.1. Abrir um atendimento

1. Clique em novo atendimento (ou use **Novo atendimento** na ficha do cliente).
2. Se veio pelo menu, primeiro pesquise e escolha o cliente (só aparecem clientes ativos).
3. Preencha:
   - **Tipo de serviço** e **Responsável** (só os ativos aparecem). Se o que você precisa ainda não existe, clique em **Novo** ao lado do campo: uma janela abre na própria tela, você informa o nome (e, se quiser, uma descrição/observação) e clica em **Cadastrar e selecionar**. O cadastro é criado e já fica escolhido no atendimento, sem perder o que você já tinha preenchido.
   - **Número do processo ou protocolo** (opcional).
   - **Data de abertura** (não pode ser no futuro).
   - **Situação**: *Aberto*, *Em andamento*, *Concluído* ou *Cancelado*.
   - **Observações** (opcional).

### 6.2. Página do atendimento

É a página mais completa do sistema. Ela mostra, de cima para baixo:

1. **Dados do atendimento**, com o botão **Editar**.
2. **Previsões de benefício do INSS**, com o botão **Nova previsão** e as ações de cada previsão.
3. **Honorários do escritório**: resumo do acordo (contratado, recebido, saldo) e o botão **Registrar acordo** ou **Abrir honorários**.
4. **Repasses internos**, com o botão **Novo repasse** e os totais previsto, pago e pendente.
5. **Histórico de alterações** do atendimento.

Se o atendimento estiver **Cancelado**, o sistema não deixa criar previsões, acordos ou repasses novos nele. Para isso, altere antes a situação do atendimento.

### 6.3. Pesquisar atendimentos

Na lista, filtre por nome do cliente, responsável, tipo de serviço e situação.

---

## 7. Previsões de benefício do INSS (o antigo caderno)

As previsões são cadastradas **dentro do atendimento** e acompanhadas pela **Agenda mensal**.

### 7.1. Situações de uma previsão

| Situação | Significado |
|---|---|
| **Previsto** | Situação inicial: a data prevista ainda não chegou. Quando chega, o sistema passa sozinho para *Aguardando confirmação*. |
| **Aguardando confirmação** | A data chegou ou passou e ainda falta confirmar com o INSS ou com o cliente. Volta sozinha para *Previsto* se a data for remarcada para o futuro. |
| **Liberado pelo INSS** | Liberação confirmada, com data e valor reais. **Só entra por "Confirmar liberação".** |
| **Adiado / não liberado** | O INSS não liberou na data prevista. |
| **Cancelado** | A previsão não vale mais. |

*Previsto* e *Aguardando confirmação* são chamadas de **pendentes**: são elas que geram os alertas de atraso.

### 7.2. Cadastrar uma previsão

Na página do atendimento, clique em **Nova previsão** e informe:

- **Data prevista de liberação**.
- **Valor estimado do benefício** (o que o cliente deve receber do INSS).
- **Tipo de pagamento**: *Primeiro pagamento*, *Retroativo / atrasados*, *Competência mensal* ou *Outro*.
- **Situação** (qualquer uma, menos "Liberado").
- **Observações** (opcional).

Um atendimento pode ter **várias previsões**, por exemplo uma para o primeiro pagamento e outra para os atrasados.

### 7.3. Confirmar a liberação

Quando o INSS liberar o benefício:

1. Clique em **Confirmar liberação** (na página do atendimento ou na Agenda, pelo botão **Atualizar**).
2. Informe a **data efetiva** (não pode ser no futuro) e o **valor confirmado** (maior que zero). O sistema já sugere a data de hoje e o valor estimado.
3. Salve. A previsão passa para **Liberado pelo INSS**.

A data prevista e o valor estimado **continuam guardados**: o sistema mantém separados o que era esperado e o que realmente aconteceu.

Previsões **canceladas** não podem ser confirmadas. Mude a situação antes, se for o caso.

### 7.4. Corrigir ou desfazer uma liberação

- **Corrigir liberação** (qualquer usuário): se a data ou o valor confirmados foram digitados errado, use o botão **Corrigir liberação**. O histórico guarda os valores antigos e os novos.
- **Desfazer** (somente administrador): apaga a data e o valor confirmados e devolve a previsão para *Aguardando confirmação*. O sistema pede confirmação e registra a operação.

### 7.5. Editar uma previsão

Pelo botão **Editar**, você altera data prevista, valor estimado, tipo de pagamento, situação e observações. Se a previsão já estiver liberada, a situação não pode ser trocada por aqui (use *Corrigir* ou peça ao administrador para *Desfazer*), e alterar o valor estimado **não** muda o valor confirmado. Abaixo do formulário aparece o histórico daquela previsão.

---

## 8. Agenda mensal

Menu **Rotina → Agenda mensal**. É a tela que substitui o caderno.

### 8.1. O que aparece

- Todas as previsões do mês escolhido, **ordenadas pela data prevista**, com cliente, tipo de serviço, responsável, tipo de pagamento, valor estimado e situação.
- Três totais no topo: **quantidade de previsões listadas**, **total estimado do INSS** (sem as canceladas) e **total confirmado**.
- Os botões **Mês anterior**, **Mês atual** e **Próximo mês** para navegar. Não é preciso "passar a limpo" a cada mês: o sistema organiza sozinho pela data.

### 8.2. Destaques visuais

Previsões pendentes recebem destaque, sempre com cor **e** texto:

- **Atrasada** (linha vermelha): a data prevista já passou e ela continua pendente.
- **Prevista para hoje** (linha amarela).
- **Próximos 7 dias** (linha amarela, com etiqueta de relógio).

Se existir **qualquer** previsão atrasada, em qualquer mês, aparece um aviso vermelho no topo com o link **Ver atrasadas**, que lista todas elas de uma vez.

### 8.3. Atualizar direto na agenda

Clique em **Atualizar** em uma linha para abrir, ali mesmo:

- **Confirmar liberação**: informe data efetiva e valor confirmado e clique no botão verde.
- **Mudar situação para**: por exemplo, marcar como *Adiado* ou *Cancelado*, e clique em **Salvar situação**. Se você escolher *Previsto* para uma data que já chegou, o sistema mantém *Aguardando confirmação* e avisa.
- Um link para **editar a previsão**, se for preciso mudar a data ou o valor previstos.

Depois de salvar, você volta para a mesma agenda, com os mesmos filtros.

O botão **Atendimento** abre a página completa do atendimento daquela previsão.

### 8.4. Filtros

- **Mês**, ou um **período** (de/até). Se o período for preenchido, ele substitui o mês.
- **Cliente** (parte do nome), **Responsável**, **Tipo de serviço** e **Situação**.
- **Somente atrasadas (qualquer mês)**.

Use **Limpar** para voltar à agenda do mês atual sem filtros.

---

## 9. Honorários

Os honorários são o que o cliente paga ao escritório. Cada atendimento pode ter **um** acordo de honorários.

### 9.1. Registrar o acordo

Na página do atendimento, clique em **Registrar acordo** e preencha:

- **Forma de cobrança**:
  - **Valor fixo**: o valor total contratado é obrigatório.
  - **Percentual**: informe o **percentual** (até 100%) e descreva a **base de cálculo** (ex.: "valores retroativos recebidos do INSS"). O valor contratado pode ficar em branco até ser conhecido.
  - **Outra condição**: o valor contratado pode ficar em branco até ser conhecido.
- **Valor total contratado**.
- **Data do acordo** (não pode ser no futuro).
- **Quantidade de parcelas** (opcional, apenas informativo).
- **Observações e condições acordadas** (opcional).

Enquanto o valor contratado estiver em branco, o saldo aparece como **"A definir"**. Quando souber o valor (por exemplo, depois que o INSS liberar os atrasados), edite o acordo e preencha.

O valor contratado **não pode** ser reduzido para menos do que já foi recebido.

### 9.2. Página de honorários

Menu **Financeiro do escritório → Honorários** lista todos os acordos com contratado, recebido e saldo. Filtre por cliente, responsável e situação do saldo: *Com saldo pendente*, *Quitados*, *Valor ainda não definido* ou *Todos*.

Ao abrir um acordo, você vê três totais no topo:

- **Valor contratado**.
- **Total recebido** (sem os recebimentos cancelados).
- **Saldo pendente** = contratado − recebido.

Abaixo ficam os dados do acordo, as **parcelas**, os **recebimentos** e o **histórico**.

### 9.3. Parcelas (opcional)

Use parcelas só quando o pagamento for dividido. Há duas formas:

- **Gerar parcelas**: informe o valor total (o sistema sugere o saldo pendente), a quantidade e o vencimento da primeira. O sistema divide em parcelas iguais, com vencimentos mensais, e coloca a diferença de centavos na última. Esse botão só aparece quando o acordo não tem parcelas ativas.
- **Adicionar parcela**: cria uma parcela de cada vez, com vencimento e valor livres.

Situações de uma parcela:

- **Aberta**: ainda não foi totalmente paga. Se o vencimento passou, aparece a etiqueta **Vencida** e a linha fica vermelha.
- **Paga**: o sistema marca automaticamente quando os recebimentos ligados a ela cobrem o valor.
- **Cancelada**: só o administrador cancela, com motivo, e apenas se a parcela não tiver recebimentos.

Só parcelas abertas podem ser editadas. Se a soma das parcelas ativas for diferente do valor contratado, aparece um aviso amarelo.

### 9.4. Registrar um recebimento

Quando o cliente pagar:

1. Clique em **Registrar recebimento** (pagamento avulso) ou em **Receber** na linha de uma parcela (o sistema já sugere o valor pendente daquela parcela).
2. Informe a **data** (não pode ser no futuro), o **valor**, a **forma de pagamento** (*Dinheiro*, *PIX*, *Transferência*, *Boleto* ou *Outro*) e observações.

Regras:

- Pagamentos **parciais** são permitidos.
- O valor não pode ultrapassar o saldo pendente. Se o acordo mudou, atualize antes o valor contratado.
- Recebimentos não podem ser editados nem apagados. Se houve erro, o **administrador cancela** o recebimento informando o motivo. Ele continua visível (riscado, com motivo, autor e data), o valor volta para o saldo pendente e, se estava ligado a uma parcela, ela volta a ficar aberta.

---

## 10. Repasses internos

Menu **Financeiro do escritório → Repasses internos**.

Repasses são comissões ou valores que o escritório paga a um responsável ou colaborador. Todos os valores são digitados manualmente; o sistema não aplica percentuais.

### 10.1. Lançar um repasse

Na página do atendimento, clique em **Novo repasse** e informe:

- **Destinatário** (um responsável ativo; o sistema já sugere o responsável do atendimento).
- **Motivo** (ex.: "Comissão pela indicação").
- **Valor previsto** e **data prevista** para o pagamento.
- **Observações** (opcional).

### 10.2. Situações e ações

- **Pendente**: ainda não foi pago. Se a data prevista passou, aparece **Atrasado**. Pode ser **editado**, **pago** ou **cancelado** por qualquer usuário.
- **Pago**: registrado pelo botão **Registrar pagamento**, com data (não pode ser no futuro) e valor efetivamente pago (o sistema sugere o valor previsto). Não pode mais ser editado. Só o **administrador** pode cancelar um repasse pago.
- **Cancelado**: exige motivo de pelo menos 5 caracteres. O repasse continua registrado, com o motivo, quem cancelou e quando.

A lista de repasses e a página do atendimento mostram os totais **previsto**, **pago** e **pendente**, sempre sem os cancelados.

---

## 11. Despesas do escritório e envio à contabilidade

Menu **Financeiro do escritório → Despesas do escritório**.

Aqui ficam os gastos avulsos do escritório (energia, água, telefone, impostos, fornecedores etc.). Todo mês a contabilidade precisa receber a planilha **"Contas Pagas"** do mês anterior **até o dia 20**. Em vez de preencher a planilha à mão, você lança cada despesa no sistema ao pagá-la e, no fim, baixa a planilha pronta, no mesmo modelo usado pela contabilidade (Sistema Domínio).

A tela mostra um mês por vez (navegue com as setas), com o total do mês, o total por categoria e a lista de despesas. Os filtros por **categoria** e **fornecedor** só mudam a lista; o quadro de envio e a planilha sempre consideram o mês inteiro.

### 11.1. Lançar uma despesa

Clique em **Nova despesa** e informe:

- **Data do pagamento** (não pode estar no futuro: lance a despesa depois de pagá-la). É ela que define em qual mês a despesa entra.
- **Categoria**. Ao escolher, o campo **Histórico** é preenchido com o texto padrão da categoria (ex.: "Despesa c/ Energia, ref."). Complete com a referência, por exemplo "Despesa c/ Energia, ref. 09/2026". Se você já tiver digitado outro texto, o sistema não o substitui.
- **Forma de pagamento** (a sigla da conta usada: BB, TRF BB, CX ECO...).
- **Nº da nota ou referência** (opcional: nota fiscal, guia, fatura).
- **Valor pago**.
- **Fornecedor**: quem recebeu o pagamento. O campo sugere os fornecedores já usados, para manter o nome sempre igual.
- **Observações** (opcional; uso interno, não vão para a planilha).

O botão **Salvar e lançar outra** grava e abre um formulário novo no mesmo mês, útil para lançar várias contas de uma vez.

Uma despesa lançada errada pode ser **editada** ou **cancelada** (com motivo de pelo menos 5 caracteres) por qualquer usuário, enquanto o mês não tiver sido enviado. A despesa cancelada continua registrada com o motivo, mas não entra nos totais nem na planilha.

### 11.2. Prazo e aviso no painel

| Situação | Quando aparece |
|---|---|
| **Em andamento** | Mês atual: ainda recebendo lançamentos. |
| **Aguardando envio** | Mês já terminou, mas ainda falta tempo até o prazo. |
| **Prazo próximo** | A partir do dia 15 do mês seguinte, até o dia 20. |
| **Atrasado** | Passou do dia 20 do mês seguinte e o mês não foi marcado como enviado. |
| **Enviado** | O mês foi marcado como enviado à contabilidade. |

A partir do dia 15, o **painel inicial** mostra um aviso com o mês a enviar e o prazo, com o atalho **Conferir e enviar**. Meses antigos que tenham despesas e não foram marcados como enviados também aparecem, como atrasados.

### 11.3. Baixar a planilha e marcar como enviado

1. No mês a enviar, confira a lista e os totais.
2. Clique em **Baixar planilha da contabilidade**. O arquivo (`contas-pagas-AAAA-MM.xlsx`) tem a aba "Contas Pagas" com as mesmas colunas do modelo da contabilidade (DATA, CATEGORIA, FORMA PAGTO, HISTORICO, Nº NOTA/ REFERENCIA DO PAGAMENTO, VALOR e FORNECEDOR, além da coluna oculta VALOR NEGATIVO), uma linha por despesa ativa, em ordem de data.
3. Envie o arquivo à contabilidade (e-mail, WhatsApp etc.). O sistema não envia sozinho.
4. Volte à tela e clique em **Marcar como enviado**.

Ao marcar como enviado, o sistema guarda quem marcou, quando, a quantidade de despesas e o total enviado, e o mês fica **travado**: não é possível lançar, editar nem cancelar despesas com data naquele mês. O mês atual não pode ser marcado como enviado, porque ainda não terminou.

Cada download da planilha fica registrado no histórico de operações.

### 11.4. Corrigir um mês já enviado

Se for preciso corrigir algo depois do envio, um **administrador** usa o botão **Reabrir mês** na própria tela, informando o motivo (pelo menos 5 caracteres). O mês volta a aceitar alterações, e a tela mostra quem reabriu, quando e por quê. Depois de corrigir, baixe a planilha de novo, envie a versão corrigida à contabilidade e marque como enviado outra vez.

---

## 12. Painel inicial e relatórios

### 12.1. Painel (Início)

O painel mostra os números de um mês (navegue com as setas) ou de um período escolhido. Ele é dividido em dois blocos que nunca se misturam.

**Valores do cliente (benefício do INSS)**, que não são receita do escritório:

- **Previsões no período**: quantidade de previsões com data prevista no período, sem as canceladas.
- **Valor estimado do INSS**: soma do valor estimado dessas previsões.
- **Liberações confirmadas**: soma dos valores confirmados cuja **data efetiva** caiu no período.
- **Pendentes de confirmação**: previsões do período ainda como *Previsto* ou *Aguardando confirmação*.
- **Atrasadas sem confirmação**: pendentes do período cuja data já passou. Se houver atrasadas em outros meses, o total geral aparece ao lado.

**Valores do escritório:**

- **Honorários contratados**: soma dos acordos firmados no período. Acordos com valor "a definir" não entram na soma, e o painel avisa quantos são.
- **Honorários recebidos**: o que entrou no caixa no período, sem os cancelados.
- **Honorários pendentes**: saldo em aberto de todos os acordos com valor definido, na posição do último dia do período.
- **Repasses pendentes**: repasses não pagos com data prevista até o fim do período, incluindo os atrasados.
- **Repasses pagos**: soma do valor pago com data de pagamento no período.

Vários indicadores têm atalhos ("Ver na agenda", "Ver atrasadas", "Ver acordos com saldo", "Ver repasses pendentes").

### 12.2. Relatórios

Menu **Consultas → Relatórios**. Há cinco abas:

1. **Previsões de benefício**: previsões com data prevista no período, com totais estimado e confirmado.
2. **Recebimentos de honorários**: recebimentos com data no período. A situação pode ser filtrada entre confirmados e cancelados.
3. **Acordos de honorários**: acordos firmados no período, com o recebido e o saldo atual. Filtre por saldo pendente, quitados ou valor não definido.
4. **Repasses internos**: repasses com data prevista no período, com totais. Aqui o filtro de responsável vale para o **destinatário** do repasse.
5. **Histórico por cliente**: veja a seção 12.5.

Filtros comuns: **mês** ou **período**, **responsável**, **tipo de serviço** e **situação**. As setas de mês mantêm os filtros escolhidos.

### 12.3. Exportar para Excel (CSV)

Em qualquer relatório, o botão **Exportar CSV** baixa um arquivo com **exatamente os mesmos filtros** da tela. O arquivo abre direto no Excel em português (separador `;`, vírgula decimal e datas dia/mês/ano).

Por segurança, o CSV **não inclui CPF nem telefone**, e cada exportação fica registrada no histórico de operações. Salve os arquivos exportados **fora** da pasta do sistema e em local com acesso restrito.

### 12.4. Imprimir

O botão **Imprimir** imprime o relatório da tela sem o menu e sem os filtros.

### 12.5. Histórico financeiro do cliente

Acesse pela aba **Histórico por cliente** (pesquise o cliente, inclusive inativos) ou pelo botão **Histórico financeiro** na ficha do cliente. Ele mostra, para cada atendimento do cliente, as previsões, o resumo dos honorários e os repasses, sempre em blocos separados.

---

## 13. Administração (somente administradores)

### 13.1. Usuários

Menu **Administração → Usuários**.

- **Cadastrar**: login (único, sem diferenciar maiúsculas), nome, perfil (*Administrador* ou *Operador*), senha e confirmação. A senha precisa ter **pelo menos 8 caracteres**.
- **Editar**: altere login, nome e perfil. Para **redefinir a senha**, preencha a nova senha; deixando em branco, a senha atual é mantida.
- **Desativar/Ativar**: o usuário desativado não consegue entrar, mas o histórico do que ele fez continua guardado.

Proteções do sistema:

- Você não pode desativar o seu próprio usuário.
- O sistema sempre precisa ter **pelo menos um administrador ativo**: não é possível desativar ou rebaixar o último.

### 13.2. Histórico de operações

Menu **Administração → Histórico de operações**.

Lista, da mais recente para a mais antiga, todas as operações relevantes: cadastros, alterações, ativações, desativações, confirmações, cancelamentos e exportações. Cada linha mostra data e hora, usuário, tipo de ação e uma descrição legível (por exemplo, "valor estimado de R$ 1.500,00 para R$ 1.480,00"). Dá para filtrar pelo tipo de registro (Cliente, Previsão de benefício, Repasse etc.).

O histórico **não** grava senhas nem CPF completo.

---

## 14. Rotinas sugeridas

### 14.1. Todo dia

1. Abra a **Agenda mensal**.
2. Se aparecer o aviso vermelho de atrasadas, clique em **Ver atrasadas** e trate cada uma: confirme a liberação, marque como *Adiado* ou remarque a data editando a previsão. (Elas já estarão como *Aguardando confirmação*, porque o sistema muda sozinho quando a data chega.)
3. Confira as linhas destacadas como **Prevista para hoje** e **Próximos 7 dias**.

### 14.2. Quando chega um cliente novo

1. **Clientes** → pesquise pelo nome ou CPF para não duplicar o cadastro.
2. Cadastre o cliente, se ainda não existir.
3. Na ficha do cliente, clique em **Novo atendimento**.
4. No atendimento, registre a **previsão** de liberação e o **acordo de honorários**.
5. Se houver comissão combinada, lance o **repasse**.

### 14.3. Quando o INSS libera o benefício

1. Na Agenda (botão **Atualizar**) ou no atendimento, clique em **Confirmar liberação** e informe data e valor reais.
2. Se o acordo de honorários era percentual e o valor estava "a definir", edite o acordo e informe o valor contratado.
3. Gere as parcelas, se o cliente for pagar em partes.

### 14.4. Quando o cliente paga honorários

Abra o acordo (pelo atendimento ou pelo menu **Honorários**) e clique em **Receber** na parcela, ou em **Registrar recebimento**.

### 14.5. No fechamento do mês

1. Confira o **Painel** do mês.
2. Em **Relatórios**, exporte o CSV de recebimentos de honorários e de repasses do mês, se for preciso para a contabilidade.
3. Em **Repasses internos**, filtre os *Pendentes* e registre os pagamentos feitos.
4. Em **Despesas do escritório**, confira se todas as contas pagas no mês foram lançadas. Até o dia 20 do mês seguinte, baixe a planilha, envie à contabilidade e clique em **Marcar como enviado** (seção 11.3).

---

## 15. Dúvidas e mensagens comuns

**"O cliente está inativo. Reative o cadastro antes de abrir um novo atendimento."**
Abra a ficha do cliente e clique em **Ativar**.

**"O responsável / tipo de serviço está inativo e não pode ser usado em novos registros."**
Reative-o em **Cadastros**, ou escolha outro.

**"Este atendimento está cancelado..."**
Edite o atendimento e mude a situação antes de lançar previsões, honorários ou repasses.

**"Para marcar como liberado, use a ação Confirmar liberação..."**
A situação "Liberado" não aparece na lista de situações de propósito: é preciso informar data e valor reais.

**"O valor ultrapassa o saldo pendente..."**
O recebimento é maior do que falta receber. Confira o valor ou atualize o valor contratado do acordo.

**"O valor contratado não pode ser menor que o total já recebido."**
Para reduzir o valor, o administrador precisa antes cancelar algum recebimento lançado por engano.

**"Esta parcela tem recebimentos. Cancele os recebimentos antes de cancelar a parcela."**
Cancele primeiro os recebimentos ligados a ela (administrador).

**"Repasse já pago só pode ser cancelado por um administrador."**
Peça a um administrador.

**"Informe o motivo do cancelamento (pelo menos 5 caracteres)."**
Todo cancelamento financeiro exige um motivo de 5 a 500 caracteres.

**"As despesas de ... já foram enviadas à contabilidade. Peça a um administrador para reabrir o mês antes de alterar."**
O mês está travado porque foi marcado como enviado. Veja a seção 11.4.

**"O mês de ... ainda não terminou. A planilha só pode ser marcada como enviada a partir do mês seguinte."**
A planilha pode ser baixada a qualquer momento para conferência, mas só é marcada como enviada depois que o mês termina.

**"A data do pagamento não pode estar no futuro..."**
Lance a despesa no dia em que ela for paga, ou depois.

**Lancei algo errado. Posso apagar?**
Não. Para manter a auditoria, lançamentos financeiros não são apagados. Cadastros são **desativados**; previsões são **canceladas** ou **corrigidas**; parcelas, recebimentos, repasses e despesas são **cancelados com motivo**.

**A previsão passou da data e mudou para "Aguardando confirmação". Quem mudou?**
O próprio sistema (veja a seção 1.3). Ela aparece como **Atrasada** na agenda até alguém confirmar a liberação, marcar como adiada ou cancelada, ou remarcar a data. O sistema nunca marca uma previsão como liberada sozinho.

---

## 16. Cuidados com dados pessoais (LGPD)

- Cada pessoa deve usar o **próprio usuário**. Não compartilhe senhas.
- Clique em **Sair** ao terminar, principalmente em computadores compartilhados.
- Arquivos CSV e impressões contêm nomes e valores de clientes: guarde em local restrito e descarte com cuidado.
- O backup do banco contém CPF, telefone e valores. O procedimento de backup e restauração está no `README.md`.

---

## Anexo técnico: visão geral do projeto

Esta seção é para quem vai dar manutenção no sistema.

**Tecnologias:** Java 21, Spring Boot 4 (Spring MVC, Spring Data JPA, Spring Security, Bean Validation), Thymeleaf com Bootstrap 5, PostgreSQL com migrações Flyway e testes com JUnit 5 e Mockito.

**Organização do código** (`src/main/java/br/com/escritorio/financeiro`):

| Pacote | Conteúdo |
|---|---|
| `domain` | Entidades JPA e enums de situação. As regras que dependem só da própria entidade ficam aqui (ex.: `PrevisaoBeneficio.confirmarLiberacao`, `Repasse.registrarPagamento`, `CalculoHonorarios`). |
| `service` | Regras de negócio, validações e registro de auditoria. Cada operação de escrita chama `AuditoriaService.registrar`. Erros de regra lançam `RegraNegocioException`, exibida no formulário. |
| `repository` | Interfaces Spring Data com as consultas de pesquisa, relatórios e somas do painel. |
| `web` | Controllers MVC, um por módulo, mais utilitários de CSV (`Csv`), de retorno seguro (`Retorno`) e a geração da planilha da contabilidade (`PlanilhaContasPagas`, com Apache POI). |
| `dto` | Formulários (`*Form`), filtros (`Filtro*`) e objetos de leitura para telas e relatórios. |
| `security` | Configuração do Spring Security, carregamento de usuários e criação do primeiro administrador (`AdminInicialRunner`). |
| `config` | Conversor de valores no padrão brasileiro, relógio injetável (`Clock`, usado nos testes) e configuração web. |
| `demo` | Dados fictícios carregados só no profile `demo`. |

**Banco de dados:** as tabelas são criadas pelas migrações em `src/main/resources/db/migration` (`V1__usuarios.sql` até `V10__despesas.sql`). O Hibernate roda em modo `validate`, ou seja, nunca altera o banco sozinho. Mudanças de estrutura sempre entram como uma nova migração.

**Permissões:** as rotas `/usuarios/**` e `/auditoria/**`, e os POSTs de desfazer liberação, de cancelar parcela ou recebimento e de reabrir um mês de despesas já enviado, exigem o papel `ADMIN` em `SecurityConfig` e também por `@PreAuthorize` nos controllers. O cancelamento de repasse pago é verificado no `RepasseService`.

**Telas:** os templates Thymeleaf ficam em `src/main/resources/templates`, um diretório por módulo, todos usando o `layout.html` comum.

# Desenvolvimento de um Sistema Web de Gestão Financeira para um Escritório de Advocacia

## 1. Papel e objetivo

Atue como um engenheiro de software sênior especializado em Java, Spring Boot, desenvolvimento web, modelagem de banco de dados, segurança da informação e sistemas de gestão.

Quero desenvolver um sistema web interno para um escritório de advocacia, com foco inicial na gestão de clientes, serviços jurídicos, previsões de liberação de benefícios do INSS e controle dos valores financeiros relacionados a cada atendimento.

O sistema substituirá um caderno utilizado atualmente pelo escritório para registrar os clientes que estão próximos de receber benefícios previdenciários. Nesse caderno são anotados principalmente o nome do cliente, o responsável pelo atendimento, a data prevista para liberação do benefício e o valor que o cliente deverá receber do INSS.

O objetivo é digitalizar esse processo, facilitar a organização mensal e permitir que o escritório acompanhe separadamente o valor destinado ao cliente, os honorários advocatícios, os pagamentos recebidos pelo escritório e os eventuais repasses ou comissões internas.

O sistema deverá ter uma interface simples e intuitiva, adequada para funcionários que não possuem conhecimentos técnicos.

## 2. Tecnologias obrigatórias

Utilize as seguintes tecnologias:

* **Linguagem:** Java 21 LTS.
* **Framework principal:** Spring Boot, utilizando uma versão estável compatível com o Java escolhido.
* **Gerenciador de dependências:** Maven.
* **Aplicação web:** Spring MVC.
* **Interface:** Thymeleaf, HTML, CSS, Bootstrap 5 e JavaScript quando necessário.
* **Persistência:** Spring Data JPA e Hibernate.
* **Banco de dados:** PostgreSQL.
* **Autenticação e autorização:** Spring Security.
* **Validação:** Jakarta Bean Validation.
* **Migrações do banco:** Flyway.
* **Testes:** JUnit 5, Mockito e ferramentas de teste do Spring Boot.
* **Ambiente local:** Docker Compose para facilitar a execução do PostgreSQL, quando apropriado.

Utilize uma arquitetura em camadas, com responsabilidades bem definidas entre controllers, services, repositories, entidades, DTOs, validações e templates.

Evite complexidade arquitetural desnecessária. O projeto deve ser bem organizado, testável e fácil de compreender e manter.

Não utilize o painel administrativo genérico como interface principal. Crie uma interface própria para a rotina do escritório.

## 3. Regras de negócio fundamentais

O sistema deve tratar como valores distintos:

1. **Valor do benefício do INSS:** valor que o cliente deverá receber do INSS. Esse é o principal valor registrado atualmente no caderno.
2. **Valor dos honorários contratados:** valor que o cliente deverá pagar ao escritório pelos serviços jurídicos, conforme o contrato celebrado.
3. **Honorários efetivamente recebidos:** valores que já entraram no caixa do escritório.
4. **Saldo de honorários pendentes:** diferença entre os honorários contratados e os pagamentos efetivamente recebidos, respeitando os ajustes registrados.
5. **Comissões ou repasses internos:** valores eventualmente destinados ao responsável pelo cliente ou a outro colaborador, quando esse tipo de pagamento existir no escritório.

Esses valores devem aparecer em campos, relatórios e indicadores separados. O valor do benefício do INSS pertence à categoria financeira do cliente e não deve ser contabilizado automaticamente como receita do escritório.

Não presuma que os honorários correspondem a uma porcentagem fixa do benefício. O valor deve ser registrado de acordo com o contrato. Quando houver cobrança percentual, a base de cálculo, a porcentagem e as condições deverão ser explicitamente configuradas.

Da mesma forma, não presuma uma regra fixa para comissões e repasses. Na primeira versão, permita o registro separado desses valores, sem inventar percentuais ou regras de cálculo.

## 4. Módulos e funcionalidades

### 4.1. Autenticação e usuários

Implemente:

* Tela de login.
* Logout.
* Senhas armazenadas com hash seguro.
* Proteção das páginas internas contra acesso não autenticado.
* Perfis de acesso, inicialmente com administrador e operador.
* Permissões para limitar operações administrativas a usuários autorizados.
* Cadastro, edição, ativação e desativação de usuários, conforme o perfil.
* Histórico de operações relevantes, identificando o usuário responsável pela alteração.

Não permita cadastro público de usuários.

Não grave senhas padrão diretamente no código. A criação do primeiro administrador deverá ser feita por meio de uma configuração segura ou procedimento documentado.

### 4.2. Cadastro de clientes

Implemente o cadastro de clientes com os seguintes campos:

* Nome completo.
* CPF, opcional quando não for necessário para o registro.
* Telefone ou contato, opcional.
* Observações administrativas, com acesso restrito quando necessário.
* Data de cadastro.
* Situação do cadastro: ativo ou inativo.

O nome do cliente deve ser obrigatório.

Implemente funcionalidades para cadastrar, editar, consultar, pesquisar e visualizar o histórico financeiro do cliente.

Evite criar registros duplicados por engano e permita consultar se já existe um cliente com o mesmo CPF, quando informado.

Não armazene documentos pessoais ou médicos desnecessários nesta primeira versão.

### 4.3. Cadastro de responsáveis

Crie um módulo para registrar os responsáveis pelos clientes e atendimentos.

Campos mínimos:

* Nome do responsável.
* Situação: ativo ou inativo.
* Observações, opcionais.

O responsável por um atendimento deverá ser selecionado a partir desse cadastro.

O responsável pelo cliente não precisa ser obrigatoriamente um usuário com acesso ao sistema.

### 4.4. Tipos de serviço jurídico

Crie um cadastro de tipos de serviço, permitindo cadastrar, editar, ativar e desativar opções.

Inclua inicialmente:

* Salário-maternidade.
* Aposentadoria por idade.
* Aposentadoria por incapacidade.
* Outros serviços.

Os tipos deverão ser configuráveis, sem exigir alterações no código para cada novo serviço cadastrado.

### 4.5. Atendimentos e processos

Um cliente pode ter mais de um atendimento ou serviço. Por isso, não associe todos os registros financeiros diretamente ao cadastro do cliente.

Cada atendimento deverá conter:

* Cliente.
* Tipo de serviço.
* Responsável pelo atendimento.
* Número do processo ou protocolo, opcional.
* Data de abertura ou cadastro.
* Situação do atendimento.
* Observações.
* Data de criação e última atualização.

A interface deverá permitir consultar todos os atendimentos vinculados a um cliente e acessar os respectivos registros financeiros.

### 4.6. Previsões de liberação dos benefícios do INSS

Este módulo é prioritário, pois representa a principal rotina que atualmente é controlada no caderno.

Cada previsão deverá conter:

* Atendimento relacionado.
* Cliente, apresentado automaticamente a partir do atendimento.
* Responsável, apresentado automaticamente a partir do atendimento.
* Tipo de serviço, apresentado automaticamente a partir do atendimento.
* Data prevista para liberação do benefício.
* Valor estimado que o cliente receberá do INSS.
* Valor efetivamente confirmado, quando conhecido.
* Data efetiva de liberação, quando confirmada.
* Tipo de pagamento, como primeiro pagamento, valores retroativos ou atrasados, competência mensal ou outro.
* Situação da previsão.
* Observações.

As situações devem permitir identificar pelo menos:

* Previsto.
* Aguardando confirmação.
* Liberado pelo INSS.
* Adiado ou não liberado na data prevista.
* Cancelado.

Permita cadastrar, editar, consultar e atualizar previsões.

Uma previsão não deve ser marcada automaticamente como liberada somente porque sua data chegou. A confirmação deve depender de uma ação explícita de um usuário autorizado.

Permita múltiplas previsões ou liberações para um mesmo atendimento quando necessário.

O sistema deverá organizar esses registros automaticamente por mês e ano da data prevista, evitando que o funcionário precise copiar manualmente todos os registros para um novo caderno no início de cada mês.

Mantenha a data prevista separada da data efetiva. O valor estimado também deverá ser separado do valor confirmado, para que alterações na previsão não apaguem o histórico do que foi informado anteriormente.

Não presuma que o sistema possui acesso direto ao INSS. Na primeira versão, as informações serão inseridas e atualizadas manualmente.

### 4.7. Controle dos honorários advocatícios

Implemente um módulo financeiro vinculado ao atendimento, separado das previsões de benefícios do INSS.

Cada acordo de honorários deverá permitir registrar:

* Atendimento relacionado.
* Forma de cobrança: valor fixo, percentual ou outra condição.
* Valor total contratado.
* Percentual acordado, quando aplicável.
* Base de cálculo, quando aplicável.
* Quantidade de parcelas, quando aplicável.
* Observações e condições acordadas.
* Data do registro.

O sistema deverá permitir registrar os recebimentos individualmente, com:

* Data do recebimento.
* Valor recebido.
* Forma de pagamento.
* Observações.

Calcule automaticamente, com base nos registros, o total recebido e o saldo de honorários pendente.

Quando houver parcelamento, permita acompanhar cada parcela individualmente, incluindo vencimento, valor e situação.

Não confunda a data de liberação do benefício pelo INSS com a data de vencimento ou recebimento dos honorários.

Permita corrigir registros financeiros com as permissões adequadas e mantenha um histórico das alterações. Evite apagar definitivamente lançamentos financeiros já confirmados; prefira cancelamento justificado ou outro mecanismo auditável.

### 4.8. Comissões e repasses internos

Implemente um módulo separado e opcional para registrar comissões ou repasses, caso o escritório utilize esse procedimento.

Cada lançamento deverá conter:

* Atendimento relacionado.
* Responsável ou destinatário do repasse.
* Descrição do motivo.
* Valor previsto.
* Valor efetivamente pago.
* Data prevista para pagamento.
* Data efetiva de pagamento.
* Situação: pendente, pago ou cancelado.
* Observações.

Na primeira versão, os valores podem ser lançados manualmente. Não aplique porcentagens padrão nem calcule comissões automaticamente sobre o benefício do cliente.

Caso seja necessário adicionar regras automatizadas futuramente, a arquitetura deverá permitir configurar a regra, a base de cálculo e a porcentagem ou o valor fixo de maneira explícita.

Os repasses não deverão ser somados aos honorários recebidos nem ao benefício do INSS como se fossem a mesma categoria financeira.

### 4.9. Painel inicial

Crie um dashboard após o login, com visual simples e profissional.

Inclua indicadores separados:

* Quantidade de previsões de benefícios para o mês.
* Valor total estimado de benefícios do INSS.
* Valor de benefícios com liberação confirmada, conforme os registros disponíveis.
* Previsões pendentes de confirmação.
* Previsões cuja data já passou e ainda não foram confirmadas.
* Total de honorários contratados.
* Total de honorários recebidos pelo escritório.
* Total de honorários pendentes.
* Total de comissões ou repasses pendentes, quando aplicável.

Não misture os valores do benefício do cliente com as receitas do escritório.

Todos os indicadores devem considerar filtros de período e os registros correspondentes. Documente claramente o que cada indicador representa.

### 4.10. Agenda e lista mensal

Implemente uma tela de controle mensal que substitua visualmente o caderno.

A tela deverá ter:

* Seletor de mês e ano.
* Lista ou tabela ordenada pela data prevista.
* Nome do cliente.
* Tipo de serviço.
* Responsável.
* Data prevista de liberação.
* Valor estimado do benefício do INSS.
* Situação.
* Acesso rápido aos detalhes do atendimento.
* Ação para atualizar a situação e informar a data e o valor efetivos.

Adicione filtros por:

* Nome do cliente.
* Responsável.
* Tipo de serviço.
* Período.
* Situação do benefício.

Destaque visualmente previsões próximas da data e previsões atrasadas ainda não confirmadas.

Não dependa apenas de cores para indicar as situações: apresente também textos ou rótulos claros.

### 4.11. Relatórios

Implemente relatórios por mês e período, com filtros por responsável, tipo de serviço e situação.

Inclua:

* Previsões de liberação de benefícios.
* Benefícios previstos e confirmados.
* Honorários contratados, recebidos e pendentes.
* Comissões ou repasses previstos, pagos e pendentes.
* Histórico financeiro por cliente e atendimento.

Disponibilize exportação CSV dos relatórios principais, respeitando os filtros aplicados e os controles de acesso.

Os relatórios deverão separar claramente os valores pertencentes ao cliente e os valores financeiros do escritório.

## 5. Interface e experiência do usuário

A interface deverá estar em português do Brasil.

Utilize:

* Bootstrap 5.
* Layout responsivo para computadores, tablets e celulares.
* Menu lateral ou navegação simples.
* Formulários claros e organizados por seções.
* Tabelas com pesquisa e filtros.
* Botões com nomes objetivos.
* Mensagens amigáveis de sucesso, erro e validação.
* Confirmação para operações destrutivas ou cancelamentos.
* Formatação monetária em reais, no padrão brasileiro.
* Datas no formato dia/mês/ano.

Priorize a facilidade de uso por funcionários que atualmente dependem de um caderno. Não exiba excesso de informações técnicas na interface.

Crie páginas próprias para dashboard, clientes, responsáveis, atendimentos, previsões de benefícios, honorários, recebimentos, comissões ou repasses e relatórios.

## 6. Segurança e privacidade

O sistema trabalhará com dados pessoais e informações relacionadas a processos jurídicos. Portanto:

* Aplique autenticação e autorização nas rotas protegidas.
* Valide os dados no backend, mesmo quando houver validação no frontend.
* Utilize consultas parametrizadas e os mecanismos seguros do Spring Data JPA.
* Configure proteção contra CSRF conforme os recursos do Spring Security.
* Armazene senhas com algoritmo de hash apropriado.
* Evite expor dados pessoais em logs.
* Registre as alterações relevantes de dados financeiros.
* Restrinja o acesso a informações de acordo com o perfil do usuário.
* Documente como realizar backups e restauração do banco.
* Adote medidas compatíveis com os princípios e obrigações aplicáveis da LGPD.

Não armazene segredos, senhas de banco ou chaves diretamente no repositório. Utilize variáveis de ambiente e forneça um arquivo `.env.example` sem credenciais reais.

## 7. Banco de dados e integridade

Modele o banco de forma relacional, com chaves primárias, chaves estrangeiras, restrições e índices apropriados.

Considere entidades separadas para:

* Usuários.
* Clientes.
* Responsáveis.
* Tipos de serviço.
* Atendimentos.
* Previsões e liberações de benefícios.
* Acordos de honorários.
* Recebimentos de honorários.
* Comissões ou repasses.
* Histórico de auditoria, quando apropriado.

Utilize tipos monetários adequados, como `BigDecimal` no Java e `NUMERIC` no PostgreSQL. Não utilize `float` ou `double` para armazenar valores financeiros.

Implemente migrações do banco com Flyway.

Defina regras para impedir valores inválidos, datas incoerentes e referências a registros inexistentes.

Uma alteração no nome do cliente ou do responsável não deverá apagar seu histórico financeiro.

## 8. Testes

Implemente testes unitários e de integração para os principais fluxos.

Teste, no mínimo:

* Cadastro e validação de clientes.
* Cadastro de atendimentos.
* Registro e atualização de previsões de benefícios.
* Separação entre valor do benefício, honorários e comissões.
* Registro de recebimentos parciais.
* Cálculo do saldo de honorários.
* Filtros mensais e por responsável.
* Controle de acesso a páginas protegidas.
* Impedimento de acesso a funções administrativas sem autorização.
* Regras de validação financeira.

Execute os testes e a compilação ao final de cada etapa importante. Corrija os erros encontrados antes de considerar uma funcionalidade concluída.

## 9. Organização do desenvolvimento

Siga estas etapas:

**Etapa 1 — Análise e planejamento**

* Se já existir um projeto no ambiente, analise sua estrutura antes de alterar arquivos.
* Se o ambiente estiver vazio, crie o projeto conforme a stack especificada.
* Apresente a arquitetura, as entidades, os relacionamentos e a estrutura de pastas escolhidos.
* Identifique decisões de negócio que precisem ser configuráveis, sem inventar regras financeiras.

**Etapa 2 — Fundação do projeto**

* Configure o Spring Boot, Maven, PostgreSQL, JPA, Flyway, Thymeleaf e Spring Security.
* Configure as variáveis de ambiente.
* Crie as migrações iniciais.
* Implemente autenticação e layout base.

**Etapa 3 — Cadastros e atendimentos**

* Implemente clientes, responsáveis, tipos de serviço e atendimentos.
* Crie os formulários, listagens, buscas e validações.

**Etapa 4 — Controle de benefícios**

* Implemente previsões, datas, valores, estados, filtros mensais e atualização das liberações.
* Desenvolva a tela que substituirá o caderno.

**Etapa 5 — Financeiro**

* Implemente os acordos de honorários, recebimentos, saldos e o módulo separado de comissões ou repasses.

**Etapa 6 — Dashboard e relatórios**

* Implemente os indicadores financeiros separados e os relatórios mensais.
* Adicione a exportação CSV.

**Etapa 7 — Testes e documentação**

* Execute os testes.
* Corrija problemas de compilação e integração.
* Documente como configurar, executar, testar e manter o sistema.

Se estiver trabalhando diretamente em um ambiente com arquivos, faça as alterações no projeto, em vez de somente apresentar exemplos de código.

Não tente implementar tudo por meio de uma única classe ou arquivo excessivamente grande. Desenvolva funcionalidades completas e integradas, mantendo cada classe com uma responsabilidade clara.

## 10. Entrega esperada

Ao concluir, o projeto deverá conter:

* Código-fonte organizado.
* Banco de dados configurado.
* Migrações versionadas.
* Telas funcionais para os módulos prioritários.
* Autenticação e autorização.
* Testes automatizados.
* Arquivo de configuração de exemplo para as variáveis de ambiente.
* `README.md` em português com instruções de instalação, configuração, execução e testes.
* Dados de demonstração fictícios, se forem úteis para validar a interface, claramente separados dos dados reais.

Não declare que uma funcionalidade está concluída sem ter implementado e, quando possível, testado seu funcionamento.

Priorize primeiro uma versão funcional para cadastrar clientes, registrar previsões de liberação dos benefícios e organizar a agenda mensal. Depois integre o controle completo de honorários e repasses.

O resultado deve ser um sistema real, extensível e utilizável pelo escritório, e não apenas um protótipo visual ou uma coleção de exemplos de código.

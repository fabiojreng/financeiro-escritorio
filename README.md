# Financeiro do Escritório

Sistema web interno para controlar clientes, atendimentos, previsões de liberação de benefícios do INSS, honorários e repasses de um escritório de advocacia. Substitui o caderno de previsões e mantém separados três tipos de valor que nunca se somam:

- **benefício do INSS**: pertence ao cliente;
- **honorários**: o que o cliente paga ao escritório;
- **repasses internos**: o que o escritório paga a colaboradores e parceiros.

## Pré-requisitos

- Java 21 ou superior
- PostgreSQL instalado localmente (porta padrão `5432`); os exemplos usam a versão 18 em `C:\Program Files\PostgreSQL\18`
- Um cliente SQL, como o DBeaver (opcional, para administrar o banco)

O Maven não precisa estar instalado: use o `mvnw` (Linux/macOS) ou `mvnw.cmd` (Windows) que acompanha o projeto.

## O que não entra no repositório

O `.gitignore` já bloqueia estes itens; confira antes de cada commit que nenhum deles aparece no `git status`:

- `.env`, com a senha do banco e do primeiro administrador. Só o `.env.example`, sem credenciais reais, é versionado.
- Backups do banco (`backups/`, `*.dump`, `*.backup`): contêm dados pessoais dos clientes.
- Arquivos CSV exportados pelos relatórios: salve-os fora da pasta do projeto.
- `target/` e configurações locais de IDE.

Nenhuma senha fica no código. O sistema não grava senhas nem CPF completo no histórico de operações e não escreve dados pessoais no log.

## Preparar o banco de dados

1. Crie o `.env` a partir do exemplo e defina `DB_PASSWORD` (sem aspas):

   ```powershell
   Copy-Item .env.example .env
   ```

2. Conectado como `postgres` no banco `postgres`, execute o script [`scripts/criar-banco-local.sql`](scripts/criar-banco-local.sql), trocando a senha pelo mesmo valor de `DB_PASSWORD`. Ele cria o usuário `financeiro` e três bancos:
   - `escritorio`: usado pela aplicação;
   - `escritorio_test`: usado pelos testes automatizados;
   - `escritorio_demo`: usado só pelos [dados de demonstração](#dados-de-demonstração) (opcional).

Se criar ou recriar um banco pela interface do DBeaver, defina o **Owner** como `financeiro` (ou rode `alter database escritorio owner to financeiro;` como `postgres`). Sem isso, a aplicação recebe "permissão negada para esquema public", porque só o dono do banco pode criar tabelas no schema `public`.

Não crie as tabelas manualmente: o Flyway executa os scripts de `src/main/resources/db/migration` quando a aplicação sobe e registra o que já rodou na tabela `flyway_schema_history`. Mudanças de estrutura são feitas sempre com uma nova migration (`V10__...sql`, `V11__...sql`), nunca editando uma que já foi executada.

## Variáveis de ambiente

Todas ficam no `.env` (lido como arquivo `.properties`, portanto sem aspas). Uma variável definida no próprio terminal tem prioridade sobre o `.env`.

| Variável | Uso |
|---|---|
| `DB_HOST`, `DB_PORT` | Servidor PostgreSQL (padrão `localhost:5432`) |
| `DB_NAME` | Banco da aplicação (padrão `escritorio`) |
| `DB_USERNAME`, `DB_PASSWORD` | Usuário e senha do banco |
| `DB_TEST_NAME` | Banco dos testes automatizados (padrão `escritorio_test`) |
| `DB_DEMO_NAME` | Banco do profile `demo` (padrão `escritorio_demo`) |
| `SERVER_PORT` | Porta HTTP (padrão `8080`) |
| `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD`, `APP_ADMIN_NAME` | Primeiro administrador; veja [Primeiro acesso](#primeiro-acesso) |
| `THYMELEAF_CACHE` | `false` em desenvolvimento, para templates recarregarem com F5 |

## Executar a aplicação

```powershell
.\mvnw.cmd spring-boot:run
```

A aplicação fica disponível em http://localhost:8080.

Para gerar o `.jar` e rodar sem o Maven:

```powershell
.\mvnw.cmd -B package
java -jar target\financeiro-0.0.1-SNAPSHOT.jar
```

Execute o `.jar` a partir da pasta do projeto, para ele encontrar o `.env`.

### Recarregamento automático

O projeto usa o Spring Boot DevTools: quando uma classe compilada muda, a aplicação reinicia sozinha em poucos segundos. Com `THYMELEAF_CACHE=false` no `.env`, alterações em templates aparecem só com F5 no navegador.

- **Rodando pelo Cursor/VS Code** (botão Run/Debug da extensão Java): basta salvar o arquivo; a IDE compila e o DevTools reinicia.
- **Rodando com `.\mvnw.cmd spring-boot:run`**: o Maven não recompila sozinho. Depois de alterar o código, rode `.\mvnw.cmd compile` em outro terminal para disparar o reinício.

O DevTools só atua em desenvolvimento; ele não é incluído no `.jar` de produção.

## Primeiro acesso

Não existe cadastro público nem senha padrão. O primeiro administrador é criado na inicialização, só quando o banco ainda não tem nenhum usuário:

1. No `.env`, preencha `APP_ADMIN_USERNAME` e `APP_ADMIN_PASSWORD` (mínimo de 8 caracteres). `APP_ADMIN_NAME` é opcional.
2. Inicie a aplicação. O log mostra `Primeiro administrador '...' criado`.
3. Entre no sistema com esse usuário e apague `APP_ADMIN_PASSWORD` do `.env`.

Se já existir algum usuário no banco, essas variáveis são ignoradas.

### Perfis de acesso

Os demais usuários são criados pelo administrador no menu **Administração → Usuários**.

- **Administrador**: tudo o que o operador faz, mais a gestão de usuários, o histórico de operações e o cancelamento de lançamentos financeiros já confirmados (como um repasse pago).
- **Operador**: rotina do escritório: clientes, atendimentos, previsões, honorários, repasses, agenda e relatórios.

## Dados de demonstração

O profile `demo` carrega dados fictícios para apresentar ou testar o sistema sem usar informações reais:

- 8 clientes, sem CPF;
- 3 responsáveis e 7 atendimentos;
- previsões atrasadas, para hoje, próximas, liberadas, adiadas e canceladas;
- acordos de honorários com parcelas, recebimentos (um deles cancelado) e saldo;
- repasses pendentes, atrasados, pagos e cancelados.

As datas são calculadas a partir do dia em que a aplicação sobe, para a agenda e o painel sempre terem movimento.

O profile usa o banco `escritorio_demo` (variável `DB_DEMO_NAME`), nunca o banco real. Os dados são inseridos pela classe `DadosDemonstracao`, fora das migrations do Flyway, e só quando o banco ainda não tem clientes nem responsáveis. Por isso, subir de novo não duplica nada.

```powershell
$env:APP_ADMIN_USERNAME = "demo"
$env:APP_ADMIN_PASSWORD = "escolha-uma-senha"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

As duas primeiras linhas criam o administrador do banco de demonstração, que começa sem usuários; nas próximas vezes, basta a última linha. Para recomeçar do zero, apague e recrie o banco `escritorio_demo` (como `postgres`, com o mesmo comando do script de criação).

## Executar os testes

```powershell
.\mvnw.cmd -B test
```

Os testes usam o banco `escritorio_test` (configurável por `DB_TEST_NAME` no `.env`), nunca o banco da aplicação. O PostgreSQL local precisa estar em execução. Cada teste de integração roda dentro de uma transação desfeita ao final, então o banco de testes não acumula dados.

## Indicadores do painel

A tela inicial mostra um mês (com setas para navegar) ou um período escolhido. Os indicadores ficam em dois blocos que nunca se misturam.

**Valores do cliente (benefício do INSS)**: dinheiro do cliente, que não é receita do escritório.

| Indicador | Significado |
|---|---|
| Previsões no período | Previsões com data prevista no período, sem as canceladas |
| Valor estimado do INSS | Soma do valor estimado dessas previsões |
| Liberações confirmadas | Soma do valor confirmado das liberações com **data efetiva** no período |
| Pendentes de confirmação | Previsões do período ainda como *Previsto* ou *Aguardando confirmação* |
| Atrasadas sem confirmação | Pendentes do período cuja data prevista já passou; o total geral de atrasadas aparece ao lado quando é diferente |

**Valores do escritório**: honorários e repasses, cada um com os próprios totais.

| Indicador | Significado |
|---|---|
| Honorários contratados | Soma do valor contratado dos acordos firmados no período; avisa quantos acordos ainda estão sem valor definido |
| Honorários recebidos | Recebimentos com data no período, sem os cancelados |
| Honorários pendentes | Saldo em aberto de todos os acordos com valor definido, na posição do último dia do período |
| Repasses pendentes | Repasses ainda não pagos com data prevista até o fim do período, incluindo os atrasados |
| Repasses pagos | Soma do valor pago com data de pagamento no período |

A única situação que muda com a passagem da data é a da previsão: *Previsto* passa para *Aguardando confirmação* quando a data chega. Isso roda ao salvar, ao iniciar a aplicação e todo dia às 00:05, e pode ser desligado com `app.situacoes.atualizacao-automatica=false`. Uma previsão só fica *Liberada* quando alguém confirma a liberação.

## Relatórios e exportação CSV

Em **Consultas → Relatórios** há relatórios de previsões, recebimentos de honorários, acordos de honorários e repasses. Todos podem ser filtrados por mês ou período, responsável, tipo de serviço e situação. Há também o histórico financeiro completo de cada cliente.

O botão **Exportar CSV** gera o arquivo com os mesmos filtros da tela. O arquivo sai em UTF-8 com BOM, separador `;`, vírgula decimal e datas em dd/MM/aaaa, para abrir direto no Excel em português. O CSV não inclui CPF nem telefone, e cada exportação fica registrada no histórico de operações.

## Backup e restauração

O backup usa as ferramentas `pg_dump` e `pg_restore`, que acompanham o PostgreSQL. Ambas pedem a senha do usuário `financeiro`.

### Fazer o backup

```powershell
New-Item -ItemType Directory -Force backups | Out-Null
$data = Get-Date -Format "yyyy-MM-dd_HHmm"
& "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe" -h localhost -U financeiro -d escritorio -F c -f ".\backups\escritorio_$data.dump"
```

- O formato `-F c` é compactado e é o que o `pg_restore` espera.
- Para conferir se o arquivo está íntegro, liste o conteúdo dele com `pg_restore.exe --list .\backups\escritorio_<data>.dump`.
- A pasta `backups/` não vai para o repositório.
- Guarde cópias fora do computador, em local com acesso restrito: o backup contém CPF, telefone e valores dos clientes.
- Para automatizar, agende o comando no **Agendador de Tarefas** do Windows. Para não precisar digitar a senha, use o arquivo `%APPDATA%\postgresql\pgpass.conf`, com uma linha `localhost:5432:escritorio:financeiro:SENHA`. Esse arquivo fica fora do projeto e nunca deve ser versionado.

### Restaurar

A forma mais segura é restaurar em um banco novo, conferir e só então trocar.

1. Conectado como `postgres`, crie o banco de destino:

   ```sql
   create database escritorio_restaurado owner financeiro encoding 'UTF8' template template0;
   ```

2. Restaure o arquivo:

   ```powershell
   & "C:\Program Files\PostgreSQL\18\bin\pg_restore.exe" -h localhost -U financeiro -d escritorio_restaurado --no-owner .\backups\escritorio_<data>.dump
   ```

3. Para conferir, aponte temporariamente `DB_NAME=escritorio_restaurado` no `.env` e abra o sistema.

4. Para colocar o banco restaurado em uso, pare a aplicação e, como `postgres`, troque os nomes:

   ```sql
   alter database escritorio rename to escritorio_antigo;
   alter database escritorio_restaurado rename to escritorio;
   ```

   Depois volte `DB_NAME=escritorio` no `.env` e mantenha `escritorio_antigo` até ter certeza de que está tudo certo.

Se o backup for de uma versão anterior do sistema, o Flyway aplica as migrations que faltarem na próxima vez que a aplicação subir.

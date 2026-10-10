# Financeiro do Escritório

Sistema web interno para controlar clientes, atendimentos, previsões de liberação de benefícios do INSS, honorários e repasses de um escritório de advocacia.

## Pré-requisitos

- Java 21 ou superior
- PostgreSQL instalado localmente (porta padrão `5432`)
- Um cliente SQL, como o DBeaver (opcional, para administrar o banco)

O Maven não precisa estar instalado: use o `mvnw` (Linux/macOS) ou `mvnw.cmd` (Windows) que acompanha o projeto.

## Preparar o banco de dados

1. Crie o `.env` a partir do exemplo e defina `DB_PASSWORD` (sem aspas):

   ```powershell
   Copy-Item .env.example .env
   ```

2. Conectado como `postgres` no banco `postgres`, execute o script [`scripts/criar-banco-local.sql`](scripts/criar-banco-local.sql), trocando a senha pelo mesmo valor de `DB_PASSWORD`. Ele cria o usuário `financeiro` e dois bancos:
   - `escritorio`: usado pela aplicação;
   - `escritorio_test`: usado pelos testes automatizados.

Se criar ou recriar um banco pela interface do DBeaver, defina o **Owner** como `financeiro` (ou rode `alter database escritorio owner to financeiro;` como `postgres`). Sem isso, a aplicação recebe "permissão negada para esquema public", porque só o dono do banco pode criar tabelas no schema `public`.

Não crie as tabelas manualmente: o Flyway executa os scripts de `src/main/resources/db/migration` quando a aplicação sobe e registra o que já rodou na tabela `flyway_schema_history`. Mudanças de estrutura são feitas sempre com uma nova migration (`V2__...sql`, `V3__...sql`), nunca editando uma que já foi executada.

## Executar a aplicação

```powershell
.\mvnw.cmd spring-boot:run
```

A aplicação fica disponível em http://localhost:8080.

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

### Usuários de teste pelo SQL

Enquanto não existe tela de cadastro, outros usuários podem ser inseridos direto no banco, conectado como `financeiro` no banco `escritorio`. A senha é gravada como hash BCrypt pela extensão `pgcrypto`:

```sql
create extension if not exists pgcrypto;

insert into usuario (username, nome, senha_hash, perfil, ativo, criado_em, atualizado_em)
values ('operador', 'Operador de Teste', crypt('operador123', gen_salt('bf', 10)), 'OPERADOR', true, now(), now());
```

Suba a aplicação uma vez antes desse insert: o administrador inicial só é criado quando a tabela `usuario` está vazia.

## Executar os testes

```powershell
.\mvnw.cmd test
```

Os testes usam o banco `escritorio_test` (configurável por `DB_TEST_NAME` no `.env`), nunca o banco da aplicação. O PostgreSQL local precisa estar em execução.

## Backup

```powershell
New-Item -ItemType Directory -Force backups | Out-Null
& "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe" -h localhost -U financeiro -d escritorio -F c -f .\backups\escritorio.dump
```

A pasta `backups/` não vai para o repositório. Guarde uma cópia dos backups fora do computador.

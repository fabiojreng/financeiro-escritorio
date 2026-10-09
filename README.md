# Financeiro do Escritório

Sistema web interno para controlar clientes, atendimentos, previsões de liberação de benefícios do INSS, honorários e repasses de um escritório de advocacia.

## Pré-requisitos

- Java 21 ou superior
- Docker Desktop (para o PostgreSQL e para os testes)

O Maven não precisa estar instalado: use o `mvnw` (Linux/macOS) ou `mvnw.cmd` (Windows) que acompanha o projeto.

## Subir o banco de dados

```powershell
Copy-Item .env.example .env   # depois edite o .env e defina DB_PASSWORD
docker compose up -d
```

O contêiner expõe o PostgreSQL na porta `5433` do computador, para não conflitar com uma instalação local do PostgreSQL na porta padrão `5432`. Para mudar, ajuste `DB_PORT` no `.env`.

## Executar a aplicação

```powershell
.\mvnw.cmd spring-boot:run
```

A aplicação fica disponível em http://localhost:8080.

## Primeiro acesso

Não existe cadastro público nem senha padrão. O primeiro administrador é criado na inicialização, só quando o banco ainda não tem nenhum usuário:

1. No `.env`, preencha `APP_ADMIN_USERNAME` e `APP_ADMIN_PASSWORD` (mínimo de 8 caracteres). `APP_ADMIN_NAME` é opcional.
2. Inicie a aplicação. O log mostra `Primeiro administrador '...' criado`.
3. Entre no sistema com esse usuário e apague `APP_ADMIN_PASSWORD` do `.env`.

Se já existir algum usuário no banco, essas variáveis são ignoradas.

## Executar os testes

```powershell
.\mvnw.cmd test
```

Os testes sobem um PostgreSQL temporário via Testcontainers; o Docker precisa estar em execução.

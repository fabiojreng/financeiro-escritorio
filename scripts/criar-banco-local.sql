-- Cria o usuario e os bancos do sistema no PostgreSQL local.
-- Execute conectado como "postgres" no banco "postgres" (no DBeaver: Alt+X para rodar o script inteiro).
-- Troque a senha pelo mesmo valor de DB_PASSWORD do .env.
-- As tabelas NAO sao criadas aqui: o Flyway cria ao subir a aplicacao (src/main/resources/db/migration).

create role financeiro with login password 'troque-pela-senha-do-DB_PASSWORD';

-- Banco usado pela aplicacao
create database escritorio
    owner financeiro
    encoding 'UTF8'
    template template0;

-- Banco usado pelos testes automatizados (.\mvnw.cmd test)
create database escritorio_test
    owner financeiro
    encoding 'UTF8'
    template template0;

-- Banco usado pelo profile demo, com dados ficticios (opcional)
create database escritorio_demo
    owner financeiro
    encoding 'UTF8'
    template template0;

package br.com.escritorio.financeiro;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/**
 * Base dos testes de integração: contexto completo, MockMvc e o banco local de testes (DB_TEST_NAME).
 * As propriedades do administrador inicial são zeradas para que um .env local não crie usuários no banco de teste.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_TEST_NAME:escritorio_test}",
        "app.admin.username=",
        "app.admin.password="
})
@AutoConfigureMockMvc
public abstract class IntegracaoTest {
}

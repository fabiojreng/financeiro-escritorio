package br.com.escritorio.financeiro;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Base dos testes de integração: contexto completo, MockMvc e PostgreSQL via Testcontainers.
 * As propriedades do administrador inicial são zeradas para que um .env local não crie usuários no banco de teste.
 */
@SpringBootTest(properties = {"app.admin.username=", "app.admin.password="})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegracaoTest {
}

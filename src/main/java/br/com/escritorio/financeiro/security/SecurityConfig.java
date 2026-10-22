package br.com.escritorio.financeiro.security;

import br.com.escritorio.financeiro.domain.Permissao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;

import static br.com.escritorio.financeiro.domain.Permissao.ADMIN_AUDITORIA;
import static br.com.escritorio.financeiro.domain.Permissao.ADMIN_PERFIS;
import static br.com.escritorio.financeiro.domain.Permissao.ADMIN_USUARIOS;
import static br.com.escritorio.financeiro.domain.Permissao.AGENDA_ALTERAR_STATUS;
import static br.com.escritorio.financeiro.domain.Permissao.AGENDA_EDITAR;
import static br.com.escritorio.financeiro.domain.Permissao.AGENDA_EXPORTAR;
import static br.com.escritorio.financeiro.domain.Permissao.AGENDA_VISUALIZAR;
import static br.com.escritorio.financeiro.domain.Permissao.CADASTROS_RESPONSAVEIS;
import static br.com.escritorio.financeiro.domain.Permissao.CADASTROS_TIPOS_SERVICO;
import static br.com.escritorio.financeiro.domain.Permissao.CLIENTES_EDITAR;
import static br.com.escritorio.financeiro.domain.Permissao.CLIENTES_INATIVAR;
import static br.com.escritorio.financeiro.domain.Permissao.CLIENTES_VISUALIZAR;
import static br.com.escritorio.financeiro.domain.Permissao.DESPESAS_CADASTROS;
import static br.com.escritorio.financeiro.domain.Permissao.DESPESAS_CANCELAR;
import static br.com.escritorio.financeiro.domain.Permissao.DESPESAS_EDITAR;
import static br.com.escritorio.financeiro.domain.Permissao.DESPESAS_ENVIAR_FECHAMENTO;
import static br.com.escritorio.financeiro.domain.Permissao.DESPESAS_REABRIR_FECHAMENTO;
import static br.com.escritorio.financeiro.domain.Permissao.DESPESAS_VISUALIZAR;
import static br.com.escritorio.financeiro.domain.Permissao.FUNCIONARIOS_CANCELAR;
import static br.com.escritorio.financeiro.domain.Permissao.FUNCIONARIOS_EDITAR;
import static br.com.escritorio.financeiro.domain.Permissao.FUNCIONARIOS_FECHAR;
import static br.com.escritorio.financeiro.domain.Permissao.FUNCIONARIOS_REABRIR;
import static br.com.escritorio.financeiro.domain.Permissao.FUNCIONARIOS_RELATORIOS;
import static br.com.escritorio.financeiro.domain.Permissao.FUNCIONARIOS_VISUALIZAR;
import static br.com.escritorio.financeiro.domain.Permissao.HONORARIOS_EDITAR;
import static br.com.escritorio.financeiro.domain.Permissao.HONORARIOS_VISUALIZAR;
import static br.com.escritorio.financeiro.domain.Permissao.RELATORIOS_VISUALIZAR;
import static br.com.escritorio.financeiro.domain.Permissao.REPASSES_EDITAR;
import static br.com.escritorio.financeiro.domain.Permissao.REPASSES_VISUALIZAR;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

/**
 * Cada rota exige uma permissão do catálogo {@link Permissao}. Rotas que não estão listadas aqui são negadas:
 * uma tela nova só fica acessível depois que a permissão dela for definida.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/webjars/**", "/favicon.ico", "/error")
                        .permitAll()
                        .requestMatchers(GET, "/", "/agenda", "/painel", "/acesso-negado").authenticated()

                        .requestMatchers(GET, "/agenda-pagamentos/planilha.xlsx").hasAuthority(p(AGENDA_EXPORTAR))
                        .requestMatchers(POST, "/agenda-pagamentos/*/status").hasAuthority(p(AGENDA_ALTERAR_STATUS))
                        .requestMatchers(GET, "/agenda-pagamentos/novo", "/agenda-pagamentos/*/editar")
                        .hasAuthority(p(AGENDA_EDITAR))
                        .requestMatchers(POST, "/agenda-pagamentos", "/agenda-pagamentos/*").hasAuthority(p(AGENDA_EDITAR))
                        .requestMatchers(GET, "/agenda-pagamentos").hasAuthority(p(AGENDA_VISUALIZAR))

                        .requestMatchers(GET, "/honorarios/*/negociar").hasAuthority(p(HONORARIOS_EDITAR))
                        .requestMatchers(POST, "/honorarios/**").hasAuthority(p(HONORARIOS_EDITAR))
                        .requestMatchers(GET, "/honorarios", "/honorarios/*").hasAuthority(p(HONORARIOS_VISUALIZAR))

                        .requestMatchers(POST, "/repasses/*").hasAuthority(p(REPASSES_EDITAR))
                        .requestMatchers(GET, "/repasses").hasAuthority(p(REPASSES_VISUALIZAR))

                        .requestMatchers(POST, "/despesas/fechamento/reabrir").hasAuthority(p(DESPESAS_REABRIR_FECHAMENTO))
                        .requestMatchers(POST, "/despesas/fechamento/enviado").hasAuthority(p(DESPESAS_ENVIAR_FECHAMENTO))
                        .requestMatchers(POST, "/despesas/*/cancelar").hasAuthority(p(DESPESAS_CANCELAR))
                        .requestMatchers(GET, "/despesas/nova", "/despesas/*/editar").hasAuthority(p(DESPESAS_EDITAR))
                        .requestMatchers(POST, "/despesas", "/despesas/*").hasAuthority(p(DESPESAS_EDITAR))
                        .requestMatchers(GET, "/despesas", "/despesas/*").hasAuthority(p(DESPESAS_VISUALIZAR))
                        .requestMatchers("/categorias-despesa/**", "/formas-pagamento-despesa/**")
                        .hasAuthority(p(DESPESAS_CADASTROS))

                        .requestMatchers(GET, "/funcionarios/relatorios", "/funcionarios/relatorios/**")
                        .hasAuthority(p(FUNCIONARIOS_RELATORIOS))
                        .requestMatchers(POST, "/funcionarios/*/acerto/reabrir").hasAuthority(p(FUNCIONARIOS_REABRIR))
                        .requestMatchers(POST, "/funcionarios/*/acerto/pago").hasAuthority(p(FUNCIONARIOS_FECHAR))
                        .requestMatchers(POST, "/funcionarios/lancamentos/*/cancelar",
                                "/funcionarios/emprestimos/*/cancelar").hasAuthority(p(FUNCIONARIOS_CANCELAR))
                        .requestMatchers(GET, "/funcionarios/novo", "/funcionarios/*/editar",
                                "/funcionarios/*/lancamentos/novo", "/funcionarios/lancamentos/*/editar",
                                "/funcionarios/*/emprestimos/novo").hasAuthority(p(FUNCIONARIOS_EDITAR))
                        .requestMatchers(POST, "/funcionarios", "/funcionarios/**").hasAuthority(p(FUNCIONARIOS_EDITAR))
                        .requestMatchers(GET, "/funcionarios", "/funcionarios/**").hasAuthority(p(FUNCIONARIOS_VISUALIZAR))

                        .requestMatchers(GET, "/relatorios", "/relatorios/**").hasAuthority(p(RELATORIOS_VISUALIZAR))

                        .requestMatchers(GET, "/clientes/sugestoes").hasAnyAuthority(qualquer(CLIENTES_VISUALIZAR,
                                AGENDA_EDITAR, AGENDA_VISUALIZAR, HONORARIOS_VISUALIZAR, REPASSES_VISUALIZAR))
                        .requestMatchers(POST, "/clientes/*/ativar", "/clientes/*/desativar")
                        .hasAuthority(p(CLIENTES_INATIVAR))
                        .requestMatchers(GET, "/clientes/novo", "/clientes/*/editar").hasAuthority(p(CLIENTES_EDITAR))
                        .requestMatchers(POST, "/clientes", "/clientes/*").hasAuthority(p(CLIENTES_EDITAR))
                        .requestMatchers(GET, "/clientes", "/clientes/*").hasAuthority(p(CLIENTES_VISUALIZAR))

                        .requestMatchers("/responsaveis/**").hasAuthority(p(CADASTROS_RESPONSAVEIS))
                        .requestMatchers("/tipos-servico/**").hasAuthority(p(CADASTROS_TIPOS_SERVICO))

                        .requestMatchers("/usuarios/**").hasAuthority(p(ADMIN_USUARIOS))
                        .requestMatchers("/perfis/**").hasAuthority(p(ADMIN_PERFIS))
                        .requestMatchers("/auditoria/**").hasAuthority(p(ADMIN_AUDITORIA))

                        .anyRequest().denyAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?erro")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?saiu")
                        .permitAll())
                .exceptionHandling(ex -> ex.accessDeniedPage("/acesso-negado"));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static String p(Permissao permissao) {
        return permissao.name();
    }

    private static String[] qualquer(Permissao... permissoes) {
        return Arrays.stream(permissoes).map(Permissao::name).toArray(String[]::new);
    }
}

package br.com.escritorio.financeiro.security;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.Usuario;
import br.com.escritorio.financeiro.repository.PerfilRepository;
import br.com.escritorio.financeiro.repository.UsuarioRepository;
import br.com.escritorio.financeiro.service.AuditoriaService;
import br.com.escritorio.financeiro.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class AdminInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialRunner.class);

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final AdminInicialProperties properties;

    public AdminInicialRunner(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository,
                              PasswordEncoder passwordEncoder, AuditoriaService auditoriaService,
                              AdminInicialProperties properties) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (!StringUtils.hasText(properties.username()) || !StringUtils.hasText(properties.password())) {
            log.warn("Nenhum usuário cadastrado. Defina APP_ADMIN_USERNAME e APP_ADMIN_PASSWORD no .env "
                    + "e reinicie a aplicação para criar o primeiro administrador.");
            return;
        }
        if (properties.password().length() < UsuarioService.TAMANHO_MINIMO_SENHA) {
            log.warn("APP_ADMIN_PASSWORD precisa ter pelo menos {} caracteres. O administrador não foi criado.",
                    UsuarioService.TAMANHO_MINIMO_SENHA);
            return;
        }

        Perfil perfilAdministrador = perfilRepository.findFirstBySistemaTrue()
                .orElseThrow(() -> new IllegalStateException("Perfil Administrador não encontrado no banco."));
        String nome = StringUtils.hasText(properties.nome()) ? properties.nome().trim() : "Administrador";
        Usuario admin = new Usuario(properties.username().trim(), nome,
                passwordEncoder.encode(properties.password()), perfilAdministrador);
        usuarioRepository.save(admin);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, UsuarioService.ENTIDADE, admin.getId(),
                "Primeiro administrador '" + admin.getUsername() + "' criado pela configuração inicial.");
        log.info("Primeiro administrador '{}' criado. Remova APP_ADMIN_PASSWORD do .env.", admin.getUsername());
    }
}

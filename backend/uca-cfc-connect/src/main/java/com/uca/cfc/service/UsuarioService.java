package com.uca.cfc.service;

import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.dto.usuario.UsuarioRequest;
import com.uca.cfc.dto.usuario.UsuarioResponse;
import com.uca.cfc.dto.usuario.UsuarioUpdateRequest;
import com.uca.cfc.entity.Usuario;
import com.uca.cfc.entity.enums.EstadoUsuario;
import com.uca.cfc.exception.DuplicateResourceException;
import com.uca.cfc.exception.ResourceInUseException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.SeguridadMapper;
import com.uca.cfc.repository.ClienteRepository;
import com.uca.cfc.repository.UsuarioRepository;
import com.uca.cfc.repository.RefreshTokenRepository;

/**
 * Reglas de negocio de los usuarios.
 * Cifra la contrasena con BCrypt antes de guardarla y evita correos repetidos.
 * Al actualizar, si no se manda contrasena nueva se conserva la que ya tenia.
 */
@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private static final String RECURSO = "Usuario";

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final RolService rolService;
    private final PasswordEncoder passwordEncoder;
    private final SeguridadMapper mapper;
    private final RefreshTokenRepository refreshTokens;

    public UsuarioService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
            RolService rolService, PasswordEncoder passwordEncoder, SeguridadMapper mapper,
            RefreshTokenRepository refreshTokens) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.rolService = rolService;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.refreshTokens = refreshTokens;
    }

    public PageResponse<UsuarioResponse> listar(Long rolId, EstadoUsuario estado, String texto, Pageable pageable) {
        return PageResponse.de(usuarioRepository.buscar(rolId, estado, normalizar(texto), pageable),
                mapper::aResponse);
    }

    public UsuarioResponse obtener(Long id) {
        return mapper.aResponse(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Ya existe un usuario con el correo '" + email + "'");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(rolService.buscar(request.rolId()));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        return mapper.aResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = buscar(id);
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("Ya existe un usuario con el correo '" + email + "'");
        }

        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setRol(rolService.buscar(request.rolId()));
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        refreshTokens.revocarTodosDelUsuario(id);
        return mapper.aResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, EstadoUsuario estado) {
        Usuario usuario = buscar(id);
        usuario.setEstado(estado);
        refreshTokens.revocarTodosDelUsuario(id);
        return mapper.aResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = buscar(id);
        if (clienteRepository.findAll().stream()
                .anyMatch(cliente -> cliente.getUsuario() != null && cliente.getUsuario().getId().equals(id))) {
            throw new ResourceInUseException("No se puede eliminar el usuario '" + usuario.getEmail()
                    + "' porque está vinculado a un cliente. Cámbielo a estado INACTIVO");
        }
        usuarioRepository.delete(usuario);
    }

    public Usuario buscar(Long id) {
        return usuarioRepository.findWithRolById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private String normalizar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}

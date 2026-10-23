package com.uca.cfc.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.auth.RegistroClienteRequest;
import com.uca.cfc.dto.cliente.ClienteRequest;
import com.uca.cfc.dto.cliente.ClienteResponse;
import com.uca.cfc.entity.Rol;
import com.uca.cfc.entity.Usuario;
import com.uca.cfc.entity.enums.EstadoUsuario;
import com.uca.cfc.entity.enums.RolSistema;
import com.uca.cfc.exception.DuplicateResourceException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.repository.RolRepository;
import com.uca.cfc.repository.UsuarioRepository;

/**
 * Autorregistro de clientes. Crea la cuenta (siempre con rol CLIENTE) y la ficha
 * de cliente en la misma transacción: si el correo, el DUI o el NIT ya existen,
 * no se guarda ninguna de las dos.
 * No se enlaza con fichas de cliente ya existentes porque el correo no está
 * verificado y cualquiera podría apropiarse del historial de otra persona.
 */
@Service
@Transactional(readOnly = true)
public class RegistroService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ClienteService clienteService;
    private final PasswordEncoder passwordEncoder;

    public RegistroService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            ClienteService clienteService, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.clienteService = clienteService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ClienteResponse registrar(RegistroClienteRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Ya existe una cuenta con el correo '" + email + "'");
        }

        Rol rolCliente = rolRepository.findByNombreIgnoreCase(RolSistema.CLIENTE.name())
                .orElseThrow(() -> new ResourceNotFoundException("El rol CLIENTE no está configurado"));

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(rolCliente);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuarioRepository.save(usuario);

        ClienteRequest datosCliente = new ClienteRequest(request.tipo(), request.nombre(), request.dui(),
                request.nit(), email, request.telefono(), request.direccion(), request.empresa());
        return clienteService.crearConUsuario(datosCliente, usuario);
    }
}

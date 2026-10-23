package com.uca.cfc.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.rol.RolRequest;
import com.uca.cfc.dto.rol.RolResponse;
import com.uca.cfc.entity.Rol;
import com.uca.cfc.entity.enums.RolSistema;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.exception.DuplicateResourceException;
import com.uca.cfc.exception.ResourceInUseException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.SeguridadMapper;
import com.uca.cfc.repository.RolRepository;
import com.uca.cfc.repository.UsuarioRepository;

/**
 * Reglas de negocio de los roles.
 * Protege los cuatro roles base del sistema para que no se renombren ni se
 * borren, y evita eliminar un rol que todavia tenga usuarios asignados.
 */
@Service
@Transactional(readOnly = true)
public class RolService {

    private static final String RECURSO = "Rol";

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final SeguridadMapper mapper;

    public RolService(RolRepository rolRepository, UsuarioRepository usuarioRepository, SeguridadMapper mapper) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.mapper = mapper;
    }

    /** Los roles son pocos: se devuelven completos, sin paginar. */
    public List<RolResponse> listar() {
        return rolRepository.findAll().stream().map(mapper::aResponse).toList();
    }

    public RolResponse obtener(Long id) {
        return mapper.aResponse(buscar(id));
    }

    @Transactional
    public RolResponse crear(RolRequest request) {
        String nombre = request.nombre().trim().toUpperCase();
        if (rolRepository.existsByNombreIgnoreCase(nombre)) {
            throw new DuplicateResourceException("Ya existe un rol con el nombre '" + nombre + "'");
        }
        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setDescripcion(request.descripcion());
        return mapper.aResponse(rolRepository.save(rol));
    }

    @Transactional
    public RolResponse actualizar(Long id, RolRequest request) {
        Rol rol = buscar(id);
        String nombre = request.nombre().trim().toUpperCase();

        // Los cuatro roles base sostienen la autorización del sistema
        if (rol.esDelSistema() && !rol.getNombre().equalsIgnoreCase(nombre)) {
            throw new BusinessException("El rol " + rol.getNombre() + " es un rol base del sistema y no puede "
                    + "renombrarse. Solo se permite actualizar su descripción");
        }
        if (rolRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new DuplicateResourceException("Ya existe un rol con el nombre '" + nombre + "'");
        }

        rol.setNombre(nombre);
        rol.setDescripcion(request.descripcion());
        return mapper.aResponse(rolRepository.save(rol));
    }

    @Transactional
    public void eliminar(Long id) {
        Rol rol = buscar(id);
        if (rol.esDelSistema()) {
            throw new BusinessException("El rol " + rol.getNombre()
                    + " es un rol base del sistema y no puede eliminarse");
        }
        if (usuarioRepository.existsByRolId(id)) {
            throw new ResourceInUseException("No se puede eliminar el rol '" + rol.getNombre()
                    + "' porque tiene usuarios asignados");
        }
        rolRepository.delete(rol);
    }

    public Rol buscar(Long id) {
        return rolRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    public Rol buscarPorNombre(RolSistema rol) {
        return rolRepository.findByNombreIgnoreCase(rol.name())
                .orElseThrow(() -> new ResourceNotFoundException("No está configurado el rol " + rol));
    }
}

package com.uca.cfc.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.categoria.CategoriaRequest;
import com.uca.cfc.dto.categoria.CategoriaResponse;
import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.entity.Categoria;
import com.uca.cfc.exception.DuplicateResourceException;
import com.uca.cfc.exception.ResourceInUseException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.CatalogoMapper;
import com.uca.cfc.repository.CategoriaRepository;
import com.uca.cfc.repository.CursoRepository;
import com.uca.cfc.repository.DiplomadoRepository;

/**
 * Reglas de negocio de las categorias.
 * Los servicios son la capa donde vive la logica: validan, coordinan y manejan
 * las transacciones. Aqui se revisa que no se repitan nombres y que no se
 * elimine una categoria que ya tiene cursos usandola.
 */
@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private static final String RECURSO = "Categoría";

    private final CategoriaRepository categoriaRepository;
    private final CursoRepository cursoRepository;
    private final DiplomadoRepository diplomadoRepository;
    private final CatalogoMapper mapper;

    public CategoriaService(CategoriaRepository categoriaRepository, CursoRepository cursoRepository,
            DiplomadoRepository diplomadoRepository, CatalogoMapper mapper) {
        this.categoriaRepository = categoriaRepository;
        this.cursoRepository = cursoRepository;
        this.diplomadoRepository = diplomadoRepository;
        this.mapper = mapper;
    }

    public PageResponse<CategoriaResponse> listar(String nombre, Pageable pageable) {
        Page<Categoria> page = (nombre == null || nombre.isBlank())
                ? categoriaRepository.findAll(pageable)
                : categoriaRepository.findByNombreContainingIgnoreCase(nombre, pageable);
        return PageResponse.de(page, mapper::aResponse);
    }

    public CategoriaResponse obtener(Long id) {
        return mapper.aResponse(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        validarNombreDisponible(request.nombre(), null);
        Categoria categoria = new Categoria();
        aplicar(request, categoria);
        return mapper.aResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscar(id);
        validarNombreDisponible(request.nombre(), categoria);
        aplicar(request, categoria);
        return mapper.aResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        if (cursoRepository.existsByCategoriaId(id) || diplomadoRepository.existsByCategoriaId(id)) {
            throw new ResourceInUseException(
                    "No se puede eliminar la categoría '" + categoria.getNombre()
                            + "' porque tiene cursos o diplomados asociados");
        }
        categoriaRepository.delete(categoria);
    }

    /** Búsqueda para uso de otros services. */
    public Categoria buscar(Long id) {
        return categoriaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private void aplicar(CategoriaRequest request, Categoria categoria) {
        categoria.setNombre(request.nombre().trim());
        categoria.setDescripcion(request.descripcion());
    }

    private void validarNombreDisponible(String nombre, Categoria actual) {
        categoriaRepository.findByNombreIgnoreCase(nombre.trim()).ifPresent(existente -> {
            if (actual == null || !existente.getId().equals(actual.getId())) {
                throw new DuplicateResourceException("Ya existe una categoría con el nombre '" + nombre.trim() + "'");
            }
        });
    }
}

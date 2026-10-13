package com.uca.cfc.service;

import org.springframework.stereotype.Component;

import com.uca.cfc.dto.curso.OfertaAcademicaRequest;
import com.uca.cfc.entity.OfertaAcademica;
import com.uca.cfc.exception.BusinessException;

/**
 * Reglas comunes a cursos y diplomados: coherencia de fechas y resolución de
 * las referencias a categoría, modalidad, docente y espacio.
 */
@Component
public class OfertaAcademicaValidator {

    private final CategoriaService categoriaService;
    private final ModalidadService modalidadService;
    private final DocenteService docenteService;
    private final EspacioService espacioService;

    public OfertaAcademicaValidator(CategoriaService categoriaService, ModalidadService modalidadService,
            DocenteService docenteService, EspacioService espacioService) {
        this.categoriaService = categoriaService;
        this.modalidadService = modalidadService;
        this.docenteService = docenteService;
        this.espacioService = espacioService;
    }

    public void aplicar(OfertaAcademicaRequest request, OfertaAcademica oferta) {
        if (request.fechaFin().isBefore(request.fechaInicio())) {
            throw new BusinessException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        oferta.setCodigo(request.codigo().trim().toUpperCase());
        oferta.setNombre(request.nombre().trim());
        oferta.setDescripcion(request.descripcion());
        oferta.setCategoria(categoriaService.buscar(request.categoriaId()));
        oferta.setModalidad(modalidadService.buscar(request.modalidadId()));
        oferta.setDocente(request.docenteId() == null ? null : docenteService.buscar(request.docenteId()));
        oferta.setEspacio(request.espacioId() == null ? null : espacioService.buscar(request.espacioId()));
        oferta.setFechaInicio(request.fechaInicio());
        oferta.setFechaFin(request.fechaFin());
        oferta.setHorario(request.horario());
        oferta.setDuracionHoras(request.duracionHoras());
        oferta.setCupoMaximo(request.cupoMaximo());
        oferta.setPrecio(request.precio());
    }
}

package com.uca.cfc.mapper;

import org.springframework.stereotype.Component;

import com.uca.cfc.dto.categoria.CategoriaResponse;
import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.dto.docente.DocenteResponse;
import com.uca.cfc.dto.espacio.EspacioResponse;
import com.uca.cfc.dto.modalidad.ModalidadResponse;
import com.uca.cfc.entity.Categoria;
import com.uca.cfc.entity.Docente;
import com.uca.cfc.entity.Espacio;
import com.uca.cfc.entity.Modalidad;

/** Conversión de entidades de catálogo a sus DTOs de respuesta. */
@Component
public class CatalogoMapper {

    public CategoriaResponse aResponse(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre(), categoria.getDescripcion(),
                categoria.getCreatedAt(), categoria.getUpdatedAt());
    }

    public ModalidadResponse aResponse(Modalidad modalidad) {
        return new ModalidadResponse(modalidad.getId(), modalidad.getNombre(), modalidad.getTipo(),
                modalidad.getDescripcion());
    }

    public DocenteResponse aResponse(Docente docente) {
        return new DocenteResponse(docente.getId(), docente.getNombres(), docente.getApellidos(),
                docente.getNombreCompleto(), docente.getEmail(), docente.getTelefono(), docente.getEspecialidad());
    }

    public EspacioResponse aResponse(Espacio espacio) {
        return new EspacioResponse(espacio.getId(), espacio.getNombre(), espacio.getTipo(), espacio.getCapacidad(),
                espacio.getPrecioPorHora(), espacio.getEquipamiento(), espacio.getUbicacion(), espacio.getEstado());
    }

    public ResumenReferencia aResumen(Categoria categoria) {
        return categoria == null ? null : new ResumenReferencia(categoria.getId(), categoria.getNombre());
    }

    public ResumenReferencia aResumen(Modalidad modalidad) {
        return modalidad == null ? null : new ResumenReferencia(modalidad.getId(), modalidad.getNombre());
    }

    public ResumenReferencia aResumen(Docente docente) {
        return docente == null ? null : new ResumenReferencia(docente.getId(), docente.getNombreCompleto());
    }

    public ResumenReferencia aResumen(Espacio espacio) {
        return espacio == null ? null : new ResumenReferencia(espacio.getId(), espacio.getNombre());
    }
}

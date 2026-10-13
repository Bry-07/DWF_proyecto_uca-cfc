package com.uca.cfc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Curso del catalogo academico.
 * Hereda de OfertaAcademica todos los campos que comparte con Diplomado
 * (codigo, fechas, cupo, precio...), asi no se repiten en las dos clases.
 */
@Entity
@Table(name = "cursos")
public class Curso extends OfertaAcademica {
}

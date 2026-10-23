package com.uca.cfc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Diplomado del catalogo academico.
 * Igual que Curso, hereda la estructura comun de OfertaAcademica.
 */
@Entity
@Table(name = "diplomados")
public class Diplomado extends OfertaAcademica {
}

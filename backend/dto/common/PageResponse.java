package com.uca.cfc.dto.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Envoltura de resultados paginados. Evita exponer la estructura interna de
 * {@link Page}, que no es estable entre versiones de Spring Data.
 */
@Schema(description = "Resultado paginado")
public record PageResponse<T>(
        List<T> contenido,
        @Schema(example = "0") int pagina,
        @Schema(example = "10") int tamanio,
        @Schema(example = "42") long totalElementos,
        @Schema(example = "5") int totalPaginas,
        @Schema(example = "false") boolean ultima) {

    public static <E, D> PageResponse<D> de(Page<E> page, Function<E, D> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}

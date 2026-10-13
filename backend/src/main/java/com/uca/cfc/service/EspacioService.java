package com.uca.cfc.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.dto.espacio.DisponibilidadResponse;
import com.uca.cfc.dto.espacio.EspacioRequest;
import com.uca.cfc.dto.espacio.EspacioResponse;
import com.uca.cfc.entity.ActividadAgenda;
import com.uca.cfc.entity.Alquiler;
import com.uca.cfc.entity.Espacio;
import com.uca.cfc.entity.enums.EstadoEspacio;
import com.uca.cfc.entity.enums.TipoEspacio;
import com.uca.cfc.exception.DuplicateResourceException;
import com.uca.cfc.exception.ResourceInUseException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.CatalogoMapper;
import com.uca.cfc.repository.ActividadAgendaRepository;
import com.uca.cfc.repository.AlquilerRepository;
import com.uca.cfc.repository.CursoRepository;
import com.uca.cfc.repository.DiplomadoRepository;
import com.uca.cfc.repository.EspacioRepository;
import com.uca.cfc.repository.SolicitudCateringRepository;
import com.uca.cfc.util.IntervaloHorario;

/**
 * Reglas de negocio de los espacios.
 * El metodo consultarDisponibilidad es el mas interesante: cruza los alquileres
 * vigentes con las actividades de la agenda para armar la lista de franjas
 * ocupadas en el rango que se consulta.
 */
@Service
@Transactional(readOnly = true)
public class EspacioService {

    private static final String RECURSO = "Espacio";

    private final EspacioRepository espacioRepository;
    private final AlquilerRepository alquilerRepository;
    private final ActividadAgendaRepository agendaRepository;
    private final CursoRepository cursoRepository;
    private final DiplomadoRepository diplomadoRepository;
    private final SolicitudCateringRepository solicitudCateringRepository;
    private final CatalogoMapper mapper;

    public EspacioService(EspacioRepository espacioRepository, AlquilerRepository alquilerRepository,
            ActividadAgendaRepository agendaRepository, CursoRepository cursoRepository,
            DiplomadoRepository diplomadoRepository, SolicitudCateringRepository solicitudCateringRepository,
            CatalogoMapper mapper) {
        this.espacioRepository = espacioRepository;
        this.alquilerRepository = alquilerRepository;
        this.agendaRepository = agendaRepository;
        this.cursoRepository = cursoRepository;
        this.diplomadoRepository = diplomadoRepository;
        this.solicitudCateringRepository = solicitudCateringRepository;
        this.mapper = mapper;
    }

    public PageResponse<EspacioResponse> listar(TipoEspacio tipo, EstadoEspacio estado, Integer capacidadMinima,
            String nombre, Pageable pageable) {
        return PageResponse.de(
                espacioRepository.buscar(tipo, estado, capacidadMinima, normalizar(nombre), pageable),
                mapper::aResponse);
    }

    public EspacioResponse obtener(Long id) {
        return mapper.aResponse(buscar(id));
    }

    @Transactional
    public EspacioResponse crear(EspacioRequest request) {
        if (espacioRepository.existsByNombreIgnoreCase(request.nombre().trim())) {
            throw new DuplicateResourceException("Ya existe un espacio con el nombre '" + request.nombre() + "'");
        }
        Espacio espacio = new Espacio();
        aplicar(request, espacio);
        return mapper.aResponse(espacioRepository.save(espacio));
    }

    @Transactional
    public EspacioResponse actualizar(Long id, EspacioRequest request) {
        Espacio espacio = buscar(id);
        if (espacioRepository.existsByNombreIgnoreCaseAndIdNot(request.nombre().trim(), id)) {
            throw new DuplicateResourceException("Ya existe un espacio con el nombre '" + request.nombre() + "'");
        }
        aplicar(request, espacio);
        return mapper.aResponse(espacioRepository.save(espacio));
    }

    @Transactional
    public void eliminar(Long id) {
        Espacio espacio = buscar(id);
        boolean enUso = alquilerRepository.existsByEspacioId(id)
                || agendaRepository.existsByEspacioId(id)
                || cursoRepository.existsByEspacioId(id)
                || diplomadoRepository.existsByEspacioId(id)
                || solicitudCateringRepository.existsByEspacioId(id);
        if (enUso) {
            throw new ResourceInUseException("No se puede eliminar el espacio '" + espacio.getNombre()
                    + "' porque tiene actividades asociadas. Cámbielo a estado INACTIVO");
        }
        espacioRepository.delete(espacio);
    }

    /**
     * Consulta de disponibilidad: cruza alquileres vigentes y actividades de la
     * agenda dentro del rango solicitado.
     */
    public DisponibilidadResponse consultarDisponibilidad(Long id, LocalDateTime desde, LocalDateTime hasta) {
        Espacio espacio = buscar(id);
        IntervaloHorario rango = new IntervaloHorario(desde, hasta);

        List<DisponibilidadResponse.Ocupacion> ocupaciones = new ArrayList<>();

        for (Alquiler alquiler : alquilerRepository.findOcupacionEnRango(id, rango.inicio(), rango.fin())) {
            ocupaciones.add(new DisponibilidadResponse.Ocupacion("ALQUILER", alquiler.getId(),
                    alquiler.getProposito(), alquiler.getFechaHoraInicio(), alquiler.getFechaHoraFin()));
        }

        for (ActividadAgenda actividad : agendaRepository.buscarSolapamientos(id, rango.inicio(), rango.fin(), null)) {
            // Los alquileres ya aparecen listados arriba con su propio identificador
            if (actividad.getTipo().esSincronizado() && actividad.getOrigenId() != null) {
                continue;
            }
            ocupaciones.add(new DisponibilidadResponse.Ocupacion("AGENDA", actividad.getId(), actividad.getTitulo(),
                    actividad.getFechaHoraInicio(), actividad.getFechaHoraFin()));
        }

        boolean disponible = ocupaciones.isEmpty() && espacio.getEstado() == EstadoEspacio.DISPONIBLE;
        return new DisponibilidadResponse(espacio.getId(), espacio.getNombre(), rango.inicio(), rango.fin(),
                disponible, ocupaciones);
    }

    public Espacio buscar(Long id) {
        return espacioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private void aplicar(EspacioRequest request, Espacio espacio) {
        espacio.setNombre(request.nombre().trim());
        espacio.setTipo(request.tipo());
        espacio.setCapacidad(request.capacidad());
        espacio.setPrecioPorHora(request.precioPorHora());
        espacio.setEquipamiento(request.equipamiento());
        espacio.setUbicacion(request.ubicacion());
        if (request.estado() != null) {
            espacio.setEstado(request.estado());
        }
    }

    private String normalizar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}

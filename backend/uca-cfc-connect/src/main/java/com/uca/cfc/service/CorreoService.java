package com.uca.cfc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.uca.cfc.entity.Usuario;

/**
 * Envío de correos del sistema. Por ahora el proyecto no tiene servidor SMTP
 * configurado, así que el mensaje de recuperación solo queda en el log (nivel DEBUG).
 * Para producción bastaría reemplazar el cuerpo de este método por un JavaMailSender.
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    public void enviarRecuperacion(Usuario usuario, String token) {
        log.debug("Recuperación de contraseña para {}. Token: {}", usuario.getEmail(), token);
    }
}

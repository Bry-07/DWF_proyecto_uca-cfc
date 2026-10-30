package com.uca.cfc.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.uca.cfc.entity.Usuario;
import com.uca.cfc.exception.CorreoNoDisponibleException;

/**
 * Entrega por SMTP. En desarrollo se permite devolver el código de prueba sin correo real.
 */
@Service
public class CorreoService {

    private final ObjectProvider<JavaMailSender> correo;
    private final boolean modoPrueba;
    private final String remitente;
    private final String frontend;

    public CorreoService(ObjectProvider<JavaMailSender> correo,
            @Value("${app.seguridad.mostrar-token-recuperacion:false}") boolean modoPrueba,
            @Value("${app.correo.remitente:no-reply@cfc.uca.edu.sv}") String remitente,
            @Value("${app.frontend-url:http://localhost:3000}") String frontend) {
        this.correo = correo;
        this.modoPrueba = modoPrueba;
        this.remitente = remitente;
        this.frontend = frontend.replaceAll("/+$", "");
    }

    public void enviarRecuperacion(Usuario usuario, String token) {
        JavaMailSender sender = correo.getIfAvailable();
        if (sender == null) {
            if (modoPrueba) return;
            throw new CorreoNoDisponibleException();
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(usuario.getEmail());
        mensaje.setSubject("Restablecer contraseña de UCA-CFC Connect");
        mensaje.setText("Use este enlace para restablecer su contraseña: " + frontend
                + "/restablecer?token=" + token + "\nCódigo de recuperación: " + token
                + "\nEl código tiene un solo uso. Si no solicitó el cambio, ignore este correo.");
        try {
            sender.send(mensaje);
        } catch (MailException ex) {
            // No registrar tokens ni informar éxito si el transporte rechazó el envío.
            throw new CorreoNoDisponibleException();
        }
    }
}

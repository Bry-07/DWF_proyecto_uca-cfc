package com.uca.cfc.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.uca.cfc.entity.Usuario;
import com.uca.cfc.service.CorreoService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Transporte SMTP real contra un servidor local, sin credenciales ni correo externo. */
class CorreoSmtpIT {
    @Test
    @SuppressWarnings("unchecked")
    void entregaElCorreoPorSmtp() throws Exception {
        try (var servidor = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
                var executor = Executors.newSingleThreadExecutor()) {
            servidor.setSoTimeout(10000);
            var recibido = executor.submit(() -> {
                try (var socket = servidor.accept();
                        var entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                        var salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
                    socket.setSoTimeout(10000);
                    salida.print("220 localhost SMTP\r\n"); salida.flush();
                    StringBuilder contenido = new StringBuilder();
                    String linea;
                    while ((linea = entrada.readLine()) != null) {
                        if (linea.equals("DATA")) {
                            salida.print("354 Continue\r\n"); salida.flush();
                            while ((linea = entrada.readLine()) != null && !linea.equals(".")) contenido.append(linea).append("\r\n");
                            salida.print("250 Queued\r\n");
                        } else if (linea.equals("QUIT")) {
                            salida.print("221 Bye\r\n"); salida.flush(); break;
                        } else salida.print("250 OK\r\n");
                        salida.flush();
                    }
                    return contenido.toString();
                }
            });
            JavaMailSenderImpl smtp = new JavaMailSenderImpl();
            smtp.setHost(servidor.getInetAddress().getHostAddress());
            smtp.setPort(servidor.getLocalPort());
            ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
            when(provider.getIfAvailable()).thenReturn(smtp);
            Usuario usuario = new Usuario(); usuario.setEmail("destino@example.com");
            new CorreoService(provider, false, "cfc@example.com", "https://cfc.example.com")
                    .enviarRecuperacion(usuario, "codigo-prueba-smtp");
            String contenido = recibido.get(10, TimeUnit.SECONDS);
            MimeMessage mensaje = new MimeMessage(Session.getInstance(new Properties()),
                    new ByteArrayInputStream(contenido.getBytes(StandardCharsets.UTF_8)));
            assertEquals("destino@example.com", mensaje.getAllRecipients()[0].toString());
            assertTrue(mensaje.getContent().toString().contains("/restablecer?token=codigo-prueba-smtp"));
            assertTrue(mensaje.getSubject().contains("UCA-CFC"));
        }
    }
}

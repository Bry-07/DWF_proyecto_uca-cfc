package com.uca.cfc.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.uca.cfc.entity.Usuario;
import com.uca.cfc.exception.CorreoNoDisponibleException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class CorreoServiceTest {
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setEmail("cliente@example.com");
        return usuario;
    }

    @Test void enviaCodigoYEnlaceAlDestinatario() {
        JavaMailSender mail = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(mail);
        new CorreoService(provider, false, "cfc@example.com", "https://cfc.example.com/")
                .enviarRecuperacion(usuario(), "codigo-seguro");
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(captor.capture());
        assertArrayEquals(new String[]{"cliente@example.com"}, captor.getValue().getTo());
        assertTrue(captor.getValue().getText().contains("https://cfc.example.com/restablecer?token=codigo-seguro"));
    }

    @Test void soloDesarrolloPermiteRecuperarSinSmtp() {
        assertDoesNotThrow(() -> new CorreoService(provider, true, "cfc@example.com", "http://localhost:3000")
                .enviarRecuperacion(usuario(), "codigo"));
        assertThrows(CorreoNoDisponibleException.class,
                () -> new CorreoService(provider, false, "cfc@example.com", "https://cfc.example.com")
                        .enviarRecuperacion(usuario(), "codigo"));
    }

    @Test void falloDeTransporteNoDevuelveExito() {
        JavaMailSender mail = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(mail);
        doThrow(new MailSendException("sin conexión")).when(mail).send(any(SimpleMailMessage.class));
        assertThrows(CorreoNoDisponibleException.class,
                () -> new CorreoService(provider, false, "cfc@example.com", "https://cfc.example.com")
                        .enviarRecuperacion(usuario(), "codigo"));
    }
}

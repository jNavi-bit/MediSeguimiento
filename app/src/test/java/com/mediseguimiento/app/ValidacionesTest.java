package com.mediseguimiento.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.mediseguimiento.app.core.Validaciones;

import org.junit.Test;

public class ValidacionesTest {

    @Test
    public void correo_validoEInvalido() {
        assertNull(Validaciones.correo("ana.lopez@correo.com"));
        assertNull(Validaciones.correo("  ANA@Correo.MX "));
        assertNotNull(Validaciones.correo(""));
        assertNotNull(Validaciones.correo("ana@"));
        assertNotNull(Validaciones.correo("ana.correo.com"));
    }

    @Test
    public void correo_seNormaliza() {
        assertEquals("ana@correo.mx", Validaciones.normalizarCorreo("  ANA@Correo.MX "));
    }

    @Test
    public void nombre_reglas() {
        assertNull(Validaciones.nombre("Ana"));
        assertNotNull(Validaciones.nombre("   "));
        assertNotNull(Validaciones.nombre("A"));
    }

    @Test
    public void passwordNueva_requiereLongitudLetrasYNumeros() {
        assertNull(Validaciones.passwordNueva("seguimiento1"));
        assertNotNull(Validaciones.passwordNueva("corta1"));
        assertNotNull(Validaciones.passwordNueva("sololetras"));
        assertNotNull(Validaciones.passwordNueva("12345678"));
    }

    @Test
    public void confirmacion_debeCoincidir() {
        assertNull(Validaciones.confirmacion("clave1234", "clave1234"));
        assertNotNull(Validaciones.confirmacion("clave1234", "clave12345"));
        assertNotNull(Validaciones.confirmacion("clave1234", ""));
    }

    @Test
    public void fuerza_creceConLaComplejidad() {
        assertEquals(0, Validaciones.fuerzaPassword(""));
        assertEquals(1, Validaciones.fuerzaPassword("abc"));
        int media = Validaciones.fuerzaPassword("abcdefg1");
        int alta = Validaciones.fuerzaPassword("Seguimiento#2026");
        assertTrue(media < alta);
        assertEquals(4, alta);
    }
}

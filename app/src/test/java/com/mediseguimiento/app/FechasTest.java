package com.mediseguimiento.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.core.Validaciones;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;

public class FechasTest {

    private static final LocalDate BASE = LocalDate.of(2026, 11, 16); // lunes
    private static final long AHORA = Fechas.aMillis(BASE, LocalTime.of(10, 0));

    private static long en(int dias, int hora, int min) {
        return Fechas.aMillis(BASE.plusDays(dias), LocalTime.of(hora, min));
    }

    @Test
    public void fechaLarga_enEspanol() {
        assertEquals("Lunes 16 de noviembre", Fechas.fechaLarga(AHORA, AHORA));
        assertTrue(Fechas.fechaLarga(en(60, 9, 0), AHORA).endsWith("de 2027"));
    }

    @Test
    public void diaRelativo() {
        assertEquals("Hoy", Fechas.diaRelativo(en(0, 18, 0), AHORA));
        assertEquals("Mañana", Fechas.diaRelativo(en(1, 8, 0), AHORA));
        assertEquals("Ayer", Fechas.diaRelativo(en(-1, 8, 0), AHORA));
        assertNull(Fechas.diaRelativo(en(3, 8, 0), AHORA));
    }

    @Test
    public void cuentaRegresiva() {
        assertEquals("En 30 min", Fechas.cuentaRegresiva(en(0, 10, 30), AHORA));
        assertEquals("En 3 h", Fechas.cuentaRegresiva(en(0, 13, 5), AHORA));
        assertEquals("Mañana", Fechas.cuentaRegresiva(en(1, 9, 0), AHORA));
        assertEquals("En 4 días", Fechas.cuentaRegresiva(en(4, 9, 0), AHORA));
        assertEquals("En 2 semanas", Fechas.cuentaRegresiva(en(15, 9, 0), AHORA));
        assertEquals("Hace 2 días", Fechas.cuentaRegresiva(en(-2, 9, 0), AHORA));
    }

    @Test
    public void horaEnFormatos() {
        long t = en(0, 15, 5);
        assertEquals("15:05", Fechas.hora(t, true));
        assertEquals("3:05 p. m.", Fechas.horaCompleta(t, false));
    }

    @Test
    public void fechaCita_validacion() {
        assertNull(Validaciones.fechaCita(en(1, 9, 0), AHORA, true));
        assertNotNull(Validaciones.fechaCita(en(-1, 9, 0), AHORA, true));
        // Al editar se permite una fecha pasada (para corregir registros).
        assertNull(Validaciones.fechaCita(en(-1, 9, 0), AHORA, false));
        assertNotNull(Validaciones.fechaCita(en(900, 9, 0), AHORA, true));
        assertNotNull(Validaciones.fechaCita(null, AHORA, true));
    }

    @Test
    public void especialidad_obligatoria() {
        assertNull(Validaciones.especialidad("Cardiología"));
        assertNotNull(Validaciones.especialidad("   "));
    }
}

package com.mediseguimiento.app.ui.agenda;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.mediseguimiento.app.core.Fechas;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class DisposicionDiaTest {

    private static int m(int h, int min) { return h * 60 + min; }

    @Test
    public void sinTraslape_unaColumnaCadaUna() {
        List<DisposicionDia.Lugar> l = DisposicionDia.calcular(
                new int[]{m(9, 0), m(10, 0)}, new int[]{m(9, 30), m(11, 0)});
        assertEquals(1, l.get(0).columnas);
        assertEquals(1, l.get(1).columnas);
        assertEquals(0, l.get(1).columna);
    }

    @Test
    public void traslapadas_ladoALado() {
        // 9:00–10:00 y 9:30–10:30 se traslapan; 11:00 queda sola.
        List<DisposicionDia.Lugar> l = DisposicionDia.calcular(
                new int[]{m(9, 0), m(9, 30), m(11, 0)}, new int[]{m(10, 0), m(10, 30), m(11, 45)});
        assertEquals(2, l.get(0).columnas);
        assertEquals(2, l.get(1).columnas);
        assertNotEquals(l.get(0).columna, l.get(1).columna);
        assertEquals(1, l.get(2).columnas);
    }

    @Test
    public void columnaLiberadaSeReutiliza() {
        // A 9–11 ocupa la col. 0; B 9–9:30 la col. 1; C 9:30–10 reutiliza la col. 1.
        List<DisposicionDia.Lugar> l = DisposicionDia.calcular(
                new int[]{m(9, 0), m(9, 0), m(9, 30)}, new int[]{m(11, 0), m(9, 30), m(10, 0)});
        assertEquals(2, l.get(0).columnas);
        assertEquals(l.get(1).columna, l.get(2).columna);
    }

    @Test
    public void duracionMinimaVisible() {
        List<DisposicionDia.Lugar> l = DisposicionDia.calcular(new int[]{m(8, 0)}, new int[]{m(8, 0)});
        assertEquals(m(8, 15), l.get(0).fin);
    }

    @Test
    public void rangoDeHoras() {
        LocalDate d = LocalDate.of(2026, 11, 17);
        long a = Fechas.aMillis(d, LocalTime.of(9, 0)), b = Fechas.aMillis(d, LocalTime.of(9, 30));
        long c = Fechas.aMillis(d, LocalTime.of(12, 15));
        assertEquals("09:00 – 09:30", Fechas.rangoHoras(a, b, true));
        assertEquals("9:00 – 9:30 a. m.", Fechas.rangoHoras(a, b, false));
        assertEquals("9:00 a. m. – 12:15 p. m.", Fechas.rangoHoras(a, c, false));
        assertEquals("1 h 30 min", Fechas.duracion(90));
    }
}

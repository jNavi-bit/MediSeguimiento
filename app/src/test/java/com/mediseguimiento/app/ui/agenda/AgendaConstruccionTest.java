package com.mediseguimiento.app.ui.agenda;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.data.db.entity.Cita;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AgendaConstruccionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);
    private static final long AHORA = Fechas.aMillis(HOY, LocalTime.of(12, 0));

    private static Cita cita(long id, int dias, int hora, String estado) {
        Cita c = new Cita();
        c.id = id;
        c.especialidad = "E" + id;
        c.estado = estado;
        c.fechaHora = Fechas.aMillis(HOY.plusDays(dias), LocalTime.of(hora, 0));
        return c;
    }

    private static List<Cita> datos() {
        List<Cita> l = new ArrayList<>();
        l.add(cita(1, 2, 9, Cita.ESTADO_PROGRAMADA));
        l.add(cita(2, 0, 8, Cita.ESTADO_PROGRAMADA));   // hoy, ya pasó la hora: sigue en próximas
        l.add(cita(3, 2, 16, Cita.ESTADO_PROGRAMADA));
        l.add(cita(4, -3, 9, Cita.ESTADO_PROGRAMADA));  // pasada sin completar
        l.add(cita(5, -1, 9, Cita.ESTADO_COMPLETADA));
        l.add(cita(6, 5, 9, Cita.ESTADO_CANCELADA));
        return l;
    }

    @Test
    public void proximas_agrupadasPorDiaYOrdenadas() {
        AgendaViewModel.Estado e = AgendaViewModel.construir(datos(), AgendaViewModel.Filtro.PROXIMAS, null, AHORA);
        // [Hoy] 2, [+2] 1, 3
        assertEquals(5, e.items.size());
        assertEquals(ItemAgenda.ENCABEZADO, e.items.get(0).tipo);
        assertTrue(e.items.get(0).titulo.startsWith("Hoy"));
        assertEquals(2, e.items.get(1).cita.id);
        assertEquals(1, e.items.get(3).cita.id);
        assertEquals(3, e.items.get(4).cita.id);
        // Solo las programadas futuras marcan días en la tira (la cancelada no).
        assertEquals(2, e.diasConCitas.size());
    }

    @Test
    public void pasadas_masRecientePrimero() {
        AgendaViewModel.Estado e = AgendaViewModel.construir(datos(), AgendaViewModel.Filtro.PASADAS, null, AHORA);
        assertEquals(5, e.items.get(1).cita.id);
        assertEquals(4, e.items.get(3).cita.id);
    }

    @Test
    public void filtroPorDia() {
        long dia = HOY.plusDays(2).toEpochDay();
        AgendaViewModel.Estado e = AgendaViewModel.construir(datos(), AgendaViewModel.Filtro.PROXIMAS, dia, AHORA);
        assertEquals(3, e.items.size());
    }

    @Test
    public void canceladas() {
        AgendaViewModel.Estado e = AgendaViewModel.construir(datos(), AgendaViewModel.Filtro.CANCELADAS, null, AHORA);
        assertEquals(2, e.items.size());
        assertEquals(6, e.items.get(1).cita.id);
    }
}

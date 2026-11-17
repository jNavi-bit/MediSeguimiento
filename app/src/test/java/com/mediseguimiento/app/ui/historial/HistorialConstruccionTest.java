package com.mediseguimiento.app.ui.historial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.db.entity.Estudio;
import com.mediseguimiento.app.data.db.entity.PreparacionConsulta;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HistorialConstruccionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);
    private static final long AHORA = Fechas.aMillis(HOY, LocalTime.NOON);

    @Test
    public void citasCanceladasNoAparecenYLaPreparacionSeResume() {
        Cita a = new Cita(); a.id = 1; a.especialidad = "Cardiología"; a.estado = Cita.ESTADO_PROGRAMADA;
        a.fechaHora = Fechas.aMillis(HOY.plusDays(1), LocalTime.of(9, 0));
        Cita b = new Cita(); b.id = 2; b.especialidad = "Nutrición"; b.estado = Cita.ESTADO_CANCELADA;
        b.fechaHora = AHORA;
        PreparacionConsulta p = new PreparacionConsulta(); p.citaId = 1; p.motivo = "Control"; p.sintomas = "Mareo";
        List<HistorialViewModel.Item> it = HistorialViewModel.deCitas(Arrays.asList(a, b),
                Collections.singletonList(p), AHORA);
        assertEquals(1, it.size());
        assertEquals("Control", it.get(0).cuerpo);
        assertTrue(it.get(0).futura);
        assertEquals("Próxima · Preparación 2/5", it.get(0).etiqueta);
    }

    @Test
    public void agrupaPorMesEnOrdenDescendente() {
        Estudio e1 = new Estudio(); e1.id = 1; e1.nombre = "Biometría"; e1.fecha = Fechas.aMillis(HOY.minusMonths(1), LocalTime.NOON);
        Estudio e2 = new Estudio(); e2.id = 2; e2.nombre = "Orina"; e2.fecha = AHORA;
        List<HistorialViewModel.Item> todos = new ArrayList<>(HistorialViewModel.deEstudios(Arrays.asList(e1, e2)));
        todos.sort((x, y) -> Long.compare(y.fecha, x.fecha));
        List<HistorialViewModel.Item> g = HistorialViewModel.agruparPorMes(todos);
        assertEquals(4, g.size());
        assertEquals(HistorialViewModel.Item.MES, g.get(0).tipo);
        assertEquals("Noviembre 2026", g.get(0).titulo);
        assertEquals("Octubre 2026", g.get(2).titulo);
    }
}

package com.mediseguimiento.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.core.resumen.ResumenGenerador;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.db.entity.Estudio;
import com.mediseguimiento.app.data.db.entity.PreparacionConsulta;
import com.mediseguimiento.app.data.repo.PreparacionRepository;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

public class ResumenGeneradorTest {

    private static final long AHORA = Fechas.aMillis(LocalDate.of(2026, 11, 16), LocalTime.of(12, 0));

    private static ResumenGenerador.Datos datos() {
        ResumenGenerador.Datos d = new ResumenGenerador.Datos();
        d.paciente = "Ana López";
        Cita c = new Cita();
        c.id = 1;
        c.especialidad = "Cardiología";
        c.medico = "Dra. Ramírez";
        c.fechaHora = Fechas.aMillis(LocalDate.of(2026, 11, 17), LocalTime.of(9, 0));
        d.cita = c;
        return d;
    }

    private static Estudio estudio(long id, String nombre, String resultado) {
        Estudio e = new Estudio();
        e.id = id;
        e.nombre = nombre;
        e.resultado = resultado;
        e.fecha = AHORA - 86_400_000L * id;
        return e;
    }

    @Test
    public void sinPreparacion_marcaCamposSinRegistrar() {
        ResumenGenerador.Resumen r = ResumenGenerador.generar(datos(), AHORA, true);
        assertEquals(0, r.apartadosCompletos);
        ResumenGenerador.Seccion prep = r.secciones.get(2);
        assertEquals("preparacion", prep.id);
        assertTrue(prep.lineas.get(0).vacia);
        assertEquals(ResumenGenerador.SIN_REGISTRAR, prep.lineas.get(0).valor);
        // Sin estudios ni consultas previas, esas secciones no aparecen.
        assertEquals(3, r.secciones.size());
    }

    @Test
    public void preparacionCompleta_cuentaCincoYDaFormatoALasPreguntas() {
        ResumenGenerador.Datos d = datos();
        PreparacionConsulta p = new PreparacionConsulta();
        p.motivo = "Control de presión";
        p.sintomas = "Mareo ocasional";
        p.cambiosRecientes = "Bajé 2 kg";
        p.tratamientos = "Losartán 50 mg";
        p.preguntas = "¿Ajusto la dosis?\n- ¿Puedo hacer ejercicio?";
        d.preparacion = p;
        ResumenGenerador.Resumen r = ResumenGenerador.generar(d, AHORA, true);
        assertEquals(5, r.apartadosCompletos);
        assertEquals(5, PreparacionRepository.completados(p));
        String preguntas = r.secciones.get(2).lineas.get(4).valor;
        assertEquals("• ¿Ajusto la dosis?\n• ¿Puedo hacer ejercicio?", preguntas);
        assertTrue(r.texto.contains("Control de presión"));
        assertTrue(r.texto.contains("No sustituye el expediente clínico"));
    }

    @Test
    public void estudios_vinculadosPrimeroYSinDuplicados() {
        ResumenGenerador.Datos d = datos();
        Estudio vinculado = estudio(2, "Perfil de lípidos", "LDL 130");
        d.estudiosVinculados = Collections.singletonList(vinculado);
        d.estudiosRecientes = Arrays.asList(estudio(1, "Química sanguínea", "Glucosa 110"), vinculado);
        ResumenGenerador.Resumen r = ResumenGenerador.generar(d, AHORA, true);
        ResumenGenerador.Seccion est = r.secciones.get(3);
        assertEquals("estudios", est.id);
        assertEquals(2, est.lineas.size());
        assertEquals("Perfil de lípidos", est.lineas.get(0).etiqueta);
        assertFalse(est.lineas.get(1).vacia);
    }
}

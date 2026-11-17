package com.mediseguimiento.app.core.demo;

import android.content.Context;

import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.core.recordatorios.Recordatorios;
import com.mediseguimiento.app.core.seguridad.ControlIntentos;
import com.mediseguimiento.app.core.seguridad.PasswordHasher;
import com.mediseguimiento.app.data.db.AppDatabase;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.db.entity.Consentimiento;
import com.mediseguimiento.app.data.db.entity.Estudio;
import com.mediseguimiento.app.data.db.entity.Paciente;
import com.mediseguimiento.app.data.db.entity.PreparacionConsulta;
import com.mediseguimiento.app.data.db.entity.Recordatorio;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuenta de demostración con datos ficticios para presentar la app "llena".
 * Todas las fechas son relativas al día de hoy, así que siempre se ve actual.
 * Llamar SIEMPRE desde un hilo de fondo: deriva una contraseña y escribe en Room.
 */
public final class DatosDemo {

    public static final String CORREO = "demo@mediseguimiento.mx";
    public static final String PASSWORD = "Demo2026";

    private DatosDemo() {}

    private static final class Ctx {
        final AppDatabase db;
        final long pid;
        final LocalDate hoy = LocalDate.now(Fechas.zona());
        final long ahora = System.currentTimeMillis();
        final List<long[]> avisos = new ArrayList<>(); // {citaId, fechaHora, minutos}

        Ctx(AppDatabase db, long pid) { this.db = db; this.pid = pid; }

        long cita(int dias, int h, int m, int dur, String esp, String medico, String lugar, String estado, String notas, int aviso) {
            Cita c = new Cita();
            c.pacienteId = pid;
            c.fechaHora = Fechas.aMillis(hoy.plusDays(dias), LocalTime.of(h, m));
            c.duracionMin = dur;
            c.especialidad = esp;
            c.medico = medico;
            c.lugar = lugar;
            c.notas = notas;
            c.estado = estado;
            c.creadoEn = ahora;
            c.actualizadoEn = ahora;
            long id = db.citaDao().insertar(c);
            if (aviso > 0 && Cita.ESTADO_PROGRAMADA.equals(estado)) {
                Recordatorio r = new Recordatorio();
                r.citaId = id;
                r.minutosAntes = aviso;
                r.disparoEn = Recordatorios.disparo(c.fechaHora, aviso);
                db.recordatorioDao().insertar(r);
                avisos.add(new long[]{id, c.fechaHora, aviso});
            }
            return id;
        }

        void prep(long citaId, String motivo, String sintomas, String cambios, String tratamientos, String preguntas) {
            PreparacionConsulta p = new PreparacionConsulta();
            p.citaId = citaId;
            p.motivo = motivo;
            p.sintomas = sintomas;
            p.cambiosRecientes = cambios;
            p.tratamientos = tratamientos;
            p.preguntas = preguntas;
            p.actualizadoEn = ahora;
            db.preparacionDao().guardar(p);
        }

        void estudio(int dias, String nombre, String lab, String resultado, Long citaId) {
            Estudio e = new Estudio();
            e.pacienteId = pid;
            e.citaId = citaId;
            e.nombre = nombre;
            e.laboratorio = lab;
            e.resultado = resultado;
            e.fecha = Fechas.aMillis(hoy.plusDays(dias), LocalTime.NOON);
            e.creadoEn = ahora;
            db.estudioDao().insertar(e);
        }
    }

    /** Crea (o reinicia) la cuenta de demostración y devuelve el id del paciente. */
    public static long crear(Context context) {
        Context app = context.getApplicationContext();
        AppDatabase db = AppDatabase.get(app);
        PasswordHasher.Derivado d = PasswordHasher.derivar(PASSWORD.toCharArray());
        final List<long[]> avisos = new ArrayList<>();
        long pid = db.runInTransaction(() -> {
            Paciente previo = db.pacienteDao().buscarPorCorreo(CORREO);
            if (previo != null) db.pacienteDao().eliminar(previo.id); // borra en cascada sus datos
            Paciente p = new Paciente();
            p.nombre = "Ana López Martínez";
            p.correo = CORREO;
            p.passwordHash = d.hash;
            p.sal = d.sal;
            p.padecimiento = "Diabetes tipo 2 e hipertensión arterial";
            p.creadoEn = Fechas.aMillis(LocalDate.now(Fechas.zona()).minusMonths(3), LocalTime.NOON);
            long id = db.pacienteDao().insertar(p);
            Ctx x = new Ctx(db, id);
            String P = Cita.ESTADO_PROGRAMADA, C = Cita.ESTADO_COMPLETADA, X = Cita.ESTADO_CANCELADA;

            // ---- Consultas pasadas ----
            long mi = x.cita(-62, 10, 0, 45, "Medicina interna", "Dr. Jorge Hernández", "UMF 34 IMSS, consultorio 2", C, null, 0);
            x.prep(mi, "Revisión inicial de control de glucosa", "Sed frecuente y cansancio por las tardes",
                    "Subí 3 kg en los últimos meses", "Metformina 850 mg cada 12 h", "¿Necesito estudios de laboratorio?\n¿Debo cambiar mi dieta?");
            long en = x.cita(-31, 9, 30, 30, "Endocrinología", "Dra. Laura Ramírez", "Hospital Civil, piso 2", C, null, 0);
            x.prep(en, "Resultados de hemoglobina glucosilada", "Mareo leve al despertar, 2 veces por semana",
                    "Empecé a caminar 20 minutos diarios", "Metformina 850 mg cada 12 h\nLosartán 50 mg cada 24 h",
                    "¿Mi HbA1c está en rango?\n¿Puedo bajar la dosis si sigo mejorando?");
            x.cita(-14, 12, 0, 45, "Nutrición", "Lic. Sofía Torres", "Clínica Norte, consultorio 8", C, "Llevar registro de alimentos de una semana", 0);
            x.cita(-7, 7, 30, 15, "Laboratorio clínico", null, "Laboratorio Central", C, "Ayuno de 8 horas", 0);
            x.cita(-2, 11, 0, 30, "Oftalmología", "Dr. Andrés Pérez", "Hospital Civil, consulta externa", P, "Revisión de fondo de ojo", 0);

            // ---- Hoy ----
            x.cita(0, 11, 0, 30, "Medicina familiar", "Dra. Elena Ruiz", "UMF 34 IMSS, consultorio 5", P, null, 0);
            x.cita(0, 17, 30, 45, "Psicología", "Mtra. Paola Vega", "Centro de Salud Mental", P, null, 15);

            // ---- Mañana: dos citas que se traslapan ----
            long ca = x.cita(1, 9, 0, 60, "Cardiología", "Dra. Laura Ramírez", "Clínica Norte, consultorio 4", P,
                    "Llevar el registro de presión arterial y los estudios recientes", 60);
            x.prep(ca, "Control de presión arterial y revisión de estudios",
                    "Palpitaciones ocasionales al subir escaleras; dolor de cabeza leve por las mañanas",
                    "Bajé 2 kg en el último mes y duermo mejor",
                    "Losartán 50 mg cada 24 h por la mañana\nMetformina 850 mg cada 12 h",
                    "¿Las palpitaciones son por la presión?\n¿Puedo hacer ejercicio más intenso?\n¿Cada cuánto me tomo la presión en casa?");
            long lab = x.cita(1, 9, 30, 30, "Laboratorio clínico", null, "Laboratorio Central", P, "Química sanguínea de control", 60);
            x.cita(1, 12, 0, 45, "Nutrición", "Lic. Sofía Torres", "Clínica Norte, consultorio 8", P, null, 60);

            // ---- Próximas ----
            long ne = x.cita(3, 10, 30, 45, "Nefrología", "Dr. Ricardo Castillo", "Hospital Civil, piso 3", P, null, 1440);
            x.prep(ne, "Revisión de función renal", "Hinchazón leve en tobillos por la noche", null, null, null);
            x.cita(4, 17, 0, 50, "Psicología", "Mtra. Paola Vega", "Centro de Salud Mental", X, null, 0);
            x.cita(7, 8, 0, 30, "Medicina interna", "Dr. Jorge Hernández", "UMF 34 IMSS, consultorio 2", P, null, 1440);
            x.cita(14, 13, 0, 30, "Oftalmología", "Dr. Andrés Pérez", "Hospital Civil, consulta externa", P, null, 1440);
            x.cita(30, 10, 0, 30, "Endocrinología", "Dra. Laura Ramírez", "Hospital Civil, piso 2", P, null, 1440);

            // ---- Estudios ----
            x.estudio(-6, "Química sanguínea", "Laboratorio Central", "Glucosa 128 mg/dL, colesterol total 212 mg/dL, triglicéridos 180 mg/dL", ca);
            x.estudio(-6, "Hemoglobina glucosilada (HbA1c)", "Laboratorio Central", "HbA1c 7.2 %", ca);
            x.estudio(-20, "Examen general de orina", "Laboratorio Central", "Sin hallazgos relevantes reportados", ne);
            x.estudio(-35, "Electrocardiograma", "Clínica Norte", "Ritmo sinusal, sin alteraciones reportadas", ca);
            x.estudio(-40, "Perfil de lípidos", "Laboratorio Central", "LDL 135 mg/dL, HDL 42 mg/dL", null);
            x.estudio(-63, "Biometría hemática", "Laboratorio Central", "Hemoglobina 13.1 g/dL", mi);

            // ---- Consentimiento ya otorgado ----
            Consentimiento k = new Consentimiento();
            k.pacienteId = id;
            k.citaId = en;
            k.destinatario = "Dra. Laura Ramírez";
            k.otorgadoEn = x.ahora - 31L * 86_400_000;
            db.consentimientoDao().insertar(k);

            avisos.addAll(x.avisos);
            return id;
        });
        // Después de confirmar la transacción se programan los avisos con WorkManager.
        for (long[] a : avisos) Recordatorios.programar(app, a[0], a[1], (int) a[2]);
        new ControlIntentos(app).reiniciar(CORREO);
        return pid;
    }
}

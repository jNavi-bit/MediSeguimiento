package com.mediseguimiento.app.data.repo;

import android.content.Context;

import com.mediseguimiento.app.core.AppExecutors;
import com.mediseguimiento.app.core.Resultado;
import com.mediseguimiento.app.core.Sesion;
import com.mediseguimiento.app.core.resumen.ResumenGenerador;
import com.mediseguimiento.app.data.db.AppDatabase;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.db.entity.Consentimiento;
import com.mediseguimiento.app.data.db.entity.Estudio;
import com.mediseguimiento.app.data.db.entity.Paciente;
import com.mediseguimiento.app.data.db.entity.PreparacionConsulta;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Reúne la información de una cita y genera su resumen (RF-05, RF-07).
 *
 * <p>Las cinco consultas son independientes entre sí, así que se lanzan <b>en paralelo</b>
 * con {@code invokeAll} sobre el grupo de E/S (WAL permite varias lecturas simultáneas).
 * Cuando todas terminan, el texto se compone en el grupo de cómputo y el resultado se
 * publica en el hilo principal.</p>
 */
public class ResumenRepository {

    private static final long SEIS_MESES_MS = 183L * 24 * 60 * 60 * 1000;

    private final AppDatabase db;
    private final AppExecutors ex = AppExecutors.get();
    private final long pacienteId;

    public ResumenRepository(Context context) {
        db = AppDatabase.get(context);
        pacienteId = new Sesion(context).pacienteId();
    }

    public void generar(long citaId, boolean h24, AuthRepository.Callback<ResumenGenerador.Resumen> cb) {
        // La coordinación se hace en un hilo de cómputo para no bloquear el grupo de E/S
        // mientras espera a que terminen sus propias tareas.
        ex.computo().execute(() -> {
            Resultado<ResumenGenerador.Resumen> r;
            try {
                long ahora = System.currentTimeMillis();
                Cita cita = db.citaDao().obtener(citaId, pacienteId);
                if (cita == null) throw new IllegalStateException("La cita ya no existe");

                Callable<Paciente> qPaciente = () -> db.pacienteDao().obtener(pacienteId);
                Callable<PreparacionConsulta> qPrep = () -> db.preparacionDao().obtenerPorCita(citaId);
                Callable<List<Estudio>> qVinculados = () -> db.estudioDao().listarPorCita(citaId);
                Callable<List<Estudio>> qRecientes = () ->
                        db.estudioDao().listarRecientes(pacienteId, cita.fechaHora - SEIS_MESES_MS, 6);
                Callable<List<Cita>> qAnteriores = () ->
                        db.citaDao().listarAnteriores(pacienteId, cita.fechaHora, 3);

                @SuppressWarnings("unchecked")
                List<Future<Object>> fs = ex.io().invokeAll(Arrays.asList(
                        (Callable<Object>) (Callable<?>) qPaciente,
                        (Callable<Object>) (Callable<?>) qPrep,
                        (Callable<Object>) (Callable<?>) qVinculados,
                        (Callable<Object>) (Callable<?>) qRecientes,
                        (Callable<Object>) (Callable<?>) qAnteriores), 10, TimeUnit.SECONDS);

                ResumenGenerador.Datos d = new ResumenGenerador.Datos();
                Paciente p = (Paciente) fs.get(0).get();
                d.paciente = p != null ? p.nombre : "";
                d.padecimiento = p != null ? p.padecimiento : null;
                d.cita = cita;
                d.preparacion = (PreparacionConsulta) fs.get(1).get();
                d.estudiosVinculados = lista(fs.get(2).get());
                d.estudiosRecientes = lista(fs.get(3).get());
                d.anteriores = lista(fs.get(4).get());
                r = Resultado.exito(ResumenGenerador.generar(d, ahora, h24));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                r = Resultado.error("Se interrumpió la generación del resumen");
            } catch (Exception e) {
                r = Resultado.error(e.getMessage() != null ? e.getMessage() : "No se pudo generar el resumen");
            }
            Resultado<ResumenGenerador.Resumen> fin = r;
            ex.main().execute(() -> cb.alTerminar(fin));
        });
    }

    /** Registra el consentimiento explícito antes de compartir (RF-07). */
    public void registrarConsentimiento(long citaId, String destinatario, AuthRepository.Callback<Long> cb) {
        ex.io().execute(() -> {
            Consentimiento c = new Consentimiento();
            c.pacienteId = pacienteId;
            c.citaId = citaId;
            c.destinatario = destinatario == null || destinatario.trim().isEmpty() ? null : destinatario.trim();
            c.otorgadoEn = System.currentTimeMillis();
            Resultado<Long> r;
            try {
                r = Resultado.exito(db.consentimientoDao().insertar(c));
            } catch (RuntimeException e) {
                r = Resultado.error("No se pudo registrar el consentimiento");
            }
            Resultado<Long> fin = r;
            ex.main().execute(() -> cb.alTerminar(fin));
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> lista(Object o) { return (List<T>) o; }
}

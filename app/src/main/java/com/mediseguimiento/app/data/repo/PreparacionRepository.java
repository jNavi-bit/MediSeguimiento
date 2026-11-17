package com.mediseguimiento.app.data.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.mediseguimiento.app.core.AppExecutors;
import com.mediseguimiento.app.core.Resultado;
import com.mediseguimiento.app.core.Sesion;
import com.mediseguimiento.app.data.db.AppDatabase;
import com.mediseguimiento.app.data.db.entity.PreparacionConsulta;

import java.util.List;

/** Preparación previa a la consulta (RF-03). */
public class PreparacionRepository {

    /** Número de apartados que componen una preparación completa. */
    public static final int APARTADOS = 5;

    private final AppDatabase db;
    private final AppExecutors ex = AppExecutors.get();
    private final long pacienteId;

    public PreparacionRepository(Context context) {
        db = AppDatabase.get(context);
        pacienteId = new Sesion(context).pacienteId();
    }

    public LiveData<PreparacionConsulta> observar(long citaId) {
        return db.preparacionDao().observarPorCita(citaId);
    }

    public LiveData<List<PreparacionConsulta>> observarTodas() {
        return db.preparacionDao().observarPorPaciente(pacienteId);
    }

    public LiveData<Integer> contar() { return db.preparacionDao().contar(pacienteId); }

    /**
     * Guarda en una transacción: se verifica que la cita pertenezca al paciente y se
     * conserva el identificador de la preparación existente (sin duplicados).
     */
    public void guardar(PreparacionConsulta p, AuthRepository.Callback<Long> cb) {
        ex.io().execute(() -> {
            Resultado<Long> r;
            try {
                long id = db.runInTransaction(() -> {
                    if (db.citaDao().obtener(p.citaId, pacienteId) == null) {
                        throw new IllegalStateException("La cita ya no existe");
                    }
                    PreparacionConsulta previa = db.preparacionDao().obtenerPorCita(p.citaId);
                    if (previa != null) p.id = previa.id;
                    p.actualizadoEn = System.currentTimeMillis();
                    return db.preparacionDao().guardar(p);
                });
                r = Resultado.exito(id);
            } catch (RuntimeException e) {
                r = Resultado.error(e.getMessage() != null ? e.getMessage() : "No se pudo guardar");
            }
            Resultado<Long> fin = r;
            ex.main().execute(() -> cb.alTerminar(fin));
        });
    }

    /** Cuántos de los cinco apartados tienen contenido. */
    public static int completados(PreparacionConsulta p) {
        if (p == null) return 0;
        int n = 0;
        for (String s : new String[]{p.motivo, p.sintomas, p.cambiosRecientes, p.tratamientos, p.preguntas}) {
            if (s != null && !s.trim().isEmpty()) n++;
        }
        return n;
    }
}

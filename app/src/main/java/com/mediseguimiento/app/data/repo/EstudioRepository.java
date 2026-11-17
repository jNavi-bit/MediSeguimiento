package com.mediseguimiento.app.data.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.mediseguimiento.app.core.AppExecutors;
import com.mediseguimiento.app.core.Resultado;
import com.mediseguimiento.app.core.Sesion;
import com.mediseguimiento.app.data.db.AppDatabase;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.db.entity.Estudio;

import java.util.List;

/** Estudios y resultados aportados por el paciente (RF-04, RF-08). */
public class EstudioRepository {

    private final AppDatabase db;
    private final AppExecutors ex = AppExecutors.get();
    private final long pacienteId;

    public EstudioRepository(Context context) {
        db = AppDatabase.get(context);
        pacienteId = new Sesion(context).pacienteId();
    }

    public LiveData<List<Estudio>> observarTodos() { return db.estudioDao().observarTodos(pacienteId); }

    public LiveData<Integer> contar() { return db.estudioDao().contar(pacienteId); }

    public void obtener(long id, AuthRepository.Callback<Estudio> cb) {
        ex.io().execute(() -> {
            Estudio e = db.estudioDao().obtener(id, pacienteId);
            Resultado<Estudio> r = e != null ? Resultado.exito(e) : Resultado.error("El estudio ya no existe");
            ex.main().execute(() -> cb.alTerminar(r));
        });
    }

    /** Citas no canceladas para vincular un estudio. */
    public void citasVinculables(AuthRepository.Callback<List<Cita>> cb) {
        ex.io().execute(() -> {
            List<Cita> l = db.citaDao().listarCronologico(pacienteId);
            l.removeIf(c -> Cita.ESTADO_CANCELADA.equals(c.estado));
            ex.main().execute(() -> cb.alTerminar(Resultado.exito(l)));
        });
    }

    /** Inserta o actualiza dentro de una transacción, validando la pertenencia de la cita vinculada. */
    public void guardar(Estudio e, AuthRepository.Callback<Long> cb) {
        ex.io().execute(() -> {
            Resultado<Long> r;
            try {
                long id = db.runInTransaction(() -> {
                    e.pacienteId = pacienteId;
                    if (e.citaId != null && db.citaDao().obtener(e.citaId, pacienteId) == null) e.citaId = null;
                    if (e.id == 0) {
                        e.creadoEn = System.currentTimeMillis();
                        return db.estudioDao().insertar(e);
                    }
                    if (db.estudioDao().obtener(e.id, pacienteId) == null) {
                        throw new IllegalStateException("El estudio ya no existe");
                    }
                    db.estudioDao().actualizar(e);
                    return e.id;
                });
                r = Resultado.exito(id);
            } catch (RuntimeException ex2) {
                r = Resultado.error(ex2.getMessage() != null ? ex2.getMessage() : "No se pudo guardar");
            }
            Resultado<Long> fin = r;
            ex.main().execute(() -> cb.alTerminar(fin));
        });
    }

    public void eliminar(long id, AuthRepository.Callback<Long> cb) {
        ex.io().execute(() -> {
            int n = db.estudioDao().eliminar(id, pacienteId);
            Resultado<Long> r = n > 0 ? Resultado.exito(id) : Resultado.error("El estudio ya no existe");
            ex.main().execute(() -> cb.alTerminar(r));
        });
    }
}

package com.mediseguimiento.app.data.repo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.mediseguimiento.app.core.AppExecutors;
import com.mediseguimiento.app.core.Resultado;
import com.mediseguimiento.app.core.Sesion;
import com.mediseguimiento.app.data.db.AppDatabase;
import com.mediseguimiento.app.data.db.dao.CitaDao;
import com.mediseguimiento.app.core.recordatorios.Recordatorios;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.db.entity.Recordatorio;

import java.util.List;

/**
 * Agenda personal de citas (RF-02, RF-08). Todas las operaciones se ejecutan en el
 * grupo de E/S y cada escritura compuesta corre dentro de una transacción de Room,
 * de modo que la comprobación y la modificación son atómicas aunque coincidan
 * varias solicitudes (RNF-04).
 */
public class CitaRepository {

    /** Motivo de un fallo al guardar, para que la interfaz reaccione adecuadamente. */
    public enum Falla { DUPLICADA, CONFLICTO_VERSION, NO_EXISTE, DESCONOCIDA }

    public static final class ErrorCita extends RuntimeException {
        public final Falla falla;

        ErrorCita(Falla falla, String mensaje) {
            super(mensaje);
            this.falla = falla;
        }
    }

    private final AppDatabase db;
    private final CitaDao dao;
    private final AppExecutors ex;
    private final long pacienteId;
    /** Contexto para programar recordatorios; es null en las pruebas de base de datos. */
    @Nullable private final Context app;

    public CitaRepository(Context context) {
        this(AppDatabase.get(context), new Sesion(context).pacienteId(), context.getApplicationContext());
    }

    /** Constructor para pruebas: base de datos en memoria y paciente explícito. */
    public CitaRepository(AppDatabase db, long pacienteId) {
        this(db, pacienteId, null);
    }

    private CitaRepository(AppDatabase db, long pacienteId, @Nullable Context app) {
        this.app = app;
        this.db = db;
        this.dao = db.citaDao();
        this.ex = AppExecutors.get();
        this.pacienteId = pacienteId;
    }

    public LiveData<List<Cita>> observarTodas() { return dao.observarTodas(pacienteId); }

    public LiveData<Cita> observar(long id) { return dao.observar(id, pacienteId); }

    public LiveData<Cita> observarProxima(long desde) { return dao.observarProxima(pacienteId, desde); }

    public void obtener(long id, AuthRepository.Callback<Cita> cb) {
        ex.io().execute(() -> {
            Cita c = dao.obtener(id, pacienteId);
            if (c != null) {
                List<Recordatorio> rs = db.recordatorioDao().activosPorCita(id);
                c.recordatorioMin = rs.isEmpty() ? Cita.RECORDATORIO_NINGUNO : rs.get(0).minutosAntes;
            }
            Resultado<Cita> r = c != null ? Resultado.exito(c) : Resultado.error("La cita ya no existe");
            ex.main().execute(() -> cb.alTerminar(r));
        });
    }

    /** Crea una cita; rechaza duplicados exactos (misma especialidad a la misma hora). */
    public void crear(@NonNull Cita nueva, AuthRepository.Callback<Long> cb) {
        ex.io().execute(() -> entregar(cb, () -> {
            long id = crearSincrono(nueva);
            reprogramar(id);
            return id;
        }));
    }

    /** Actualiza con control optimista: falla si otra operación modificó la cita antes. */
    public void actualizar(@NonNull Cita editada, AuthRepository.Callback<Long> cb) {
        ex.io().execute(() -> entregar(cb, () -> {
            long id = actualizarSincrono(editada);
            reprogramar(id);
            return id;
        }));
    }

    public void cambiarEstado(long id, @NonNull String estado, AuthRepository.Callback<Long> cb) {
        ex.io().execute(() -> entregar(cb, () -> {
            int n = dao.cambiarEstado(id, pacienteId, estado, System.currentTimeMillis());
            if (n == 0) throw new ErrorCita(Falla.NO_EXISTE, "La cita ya no existe");
            reprogramar(id); // cancelar/completar quita el aviso; reactivar lo restaura
            return id;
        }));
    }

    public void eliminar(long id, AuthRepository.Callback<Long> cb) {
        ex.io().execute(() -> entregar(cb, () -> {
            // Las preparaciones, recordatorios y consentimientos se eliminan en cascada.
            int n = dao.eliminar(id, pacienteId);
            if (n == 0) throw new ErrorCita(Falla.NO_EXISTE, "La cita ya no existe");
            if (app != null) Recordatorios.cancelar(app, id);
            return id;
        }));
    }

    // ===== Versiones síncronas (se llaman desde un hilo de fondo; también las usan las pruebas) =====

    public long crearSincrono(Cita nueva) {
        return db.runInTransaction(() -> {
            if (dao.contarDuplicadas(pacienteId, nueva.fechaHora, nueva.especialidad, -1) > 0) {
                throw new ErrorCita(Falla.DUPLICADA, "Ya tienes una cita de " + nueva.especialidad + " a esa hora");
            }
            long ahora = System.currentTimeMillis();
            nueva.pacienteId = pacienteId;
            nueva.estado = Cita.ESTADO_PROGRAMADA;
            nueva.creadoEn = ahora;
            nueva.actualizadoEn = ahora;
            nueva.version = 0;
            long id = dao.insertar(nueva);
            guardarRecordatorio(id, nueva);
            return id;
        });
    }

    public long actualizarSincrono(Cita c) {
        return db.runInTransaction(() -> {
            if (dao.contarDuplicadas(pacienteId, c.fechaHora, c.especialidad, c.id) > 0) {
                throw new ErrorCita(Falla.DUPLICADA, "Ya tienes una cita de " + c.especialidad + " a esa hora");
            }
            int n = dao.actualizarSiVersion(c.id, pacienteId, c.version, c.fechaHora, c.especialidad,
                    c.medico, c.lugar, c.notas, c.estado, c.duracionMin, System.currentTimeMillis());
            if (n == 0) {
                if (dao.obtener(c.id, pacienteId) == null) {
                    throw new ErrorCita(Falla.NO_EXISTE, "La cita ya no existe");
                }
                throw new ErrorCita(Falla.CONFLICTO_VERSION,
                        "La cita cambió mientras la editabas. Revisa la versión más reciente.");
            }
            guardarRecordatorio(c.id, c);
            return c.id;
        });
    }

    /** Dentro de la transacción: sustituye el recordatorio activo por el elegido. */
    private void guardarRecordatorio(long citaId, Cita c) {
        if (c.recordatorioMin == Cita.RECORDATORIO_SIN_CAMBIO) return;
        db.recordatorioDao().desactivarPorCita(citaId);
        if (c.recordatorioMin >= 0) {
            Recordatorio r = new Recordatorio();
            r.citaId = citaId;
            r.minutosAntes = c.recordatorioMin;
            r.disparoEn = Recordatorios.disparo(c.fechaHora, c.recordatorioMin);
            db.recordatorioDao().insertar(r);
        }
    }

    /**
     * Después de confirmar la transacción, sincroniza WorkManager con la base de datos:
     * solo una cita programada con recordatorio activo conserva su aviso.
     */
    private void reprogramar(long citaId) {
        if (app == null) return;
        Cita c = dao.obtener(citaId, pacienteId);
        List<Recordatorio> activos = db.recordatorioDao().activosPorCita(citaId);
        if (c == null || !Cita.ESTADO_PROGRAMADA.equals(c.estado) || activos.isEmpty()) {
            Recordatorios.cancelar(app, citaId);
        } else {
            Recordatorios.programar(app, citaId, c.fechaHora, activos.get(0).minutosAntes);
        }
    }

    private interface Operacion { long ejecutar(); }

    private void entregar(AuthRepository.Callback<Long> cb, Operacion op) {
        Resultado<Long> r;
        try {
            r = Resultado.exito(op.ejecutar());
        } catch (ErrorCita e) {
            r = Resultado.error(e.getMessage() == null ? "" : e.getMessage(), e.falla);
        } catch (RuntimeException e) {
            r = Resultado.error("No se pudo guardar. Intenta de nuevo.", Falla.DESCONOCIDA);
        }
        Resultado<Long> fin = r;
        ex.main().execute(() -> cb.alTerminar(fin));
    }

    @Nullable
    public static Falla falla(Resultado<?> r) {
        return r.detalle instanceof Falla ? (Falla) r.detalle : null;
    }
}

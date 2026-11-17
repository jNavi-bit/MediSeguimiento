package com.mediseguimiento.app.ui.cita;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mediseguimiento.app.core.Resultado;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.repo.CitaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicBoolean;

/** Estado del formulario de cita; sobrevive a rotaciones. */
public class CitaFormViewModel extends AndroidViewModel {

    private final CitaRepository repo;
    private final MutableLiveData<Resultado<Long>> guardado = new MutableLiveData<>();
    private final MutableLiveData<Cita> cargada = new MutableLiveData<>();
    private final AtomicBoolean ocupado = new AtomicBoolean(false);

    /** Selección actual de fecha y hora (independiente de los campos de texto). */
    public LocalDate fecha;
    public LocalTime hora;
    /** Minutos de anticipación del aviso; -1 = sin aviso. */
    public int recordatorioMin = 24 * 60;
    /** Duración estimada de la cita en minutos. */
    public int duracionMin = 30;
    /** Instantánea inicial para detectar cambios sin guardar. */
    public String instantaneaInicial;
    public boolean inicializado;

    public CitaFormViewModel(@NonNull Application app) {
        super(app);
        repo = new CitaRepository(app);
    }

    public LiveData<Resultado<Long>> guardado() { return guardado; }

    public LiveData<Cita> cargada() { return cargada; }

    public void cargar(long id) {
        repo.obtener(id, r -> cargada.setValue(r.datos));
    }

    @Nullable public Cita original() { return cargada.getValue(); }

    public boolean ocupado() { return ocupado.get(); }

    public void guardar(Cita c, boolean nueva) {
        if (!ocupado.compareAndSet(false, true)) return;
        guardado.setValue(Resultado.cargando());
        AuthCallback cb = r -> {
            ocupado.set(false);
            guardado.setValue(r);
        };
        if (nueva) repo.crear(c, cb::alTerminar);
        else repo.actualizar(c, cb::alTerminar);
    }

    public void consumir() { guardado.setValue(null); }

    private interface AuthCallback { void alTerminar(Resultado<Long> r); }
}

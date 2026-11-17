package com.mediseguimiento.app.ui.inicio;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.repo.CitaRepository;

/**
 * Próxima cita del paciente. La consulta depende de "ahora"; al volver a la pantalla se
 * actualiza ese instante para que una cita que ya pasó deje de mostrarse.
 */
public class InicioViewModel extends AndroidViewModel {

    private final MutableLiveData<Long> ahora = new MutableLiveData<>(System.currentTimeMillis());
    private final LiveData<Cita> proxima;

    public InicioViewModel(@NonNull Application app) {
        super(app);
        CitaRepository repo = new CitaRepository(app);
        // Se incluyen las citas de hoy aunque su hora ya haya pasado hace poco (1 h).
        proxima = Transformations.switchMap(ahora, t -> repo.observarProxima(t - 60 * 60 * 1000L));
    }

    public LiveData<Cita> proxima() { return proxima; }

    public void refrescar() {
        long t = System.currentTimeMillis();
        Long previo = ahora.getValue();
        // Evita recrear la consulta si no ha cambiado el minuto.
        if (previo == null || t / 60_000 != previo / 60_000) ahora.setValue(t);
    }

}

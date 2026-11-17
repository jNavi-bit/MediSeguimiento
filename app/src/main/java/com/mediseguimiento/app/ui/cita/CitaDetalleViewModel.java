package com.mediseguimiento.app.ui.cita;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.repo.AuthRepository;
import com.mediseguimiento.app.data.repo.CitaRepository;

/** Observa una cita y expone sus acciones (cambiar estado, eliminar). */
public class CitaDetalleViewModel extends AndroidViewModel {

    private final CitaRepository repo;
    private LiveData<Cita> cita;
    private long id = -1;

    public CitaDetalleViewModel(@NonNull Application app) {
        super(app);
        repo = new CitaRepository(app);
    }

    public LiveData<Cita> cita(long id) {
        if (cita == null || this.id != id) {
            this.id = id;
            cita = repo.observar(id);
        }
        return cita;
    }

    public void cambiarEstado(String estado, AuthRepository.Callback<Long> cb) {
        repo.cambiarEstado(id, estado, cb);
    }

    public void eliminar(AuthRepository.Callback<Long> cb) { repo.eliminar(id, cb); }
}

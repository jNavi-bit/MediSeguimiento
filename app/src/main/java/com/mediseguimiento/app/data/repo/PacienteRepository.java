package com.mediseguimiento.app.data.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.mediseguimiento.app.data.db.AppDatabase;
import com.mediseguimiento.app.data.db.entity.Paciente;

/** Acceso a los datos del perfil del paciente. Room entrega el LiveData en segundo plano. */
public class PacienteRepository {

    private final AppDatabase db;

    public PacienteRepository(Context context) {
        db = AppDatabase.get(context);
    }

    public LiveData<Paciente> observar(long id) {
        return db.pacienteDao().observar(id);
    }
}

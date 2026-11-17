package com.mediseguimiento.app.ui.common;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.mediseguimiento.app.core.Sesion;
import com.mediseguimiento.app.data.db.entity.Paciente;
import com.mediseguimiento.app.data.repo.PacienteRepository;

/** Expone el paciente de la sesión actual. Compartido por la actividad y sus pestañas. */
public class PacienteViewModel extends AndroidViewModel {

    private final LiveData<Paciente> paciente;

    public PacienteViewModel(@NonNull Application app) {
        super(app);
        paciente = new PacienteRepository(app).observar(new Sesion(app).pacienteId());
    }

    public LiveData<Paciente> paciente() { return paciente; }

    /** Primer nombre para saludos ("Ana María López" → "Ana"). */
    public static String primerNombre(Paciente p) {
        if (p == null || p.nombre.trim().isEmpty()) return "";
        return p.nombre.trim().split("\\s+")[0];
    }

    /** Iniciales para el avatar ("Ana López" → "AL"). */
    public static String iniciales(Paciente p) {
        if (p == null) return "";
        String[] partes = p.nombre.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length && sb.length() < 2; i++) {
            if (!partes[i].isEmpty()) sb.appendCodePoint(partes[i].codePointAt(0));
        }
        return sb.toString().toUpperCase();
    }
}

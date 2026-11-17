package com.mediseguimiento.app.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mediseguimiento.app.core.Resultado;
import com.mediseguimiento.app.data.repo.AuthRepository;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Estado de la pantalla de acceso. Sobrevive a rotaciones y evita que un doble toque
 * dispare dos solicitudes simultáneas ({@link AtomicBoolean}).
 */
public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repo;
    private final MutableLiveData<Resultado<Long>> estado = new MutableLiveData<>();
    private final MutableLiveData<Boolean> modoRegistro = new MutableLiveData<>(false);
    private final AtomicBoolean ocupado = new AtomicBoolean(false);

    public AuthViewModel(@NonNull Application app) {
        super(app);
        repo = new AuthRepository(app);
    }

    public LiveData<Resultado<Long>> estado() { return estado; }

    public LiveData<Boolean> modoRegistro() { return modoRegistro; }

    public void setModoRegistro(boolean registro) {
        if (!Boolean.valueOf(registro).equals(modoRegistro.getValue())) modoRegistro.setValue(registro);
    }

    public boolean ocupado() { return ocupado.get(); }

    public void iniciarSesion(String correo, char[] password) {
        if (!ocupado.compareAndSet(false, true)) return;
        estado.setValue(Resultado.cargando());
        repo.iniciarSesion(correo, password, this::publicar);
    }

    public void registrar(String nombre, String correo, char[] password) {
        if (!ocupado.compareAndSet(false, true)) return;
        estado.setValue(Resultado.cargando());
        repo.registrar(nombre, correo, password, this::publicar);
    }

    /** Limpia el último resultado para no repetirlo tras una rotación. */
    public void consumir() { estado.setValue(null); }

    private void publicar(Resultado<Long> r) {
        ocupado.set(false);
        estado.setValue(r);
    }
}

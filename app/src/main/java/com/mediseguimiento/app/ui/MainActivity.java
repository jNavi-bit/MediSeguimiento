package com.mediseguimiento.app.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Sesion;
import com.mediseguimiento.app.core.recordatorios.Notificaciones;
import com.mediseguimiento.app.databinding.ActivityMainBinding;
import com.mediseguimiento.app.ui.agenda.AgendaFragment;
import com.mediseguimiento.app.ui.estudios.EstudiosFragment;
import com.mediseguimiento.app.ui.historial.HistorialFragment;
import com.mediseguimiento.app.ui.auth.AuthActivity;
import com.mediseguimiento.app.ui.auth.BienvenidaActivity;
import com.mediseguimiento.app.ui.common.PacienteViewModel;
import com.mediseguimiento.app.ui.perfil.PerfilFragment;
import com.mediseguimiento.app.ui.inicio.InicioFragment;

/**
 * Contenedor principal con navegación inferior. Los fragmentos se conservan
 * (mostrar/ocultar) para no perder su estado al cambiar de pestaña.
 */
public class MainActivity extends AppCompatActivity {

    private static final String KEY_TAB = "tab";
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // Enrutamiento: sin sesión se muestra la bienvenida (primera vez) o el acceso.
        Sesion sesion = new Sesion(this);
        if (!sesion.activa()) {
            Class<?> destino = sesion.onboardingVisto() ? AuthActivity.class : BienvenidaActivity.class;
            startActivity(new Intent(this, destino));
            finish();
            return;
        }

        // Si la cuenta ya no existe (p. ej. datos borrados) se cierra la sesión.
        new ViewModelProvider(this).get(PacienteViewModel.class).paciente().observe(this, p -> {
            if (p == null) {
                sesion.cerrar();
                startActivity(new Intent(this, AuthActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            }
        });

        Notificaciones.asegurarCanal(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // "Atrás" en cualquier pestaña distinta de Inicio regresa a Inicio antes de salir.
        OnBackPressedCallback volverAInicio = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() { binding.bottomNav.setSelectedItemId(R.id.nav_inicio); }
        };
        getOnBackPressedDispatcher().addCallback(this, volverAInicio);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            mostrar(item.getItemId());
            volverAInicio.setEnabled(item.getItemId() != R.id.nav_inicio);
            return true;
        });
        binding.bottomNav.setOnItemReselectedListener(item -> { /* sin acción */ });

        int tab = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_TAB, R.id.nav_inicio) : R.id.nav_inicio;
        binding.bottomNav.setSelectedItemId(tab);
        mostrar(tab);
        volverAInicio.setEnabled(tab != R.id.nav_inicio);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) outState.putInt(KEY_TAB, binding.bottomNav.getSelectedItemId());
    }

    /** Permite a otras pantallas (p. ej. accesos rápidos) cambiar de pestaña. */
    public void irA(@IdRes int itemId) {
        binding.bottomNav.setSelectedItemId(itemId);
    }

    private void mostrar(@IdRes int itemId) {
        FragmentManager fm = getSupportFragmentManager();
        String tag = "tab_" + itemId;
        FragmentTransaction tx = fm.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(R.anim.tab_entrar, R.anim.tab_salir);
        for (Fragment f : fm.getFragments()) {
            if (!tag.equals(f.getTag()) && !f.isHidden()) tx.hide(f);
        }
        Fragment destino = fm.findFragmentByTag(tag);
        if (destino == null) {
            tx.add(R.id.fragment_container, crear(itemId), tag);
        } else {
            tx.show(destino);
        }
        tx.commitNow();
    }

    private Fragment crear(@IdRes int itemId) {
        if (itemId == R.id.nav_agenda) {
            return new AgendaFragment();
        } else if (itemId == R.id.nav_estudios) {
            return new EstudiosFragment();
        } else if (itemId == R.id.nav_historial) {
            return new HistorialFragment();
        } else if (itemId == R.id.nav_perfil) {
            return new PerfilFragment();
        }
        return new InicioFragment();
    }
}

package com.mediseguimiento.app.core;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Sesión local del prototipo (RF-01). Solo guarda el identificador del paciente;
 * ningún dato médico ni la contraseña.
 */
public final class Sesion {

    private static final String PREFS = "sesion";
    private static final String K_PACIENTE = "paciente_id";
    private static final String K_ONBOARDING = "onboarding_visto";
    private static final long SIN_SESION = -1L;

    private final SharedPreferences prefs;

    public Sesion(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean activa() { return pacienteId() != SIN_SESION; }

    public long pacienteId() { return prefs.getLong(K_PACIENTE, SIN_SESION); }

    public void iniciar(long pacienteId) {
        prefs.edit().putLong(K_PACIENTE, pacienteId).putBoolean(K_ONBOARDING, true).apply();
    }

    public void cerrar() { prefs.edit().remove(K_PACIENTE).apply(); }

    public boolean onboardingVisto() { return prefs.getBoolean(K_ONBOARDING, false); }

    public void marcarOnboardingVisto() { prefs.edit().putBoolean(K_ONBOARDING, true).apply(); }
}

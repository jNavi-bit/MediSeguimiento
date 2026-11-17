package com.mediseguimiento.app.ui.common;

import androidx.annotation.ColorRes;

import com.mediseguimiento.app.R;

import java.util.Locale;

/** Asigna un color estable a cada especialidad para reconocerla de un vistazo. */
public final class Acentos {

    private static final int[][] PALETA = {
            {R.color.accent_agenda, R.color.accent_agenda_bg},
            {R.color.accent_estudios, R.color.accent_estudios_bg},
            {R.color.accent_prep, R.color.accent_prep_bg},
            {R.color.accent_historial, R.color.accent_historial_bg},
    };

    private Acentos() {}

    private static int indice(String clave) {
        String k = clave == null ? "" : clave.trim().toLowerCase(Locale.ROOT);
        return Math.floorMod(k.hashCode(), PALETA.length);
    }

    @ColorRes public static int color(String especialidad) { return PALETA[indice(especialidad)][0]; }

    @ColorRes public static int fondo(String especialidad) { return PALETA[indice(especialidad)][1]; }
}

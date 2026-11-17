package com.mediseguimiento.app.ui.common;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/** Utilidades para trabajar de borde a borde (edge-to-edge). */
public final class Ventana {

    private Ventana() {}

    /**
     * Suma el alto de la barra de estado al padding superior original de la vista,
     * para que el contenido no quede debajo de ella.
     */
    public static void aplicarPaddingSuperior(View vista) {
        final int top = vista.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(vista, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(v.getPaddingLeft(), top + barras.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(vista);
    }

    /**
     * Suma la altura de la barra de navegación (o del teclado, si está visible)
     * al padding inferior original de la vista.
     */
    public static void aplicarPaddingInferior(View vista) {
        final int bottom = vista.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(vista, (v, insets) -> {
            int nav = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom + Math.max(nav, ime));
            return insets;
        });
        ViewCompat.requestApplyInsets(vista);
    }

    /**
     * Registra la animación de cierre de una actividad. En Android 14+ se declara una vez;
     * en versiones anteriores se aplica justo después de {@code finish()}.
     */
    @SuppressWarnings("deprecation")
    public static void animacionCierre(Activity a, int entrar, int salir, boolean alTerminar) {
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            if (!alTerminar) a.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, entrar, salir);
        } else if (alTerminar) {
            a.overridePendingTransition(entrar, salir);
        }
    }

    /**
     * Íconos oscuros de barra de estado solo cuando el fondo es claro.
     * @param fondoOscuro true si la parte superior de la pantalla es oscura (p. ej. el encabezado).
     */
    public static void iconosBarraEstado(Activity activity, boolean fondoOscuro) {
        boolean modoNoche = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView())
                .setAppearanceLightStatusBars(!modoNoche && !fondoOscuro);
    }
}

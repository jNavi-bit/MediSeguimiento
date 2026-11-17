package com.mediseguimiento.app.ui.common;

import android.content.Context;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;

/** Estados reutilizables de botones. */
public final class Botones {

    private Botones() {}

    /** Muestra un indicador giratorio dentro del botón y bloquea nuevos toques. */
    public static void cargando(MaterialButton b, CharSequence texto) {
        Context ctx = b.getContext();
        CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(ctx, null, 0,
                com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall);
        spec.indicatorColors = new int[]{b.getCurrentTextColor()};
        b.setIcon(IndeterminateDrawable.createCircularDrawable(ctx, spec));
        b.setText(texto);
        b.setClickable(false);
    }

    public static void normal(MaterialButton b, CharSequence texto, int icono) {
        if (icono == 0) b.setIcon(null); else b.setIconResource(icono);
        b.setText(texto);
        b.setClickable(true);
    }

}

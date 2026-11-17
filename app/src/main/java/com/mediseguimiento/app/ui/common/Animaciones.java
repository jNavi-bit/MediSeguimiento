package com.mediseguimiento.app.ui.common;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;

/** Micro-interacciones reutilizables. */
public final class Animaciones {

    /** Curva "emphasized" de Material 3. */
    public static final PathInterpolator ENFATIZADA = new PathInterpolator(0.2f, 0f, 0f, 1f);

    private Animaciones() {}

    /** Flotación suave infinita (sube y baja). */
    public static Animator flotar(View v, float distanciaDp, long duracion, long retraso) {
        float d = distanciaDp * v.getResources().getDisplayMetrics().density;
        ObjectAnimator a = ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, -d / 2f, d / 2f);
        a.setDuration(duracion);
        a.setStartDelay(retraso);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        a.start();
        return a;
    }

    /** Pulso de escala infinito, como una respiración. */
    public static Animator respirar(View v, float escala, long duracion) {
        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, escala),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, escala));
        a.setDuration(duracion);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        a.start();
        return a;
    }

    /** Rotación lenta continua. */
    public static Animator girar(View v, long duracionVuelta) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, View.ROTATION, 0f, 360f);
        a.setDuration(duracionVuelta);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setInterpolator(null);
        a.start();
        return a;
    }

    /** Sacudida horizontal para indicar un error. */
    public static void sacudir(View v) {
        float d = 10 * v.getResources().getDisplayMetrics().density;
        ObjectAnimator a = ObjectAnimator.ofFloat(v, View.TRANSLATION_X, 0, d, -d, d * .7f, -d * .7f, d * .3f, 0);
        a.setDuration(420);
        a.start();
    }

    /** Entrada escalonada: aparece desde abajo con desvanecimiento. */
    public static void entrar(View v, long retraso) {
        v.setAlpha(0f);
        v.setTranslationY(24 * v.getResources().getDisplayMetrics().density);
        v.animate().alpha(1f).translationY(0f).setStartDelay(retraso)
                .setDuration(520).setInterpolator(ENFATIZADA).start();
    }

    /** Aparición tipo "pop" con escala. */
    public static void aparecerEscalando(View v, long retraso) {
        v.setAlpha(0f);
        v.setScaleX(.85f);
        v.setScaleY(.85f);
        v.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(retraso)
                .setDuration(480).setInterpolator(new DecelerateInterpolator(2f)).start();
    }

    public static void vibrarError(View v) {
        v.performHapticFeedback(Build.VERSION.SDK_INT >= 30
                ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.LONG_PRESS);
    }

    public static void vibrarExito(View v) {
        v.performHapticFeedback(Build.VERSION.SDK_INT >= 30
                ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.VIRTUAL_KEY);
    }

    /**
     * MaterialButton calcula la posición de un ícono "textStart/textEnd" antes de conocer su
     * ancho final; al reasignar el texto una vez medido, el ícono queda junto al texto.
     */
    public static void alinearIcono(com.google.android.material.button.MaterialButton boton) {
        boton.post(() -> {
            CharSequence t = boton.getText();
            boton.setText(null);
            boton.setText(t);
        });
    }

    public static void vibrarToque(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }
}

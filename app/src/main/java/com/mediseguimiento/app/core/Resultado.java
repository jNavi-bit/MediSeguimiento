package com.mediseguimiento.app.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Estado observable de una operación: cargando, éxito o error (TC8 §10.2).
 */
public final class Resultado<T> {

    public enum Estado { CARGANDO, EXITO, ERROR }

    @NonNull public final Estado estado;
    @Nullable public final T datos;
    @Nullable public final String mensaje;
    /** Detalle adicional del error (opcional), p. ej. tiempo de bloqueo. */
    @Nullable public final Object detalle;

    private Resultado(@NonNull Estado estado, @Nullable T datos, @Nullable String mensaje,
                      @Nullable Object detalle) {
        this.estado = estado;
        this.datos = datos;
        this.mensaje = mensaje;
        this.detalle = detalle;
    }

    public static <T> Resultado<T> cargando() { return new Resultado<>(Estado.CARGANDO, null, null, null); }

    public static <T> Resultado<T> exito(@Nullable T datos) { return new Resultado<>(Estado.EXITO, datos, null, null); }

    public static <T> Resultado<T> error(@NonNull String mensaje) { return new Resultado<>(Estado.ERROR, null, mensaje, null); }

    public static <T> Resultado<T> error(@NonNull String mensaje, @Nullable Object detalle) {
        return new Resultado<>(Estado.ERROR, null, mensaje, detalle);
    }
}

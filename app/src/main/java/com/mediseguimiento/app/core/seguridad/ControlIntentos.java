package com.mediseguimiento.app.core.seguridad;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Limita intentos fallidos de inicio de sesión por cuenta: tras {@link #MAX_INTENTOS}
 * fallos la cuenta se bloquea temporalmente. El correo no se guarda en claro,
 * solo un resumen SHA-256. Los métodos son {@code synchronized} porque se invocan
 * desde hilos del grupo de E/S.
 */
public final class ControlIntentos {

    public static final int MAX_INTENTOS = 5;
    public static final long DURACION_BLOQUEO_MS = 30_000;

    private final SharedPreferences prefs;

    public ControlIntentos(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("intentos", Context.MODE_PRIVATE);
    }

    /** Milisegundos restantes de bloqueo, o 0 si la cuenta no está bloqueada. */
    public synchronized long bloqueoRestante(String correo, long ahora) {
        long hasta = prefs.getLong(clave(correo) + "_hasta", 0);
        return Math.max(0, hasta - ahora);
    }

    /** Registra un fallo y devuelve cuántos intentos quedan antes del bloqueo. */
    public synchronized int registrarFallo(String correo, long ahora) {
        String k = clave(correo);
        int fallos = prefs.getInt(k + "_fallos", 0) + 1;
        SharedPreferences.Editor ed = prefs.edit();
        if (fallos >= MAX_INTENTOS) {
            ed.putLong(k + "_hasta", ahora + DURACION_BLOQUEO_MS).putInt(k + "_fallos", 0);
            fallos = MAX_INTENTOS;
        } else {
            ed.putInt(k + "_fallos", fallos);
        }
        ed.apply();
        return MAX_INTENTOS - fallos;
    }

    public synchronized void reiniciar(String correo) {
        String k = clave(correo);
        prefs.edit().remove(k + "_fallos").remove(k + "_hasta").apply();
    }

    private static String clave(String correo) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(correo.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12; i++) sb.append(String.format("%02x", d[i]));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(correo.hashCode());
        }
    }
}

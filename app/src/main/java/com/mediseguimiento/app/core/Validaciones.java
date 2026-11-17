package com.mediseguimiento.app.core;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reglas de validación de formularios (RNF-04). Java puro para poder probarse
 * con pruebas unitarias sin emulador. Devuelve null cuando el valor es válido
 * o el mensaje de error en español (RNF-01).
 */
public final class Validaciones {

    private static final Pattern CORREO =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public static final int PASSWORD_MIN = 8;

    private Validaciones() {}

    public static String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    public static String nombre(String nombre) {
        String n = nombre == null ? "" : nombre.trim();
        if (n.isEmpty()) return "Escribe tu nombre";
        if (n.length() < 2) return "El nombre es demasiado corto";
        if (n.length() > 60) return "Usa un máximo de 60 caracteres";
        return null;
    }

    public static String correo(String correo) {
        String c = normalizarCorreo(correo);
        if (c.isEmpty()) return "Escribe tu correo electrónico";
        if (!CORREO.matcher(c).matches()) return "Revisa el formato, por ejemplo: nombre@correo.com";
        return null;
    }

    public static String passwordNueva(CharSequence pw) {
        if (pw == null || pw.length() == 0) return "Crea una contraseña";
        if (pw.length() < PASSWORD_MIN) return "Usa al menos " + PASSWORD_MIN + " caracteres";
        boolean letra = false, digito = false;
        for (int i = 0; i < pw.length(); i++) {
            char ch = pw.charAt(i);
            if (Character.isLetter(ch)) letra = true;
            else if (Character.isDigit(ch)) digito = true;
        }
        if (!letra || !digito) return "Combina letras y números";
        return null;
    }

    public static String passwordIngresada(CharSequence pw) {
        return (pw == null || pw.length() == 0) ? "Escribe tu contraseña" : null;
    }

    public static String confirmacion(CharSequence pw, CharSequence confirmacion) {
        if (confirmacion == null || confirmacion.length() == 0) return "Confirma tu contraseña";
        return pw != null && pw.toString().contentEquals(confirmacion) ? null : "Las contraseñas no coinciden";
    }

    /** Especialidad o motivo general de la cita (obligatorio). */
    public static String especialidad(String e) {
        String v = e == null ? "" : e.trim();
        if (v.isEmpty()) return "Indica la especialidad o el tipo de consulta";
        if (v.length() > 60) return "Usa un máximo de 60 caracteres";
        return null;
    }

    /** Texto libre opcional con longitud máxima. */
    public static String opcional(String v, int max) {
        return v != null && v.trim().length() > max ? "Usa un máximo de " + max + " caracteres" : null;
    }

    /**
     * Fecha de una cita. Las citas nuevas deben ser futuras (con un minuto de tolerancia);
     * al editar se permiten fechas pasadas para corregir registros. Máximo dos años adelante.
     */
    public static String fechaCita(Long fechaHora, long ahora, boolean nueva) {
        if (fechaHora == null) return "Elige la fecha y la hora";
        if (nueva && fechaHora < ahora - 60_000) return "Elige una fecha y hora futuras";
        if (fechaHora > ahora + 2L * 366 * 24 * 60 * 60 * 1000) return "La fecha está demasiado lejos";
        return null;
    }

    /**
     * Fuerza estimada de 0 (vacía) a 4 (excelente) según longitud y variedad de caracteres.
     */
    public static int fuerzaPassword(CharSequence pw) {
        if (pw == null || pw.length() == 0) return 0;
        boolean min = false, may = false, dig = false, sim = false;
        for (int i = 0; i < pw.length(); i++) {
            char ch = pw.charAt(i);
            if (Character.isLowerCase(ch)) min = true;
            else if (Character.isUpperCase(ch)) may = true;
            else if (Character.isDigit(ch)) dig = true;
            else sim = true;
        }
        int variedad = (min ? 1 : 0) + (may ? 1 : 0) + (dig ? 1 : 0) + (sim ? 1 : 0);
        int puntos = 0;
        if (pw.length() >= PASSWORD_MIN) puntos++;
        if (pw.length() >= 12) puntos++;
        if (variedad >= 2) puntos++;
        if (variedad >= 3) puntos++;
        if (pw.length() < PASSWORD_MIN) puntos = Math.min(puntos, 1);
        return Math.max(1, Math.min(4, puntos));
    }
}

package com.mediseguimiento.app.core;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Formato y cálculo de fechas en español de México con java.time (disponible desde API 26).
 * Java puro: se prueba con pruebas unitarias.
 */
public final class Fechas {

    public static final Locale ES = new Locale("es", "MX");

    private static final DateTimeFormatter LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES);
    private static final DateTimeFormatter LARGA_ANIO = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter CORTA = DateTimeFormatter.ofPattern("EEE d MMM", ES);
    private static final DateTimeFormatter MES_ANIO = DateTimeFormatter.ofPattern("MMMM yyyy", ES);
    private static final DateTimeFormatter H24 = DateTimeFormatter.ofPattern("HH:mm", ES);
    private static final DateTimeFormatter H12 = DateTimeFormatter.ofPattern("h:mm", ES);
    private static final DateTimeFormatter DIA_SEMANA = DateTimeFormatter.ofPattern("EEE", ES);

    private Fechas() {}

    public static ZoneId zona() { return ZoneId.systemDefault(); }

    public static LocalDateTime aLocal(long ms) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), zona());
    }

    public static LocalDate aFecha(long ms) { return aLocal(ms).toLocalDate(); }

    public static long aMillis(LocalDate fecha, LocalTime hora) {
        return fecha.atTime(hora).atZone(zona()).toInstant().toEpochMilli();
    }

    public static long inicioDelDia(long ms) {
        return aFecha(ms).atStartOfDay(zona()).toInstant().toEpochMilli();
    }

    public static String capitalizar(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(ES) + s.substring(1);
    }

    /** "Lunes 23 de noviembre" (agrega el año si no es el actual). */
    public static String fechaLarga(long ms, long ahora) {
        LocalDate d = aFecha(ms);
        DateTimeFormatter f = d.getYear() == aFecha(ahora).getYear() ? LARGA : LARGA_ANIO;
        return capitalizar(d.format(f));
    }

    /** "Lun 23 nov" */
    public static String fechaCorta(long ms) {
        return capitalizar(aFecha(ms).format(CORTA).replace(".", ""));
    }

    public static String mesAnio(LocalDate d) { return capitalizar(d.format(MES_ANIO)); }

    public static String diaSemanaCorto(LocalDate d) {
        return d.format(DIA_SEMANA).replace(".", "").toUpperCase(ES);
    }

    /** Hora sin periodo: "10:30" (24 h) o "10:30" (12 h, el periodo va aparte). */
    public static String hora(long ms, boolean h24) {
        return aLocal(ms).format(h24 ? H24 : H12);
    }

    /** "a. m." / "p. m." para el formato de 12 horas. */
    public static String periodo(long ms) {
        return aLocal(ms).getHour() < 12 ? "a. m." : "p. m.";
    }

    public static String horaCompleta(long ms, boolean h24) {
        return h24 ? hora(ms, true) : hora(ms, false) + " " + periodo(ms);
    }

    /** Diferencia en días de calendario (no en bloques de 24 h). */
    public static long diasEntre(long desde, long hasta) {
        return ChronoUnit.DAYS.between(aFecha(desde), aFecha(hasta));
    }

    /** "Hoy", "Mañana", "Ayer" o null si es otro día. */
    public static String diaRelativo(long ms, long ahora) {
        long d = diasEntre(ahora, ms);
        if (d == 0) return "Hoy";
        if (d == 1) return "Mañana";
        if (d == -1) return "Ayer";
        return null;
    }

    /** Encabezado de sección para la agenda. */
    public static String encabezadoDia(long ms, long ahora) {
        String rel = diaRelativo(ms, ahora);
        String larga = fechaLarga(ms, ahora);
        return rel == null ? larga : rel + " · " + larga;
    }

    /** Texto breve de cuánto falta: "En 25 min", "En 3 h", "Mañana", "En 4 días", "Hace 2 días". */
    public static String cuentaRegresiva(long ms, long ahora) {
        long min = Duration.ofMillis(ms - ahora).toMinutes();
        long dias = diasEntre(ahora, ms);
        if (min < 0) {
            if (dias == 0) return "Hoy, ya pasó";
            if (dias == -1) return "Ayer";
            return "Hace " + (-dias) + " días";
        }
        if (dias == 0) {
            if (min < 1) return "Ahora";
            if (min < 60) return "En " + min + " min";
            return "En " + (min / 60) + " h";
        }
        if (dias == 1) return "Mañana";
        if (dias < 7) return "En " + dias + " días";
        if (dias < 14) return "En 1 semana";
        if (dias < 60) return "En " + (dias / 7) + " semanas";
        long meses = ChronoUnit.MONTHS.between(aFecha(ahora), aFecha(ms));
        return meses <= 1 ? "En 1 mes" : "En " + meses + " meses";
    }
}

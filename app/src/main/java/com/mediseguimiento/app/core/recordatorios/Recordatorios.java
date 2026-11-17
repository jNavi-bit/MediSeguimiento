package com.mediseguimiento.app.core.recordatorios;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Programa recordatorios locales con WorkManager (RF-06). Android puede diferir la
 * ejecución para ahorrar batería, así que el aviso es aproximado, no de tiempo real
 * estricto (TC7 §7.4). Cada cita tiene un trabajo único que se reemplaza al editarla.
 */
public final class Recordatorios {

    public static final String CLAVE_CITA = "cita_id";
    public static final String CLAVE_MINUTOS = "minutos";
    public static final int[] OPCIONES_MIN = {15, 60, 24 * 60};

    private Recordatorios() {}

    private static String nombre(long citaId) { return "recordatorio_cita_" + citaId; }

    /** Momento en que sonaría el aviso. */
    public static long disparo(long fechaHora, int minutosAntes) {
        return fechaHora - minutosAntes * 60_000L;
    }

    /** @return true si quedó programado; false si el momento del aviso ya pasó. */
    public static boolean programar(Context ctx, long citaId, long fechaHora, int minutosAntes) {
        long retraso = disparo(fechaHora, minutosAntes) - System.currentTimeMillis();
        if (retraso <= 0) {
            cancelar(ctx, citaId);
            return false;
        }
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(RecordatorioWorker.class)
                .setInitialDelay(retraso, TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder()
                        .putLong(CLAVE_CITA, citaId)
                        .putInt(CLAVE_MINUTOS, minutosAntes).build())
                .addTag("recordatorio")
                .build();
        WorkManager.getInstance(ctx).enqueueUniqueWork(nombre(citaId), ExistingWorkPolicy.REPLACE, req);
        return true;
    }

    public static void cancelar(Context ctx, long citaId) {
        WorkManager.getInstance(ctx).cancelUniqueWork(nombre(citaId));
    }

    /** Texto legible de la anticipación: "15 min antes", "1 h antes", "1 día antes". */
    public static String etiqueta(int minutos) {
        if (minutos >= 24 * 60) return (minutos / (24 * 60)) == 1 ? "1 día antes" : (minutos / (24 * 60)) + " días antes";
        if (minutos >= 60) return (minutos / 60) + " h antes";
        return minutos + " min antes";
    }
}

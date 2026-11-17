package com.mediseguimiento.app.core.recordatorios;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.data.db.AppDatabase;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.ui.MainActivity;
import com.mediseguimiento.app.ui.cita.CitaDetalleActivity;

/**
 * Trabajo en segundo plano que muestra el aviso de una cita. WorkManager lo ejecuta en
 * su propio grupo de hilos; aquí se puede leer la base de datos de forma síncrona.
 */
public class RecordatorioWorker extends Worker {

    public RecordatorioWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        long id = getInputData().getLong(Recordatorios.CLAVE_CITA, -1);
        Cita c = AppDatabase.get(ctx).citaDao().obtenerPorId(id);
        // La cita pudo cancelarse o eliminarse después de programar el aviso.
        if (c == null || !Cita.ESTADO_PROGRAMADA.equals(c.estado)) return Result.success();

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success();
        }
        Notificaciones.asegurarCanal(ctx);

        long ahora = System.currentTimeMillis();
        String cuando = Fechas.cuentaRegresiva(c.fechaHora, ahora);
        String titulo = "Tu cita de " + c.especialidad + ": " + cuando.toLowerCase(Fechas.ES);
        String texto = Fechas.fechaCorta(c.fechaHora) + " · "
                + Fechas.horaCompleta(c.fechaHora, DateFormat.is24HourFormat(ctx))
                + (TextUtils.isEmpty(c.lugar) ? "" : " · " + c.lugar)
                + ". Revisa tu preparación antes de salir.";
        texto = texto.replace("..", "."); // "a. m." ya termina en punto

        Intent detalle = new Intent(ctx, CitaDetalleActivity.class).putExtra(CitaDetalleActivity.EXTRA_ID, c.id);
        PendingIntent pi = TaskStackBuilder.create(ctx)
                .addNextIntent(new Intent(ctx, MainActivity.class))
                .addNextIntent(detalle)
                .getPendingIntent((int) c.id, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Versión pública para la pantalla de bloqueo: sin datos médicos.
        android.app.Notification publica = new NotificationCompat.Builder(ctx, Notificaciones.CANAL_CITAS)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle("Tienes una cita próxima")
                .setContentText("Abre MediSeguimiento para ver los detalles")
                .build();

        NotificationCompat.Builder n = new NotificationCompat.Builder(ctx, Notificaciones.CANAL_CITAS)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setColor(ContextCompat.getColor(ctx, R.color.md_primary))
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publica)
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            NotificationManagerCompat.from(ctx).notify((int) c.id, n.build());
        } catch (SecurityException ignorada) {
            // Permiso revocado entre la comprobación y el aviso.
        }
        return Result.success();
    }
}

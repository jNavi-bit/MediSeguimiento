package com.mediseguimiento.app.core.recordatorios;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

/** Canal de notificaciones de recordatorios (requerido desde Android 8.0). */
public final class Notificaciones {

    public static final String CANAL_CITAS = "recordatorios_citas";

    private Notificaciones() {}

    public static void asegurarCanal(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CANAL_CITAS) != null) return;
        NotificationChannel c = new NotificationChannel(CANAL_CITAS, "Recordatorios de citas",
                NotificationManager.IMPORTANCE_HIGH);
        c.setDescription("Avisos antes de tus citas de seguimiento");
        // RNF-05: en la pantalla de bloqueo no se muestra el contenido completo.
        c.setLockscreenVisibility(android.app.Notification.VISIBILITY_PRIVATE);
        nm.createNotificationChannel(c);
    }
}

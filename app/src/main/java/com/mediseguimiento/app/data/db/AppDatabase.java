package com.mediseguimiento.app.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.mediseguimiento.app.data.db.dao.CitaDao;
import com.mediseguimiento.app.data.db.dao.ConsentimientoDao;
import com.mediseguimiento.app.data.db.dao.EstudioDao;
import com.mediseguimiento.app.data.db.dao.PacienteDao;
import com.mediseguimiento.app.data.db.dao.PreparacionDao;
import com.mediseguimiento.app.data.db.dao.RecordatorioDao;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.db.entity.Consentimiento;
import com.mediseguimiento.app.data.db.entity.Estudio;
import com.mediseguimiento.app.data.db.entity.Paciente;
import com.mediseguimiento.app.data.db.entity.PreparacionConsulta;
import com.mediseguimiento.app.data.db.entity.Recordatorio;

/**
 * Base de datos local Room/SQLite. Se usa un singleton con doble verificación
 * y {@code volatile} para que varios hilos no creen instancias distintas.
 */
@Database(entities = {Paciente.class, Cita.class, PreparacionConsulta.class,
        Estudio.class, Recordatorio.class, Consentimiento.class},
        version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String NOMBRE = "mediseguimiento.db";

    /**
     * Versión 2: agrega la duración de las citas. La migración conserva todos los datos
     * existentes; las citas anteriores quedan con 30 minutos.
     */
    public static final Migration MIGRACION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE citas ADD COLUMN duracion_min INTEGER NOT NULL DEFAULT 30");
        }
    };
    private static volatile AppDatabase instancia;

    public abstract PacienteDao pacienteDao();
    public abstract CitaDao citaDao();
    public abstract PreparacionDao preparacionDao();
    public abstract EstudioDao estudioDao();
    public abstract RecordatorioDao recordatorioDao();
    public abstract ConsentimientoDao consentimientoDao();

    public static AppDatabase get(@NonNull Context context) {
        AppDatabase local = instancia;
        if (local == null) {
            synchronized (AppDatabase.class) {
                local = instancia;
                if (local == null) {
                    local = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, NOMBRE)
                            // WAL permite lecturas concurrentes mientras otro hilo escribe.
                            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                            .addMigrations(MIGRACION_1_2)
                            .addCallback(new Callback() {
                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    db.execSQL("PRAGMA foreign_keys = ON");
                                }
                            })
                            .build();
                    instancia = local;
                }
            }
        }
        return local;
    }
}

package com.mediseguimiento.app.data.db.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Recordatorio local de una cita (RF-06). El identificador del trabajo de
 * WorkManager se guarda para poder cancelarlo al editar o eliminar la cita.
 */
@Entity(tableName = "recordatorios",
        foreignKeys = @ForeignKey(entity = Cita.class,
                parentColumns = "id", childColumns = "cita_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("cita_id")})
public class Recordatorio {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "cita_id")
    public long citaId;

    @ColumnInfo(name = "minutos_antes")
    public int minutosAntes;

    /** Momento programado de la notificación en milisegundos epoch. */
    @ColumnInfo(name = "disparo_en")
    public long disparoEn;

    @Nullable
    @ColumnInfo(name = "work_id")
    public String workId;

    public boolean activo = true;
}

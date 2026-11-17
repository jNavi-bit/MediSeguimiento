package com.mediseguimiento.app.data.db.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Información que el paciente prepara antes de una cita (RF-03).
 * Existe como máximo una preparación por cita (índice único).
 */
@Entity(tableName = "preparaciones",
        foreignKeys = @ForeignKey(entity = Cita.class,
                parentColumns = "id", childColumns = "cita_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "cita_id", unique = true)})
public class PreparacionConsulta {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "cita_id")
    public long citaId;

    @Nullable public String motivo;
    @Nullable public String sintomas;

    @Nullable
    @ColumnInfo(name = "cambios_recientes")
    public String cambiosRecientes;

    @Nullable public String tratamientos;
    @Nullable public String preguntas;

    @ColumnInfo(name = "actualizado_en")
    public long actualizadoEn;
}

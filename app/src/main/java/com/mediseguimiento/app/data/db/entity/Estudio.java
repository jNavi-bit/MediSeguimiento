package com.mediseguimiento.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Estudio realizado y su resultado, capturado manualmente por el paciente (RF-04).
 * Puede vincularse opcionalmente a una cita para incluirse en su resumen.
 */
@Entity(tableName = "estudios",
        foreignKeys = {
                @ForeignKey(entity = Paciente.class, parentColumns = "id",
                        childColumns = "paciente_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Cita.class, parentColumns = "id",
                        childColumns = "cita_id", onDelete = ForeignKey.SET_NULL)
        },
        indices = {@Index("paciente_id"), @Index("cita_id"), @Index("fecha")})
public class Estudio {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "paciente_id")
    public long pacienteId;

    @Nullable
    @ColumnInfo(name = "cita_id")
    public Long citaId;

    @NonNull
    public String nombre = "";

    /** Fecha de realización en milisegundos epoch. */
    public long fecha;

    @Nullable public String resultado;
    @Nullable public String laboratorio;
    @Nullable public String notas;

    @ColumnInfo(name = "creado_en")
    public long creadoEn;
}

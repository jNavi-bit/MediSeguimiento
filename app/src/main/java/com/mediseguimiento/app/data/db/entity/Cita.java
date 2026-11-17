package com.mediseguimiento.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Cita de seguimiento en la agenda personal (RF-02). No equivale a una
 * reservación oficial en una institución de salud.
 */
@Entity(tableName = "citas",
        foreignKeys = @ForeignKey(entity = Paciente.class,
                parentColumns = "id", childColumns = "paciente_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("paciente_id"), @Index("fecha_hora")})
public class Cita {

    public static final String ESTADO_PROGRAMADA = "PROGRAMADA";
    public static final String ESTADO_COMPLETADA = "COMPLETADA";
    public static final String ESTADO_CANCELADA = "CANCELADA";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "paciente_id")
    public long pacienteId;

    /** Fecha y hora de la cita en milisegundos epoch. */
    @ColumnInfo(name = "fecha_hora")
    public long fechaHora;

    @NonNull
    public String especialidad = "";

    @Nullable
    public String medico;

    @Nullable
    public String lugar;

    @Nullable
    public String notas;

    @NonNull
    public String estado = ESTADO_PROGRAMADA;

    @ColumnInfo(name = "creado_en")
    public long creadoEn;

    @ColumnInfo(name = "actualizado_en")
    public long actualizadoEn;

    /**
     * Control de concurrencia optimista: cada actualización incrementa la versión
     * y solo se aplica si la versión leída coincide con la almacenada (RNF-04).
     */
    public int version;

    /** Sin cambios en el recordatorio (valor por defecto). */
    public static final int RECORDATORIO_SIN_CAMBIO = -2;
    /** Sin recordatorio. */
    public static final int RECORDATORIO_NINGUNO = -1;

    /**
     * Minutos de anticipación elegidos en el formulario. No es una columna: el
     * recordatorio se guarda en su propia tabla dentro de la misma transacción.
     */
    @Ignore
    public int recordatorioMin = RECORDATORIO_SIN_CAMBIO;
}

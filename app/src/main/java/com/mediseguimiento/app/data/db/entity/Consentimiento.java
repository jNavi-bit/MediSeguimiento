package com.mediseguimiento.app.data.db.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Registro del consentimiento explícito del paciente antes de compartir
 * el resumen de una cita (RF-07).
 */
@Entity(tableName = "consentimientos",
        foreignKeys = {
                @ForeignKey(entity = Paciente.class, parentColumns = "id",
                        childColumns = "paciente_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Cita.class, parentColumns = "id",
                        childColumns = "cita_id", onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("paciente_id"), @Index("cita_id")})
public class Consentimiento {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "paciente_id")
    public long pacienteId;

    @ColumnInfo(name = "cita_id")
    public long citaId;

    /** A quién se muestra o comparte (p. ej. "Dra. López, Medicina interna"). */
    @Nullable
    public String destinatario;

    @ColumnInfo(name = "otorgado_en")
    public long otorgadoEn;

    public boolean revocado;
}

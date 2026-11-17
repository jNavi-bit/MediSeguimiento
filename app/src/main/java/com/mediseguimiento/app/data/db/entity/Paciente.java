package com.mediseguimiento.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Usuario del prototipo (paciente). La contraseña nunca se guarda en claro:
 * solo su hash PBKDF2 y la sal correspondiente (RNF-05).
 */
@Entity(tableName = "pacientes",
        indices = {@Index(value = "correo", unique = true)})
public class Paciente {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String nombre = "";

    /** Correo normalizado en minúsculas; único para evitar cuentas duplicadas (RNF-04). */
    @NonNull
    public String correo = "";

    @NonNull
    @ColumnInfo(name = "password_hash")
    public String passwordHash = "";

    @NonNull
    public String sal = "";

    /** Fecha de nacimiento en milisegundos epoch (opcional). */
    @Nullable
    @ColumnInfo(name = "fecha_nacimiento")
    public Long fechaNacimiento;

    /** Padecimiento en seguimiento, descrito por el propio paciente (opcional). */
    @Nullable
    public String padecimiento;

    @ColumnInfo(name = "creado_en")
    public long creadoEn;
}

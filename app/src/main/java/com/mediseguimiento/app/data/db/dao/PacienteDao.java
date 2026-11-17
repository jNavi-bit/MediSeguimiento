package com.mediseguimiento.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.mediseguimiento.app.data.db.entity.Paciente;

@Dao
public interface PacienteDao {

    /** ABORT: si el correo ya existe, Room lanza una excepción en lugar de duplicar. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertar(Paciente paciente);

    @Update
    int actualizar(Paciente paciente);

    @Query("SELECT * FROM pacientes WHERE correo = :correo LIMIT 1")
    Paciente buscarPorCorreo(String correo);

    @Query("SELECT * FROM pacientes WHERE id = :id")
    Paciente obtener(long id);

    @Query("SELECT * FROM pacientes WHERE id = :id")
    LiveData<Paciente> observar(long id);

    @Query("DELETE FROM pacientes WHERE id = :id")
    int eliminar(long id);
}

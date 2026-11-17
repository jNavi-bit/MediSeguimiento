package com.mediseguimiento.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.mediseguimiento.app.data.db.entity.Estudio;

import java.util.List;

@Dao
public interface EstudioDao {

    @Insert
    long insertar(Estudio estudio);

    @Update
    int actualizar(Estudio estudio);

    @Query("DELETE FROM estudios WHERE id = :id AND paciente_id = :pacienteId")
    int eliminar(long id, long pacienteId);

    @Query("SELECT * FROM estudios WHERE id = :id AND paciente_id = :pacienteId")
    Estudio obtener(long id, long pacienteId);

    @Query("SELECT * FROM estudios WHERE paciente_id = :pacienteId ORDER BY fecha DESC")
    LiveData<List<Estudio>> observarTodos(long pacienteId);

    @Query("SELECT * FROM estudios WHERE paciente_id = :pacienteId ORDER BY fecha DESC")
    List<Estudio> listarCronologico(long pacienteId);

    @Query("SELECT * FROM estudios WHERE paciente_id = :pacienteId AND fecha >= :desde ORDER BY fecha DESC LIMIT :limite")
    List<Estudio> listarRecientes(long pacienteId, long desde, int limite);

    @Query("SELECT COUNT(*) FROM estudios WHERE paciente_id = :pacienteId")
    LiveData<Integer> contar(long pacienteId);

    @Query("SELECT * FROM estudios WHERE cita_id = :citaId ORDER BY fecha DESC")
    List<Estudio> listarPorCita(long citaId);
}

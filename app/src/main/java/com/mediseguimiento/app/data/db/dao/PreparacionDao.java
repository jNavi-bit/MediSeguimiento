package com.mediseguimiento.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mediseguimiento.app.data.db.entity.PreparacionConsulta;

import java.util.List;

@Dao
public interface PreparacionDao {

    /** Una preparación por cita: si ya existe se reemplaza (índice único cita_id). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long guardar(PreparacionConsulta preparacion);

    @Query("SELECT * FROM preparaciones WHERE cita_id = :citaId")
    PreparacionConsulta obtenerPorCita(long citaId);

    @Query("SELECT * FROM preparaciones WHERE cita_id = :citaId")
    LiveData<PreparacionConsulta> observarPorCita(long citaId);

    @Query("SELECT p.* FROM preparaciones p JOIN citas c ON c.id = p.cita_id WHERE c.paciente_id = :pacienteId")
    LiveData<List<PreparacionConsulta>> observarPorPaciente(long pacienteId);

    @Query("SELECT COUNT(*) FROM preparaciones p JOIN citas c ON c.id = p.cita_id WHERE c.paciente_id = :pacienteId")
    LiveData<Integer> contar(long pacienteId);

    @Query("DELETE FROM preparaciones WHERE cita_id = :citaId")
    int eliminarPorCita(long citaId);
}

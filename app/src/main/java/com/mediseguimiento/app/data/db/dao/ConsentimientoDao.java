package com.mediseguimiento.app.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.mediseguimiento.app.data.db.entity.Consentimiento;

import java.util.List;

@Dao
public interface ConsentimientoDao {

    @Insert
    long insertar(Consentimiento consentimiento);

    @Query("SELECT * FROM consentimientos WHERE cita_id = :citaId AND paciente_id = :pacienteId " +
            "ORDER BY otorgado_en DESC")
    List<Consentimiento> listarPorCita(long citaId, long pacienteId);

    @Query("UPDATE consentimientos SET revocado = 1 WHERE id = :id AND paciente_id = :pacienteId")
    int revocar(long id, long pacienteId);
}

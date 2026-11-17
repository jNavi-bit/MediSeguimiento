package com.mediseguimiento.app.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.mediseguimiento.app.data.db.entity.Recordatorio;

import java.util.List;

@Dao
public interface RecordatorioDao {

    @Insert
    long insertar(Recordatorio recordatorio);

    @Query("SELECT * FROM recordatorios WHERE cita_id = :citaId AND activo = 1")
    List<Recordatorio> activosPorCita(long citaId);

    @Query("UPDATE recordatorios SET activo = 0 WHERE cita_id = :citaId")
    int desactivarPorCita(long citaId);

    @Query("UPDATE recordatorios SET work_id = :workId WHERE id = :id")
    int asignarWorkId(long id, String workId);
}

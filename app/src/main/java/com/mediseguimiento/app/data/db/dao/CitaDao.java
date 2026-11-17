package com.mediseguimiento.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.mediseguimiento.app.data.db.entity.Cita;

import java.util.List;

@Dao
public interface CitaDao {

    @Insert
    long insertar(Cita cita);

    /**
     * Actualización con control de concurrencia optimista: solo se aplica si
     * nadie modificó la cita desde que se leyó. Devuelve 0 si hubo conflicto.
     */
    @Query("UPDATE citas SET fecha_hora = :fechaHora, especialidad = :especialidad, " +
            "medico = :medico, lugar = :lugar, notas = :notas, estado = :estado, " +
            "actualizado_en = :ahora, version = version + 1 " +
            "WHERE id = :id AND paciente_id = :pacienteId AND version = :versionLeida")
    int actualizarSiVersion(long id, long pacienteId, int versionLeida, long fechaHora,
                            String especialidad, String medico, String lugar,
                            String notas, String estado, long ahora);

    @Query("UPDATE citas SET estado = :estado, actualizado_en = :ahora, version = version + 1 " +
            "WHERE id = :id AND paciente_id = :pacienteId")
    int cambiarEstado(long id, long pacienteId, String estado, long ahora);

    @Query("DELETE FROM citas WHERE id = :id AND paciente_id = :pacienteId")
    int eliminar(long id, long pacienteId);

    @Query("SELECT * FROM citas WHERE id = :id AND paciente_id = :pacienteId")
    Cita obtener(long id, long pacienteId);

    @Query("SELECT * FROM citas WHERE id = :id AND paciente_id = :pacienteId")
    LiveData<Cita> observar(long id, long pacienteId);

    @Query("SELECT * FROM citas WHERE paciente_id = :pacienteId ORDER BY fecha_hora ASC")
    LiveData<List<Cita>> observarTodas(long pacienteId);

    @Query("SELECT * FROM citas WHERE paciente_id = :pacienteId AND estado = 'PROGRAMADA' " +
            "AND fecha_hora >= :desde ORDER BY fecha_hora ASC LIMIT 1")
    LiveData<Cita> observarProxima(long pacienteId, long desde);

    @Query("SELECT * FROM citas WHERE paciente_id = :pacienteId ORDER BY fecha_hora DESC")
    List<Cita> listarCronologico(long pacienteId);

    /** Búsqueda sin filtro de paciente: solo para el trabajador de recordatorios. */
    @Query("SELECT * FROM citas WHERE id = :id")
    Cita obtenerPorId(long id);

    @Query("SELECT COUNT(*) FROM citas WHERE paciente_id = :pacienteId AND estado = 'PROGRAMADA' AND fecha_hora >= :desde")
    LiveData<Integer> contarProximas(long pacienteId, long desde);

    /** Consultas anteriores a una fecha (para el resumen), excluyendo canceladas. */
    @Query("SELECT * FROM citas WHERE paciente_id = :pacienteId AND fecha_hora < :antesDe " +
            "AND estado != 'CANCELADA' ORDER BY fecha_hora DESC LIMIT :limite")
    List<Cita> listarAnteriores(long pacienteId, long antesDe, int limite);

    /** Evita duplicados exactos: misma especialidad a la misma hora (RNF-04). */
    @Query("SELECT COUNT(*) FROM citas WHERE paciente_id = :pacienteId AND fecha_hora = :fechaHora " +
            "AND especialidad = :especialidad AND estado != 'CANCELADA' AND id != :excluirId")
    int contarDuplicadas(long pacienteId, long fechaHora, String especialidad, long excluirId);
}

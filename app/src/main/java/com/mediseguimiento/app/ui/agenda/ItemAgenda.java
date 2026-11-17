package com.mediseguimiento.app.ui.agenda;

import androidx.annotation.Nullable;

import com.mediseguimiento.app.data.db.entity.Cita;

import java.util.Objects;

/** Elemento de la lista de la agenda: encabezado de día o cita. */
public final class ItemAgenda {

    public static final int ENCABEZADO = 0;
    public static final int CITA = 1;

    public final int tipo;
    public final long id;
    @Nullable public final String titulo;
    @Nullable public final Cita cita;

    private ItemAgenda(int tipo, long id, @Nullable String titulo, @Nullable Cita cita) {
        this.tipo = tipo;
        this.id = id;
        this.titulo = titulo;
        this.cita = cita;
    }

    public static ItemAgenda encabezado(long epochDay, String titulo) {
        return new ItemAgenda(ENCABEZADO, -epochDay - 1, titulo, null);
    }

    public static ItemAgenda de(Cita c) { return new ItemAgenda(CITA, c.id, null, c); }

    /** Igualdad de contenido para DiffUtil (la versión cambia con cada edición). */
    public boolean mismoContenido(ItemAgenda o) {
        if (tipo != o.tipo) return false;
        if (tipo == ENCABEZADO) return Objects.equals(titulo, o.titulo);
        return cita != null && o.cita != null && cita.version == o.cita.version
                && cita.estado.equals(o.cita.estado) && cita.fechaHora == o.cita.fechaHora;
    }
}

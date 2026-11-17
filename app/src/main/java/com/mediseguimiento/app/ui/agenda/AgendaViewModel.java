package com.mediseguimiento.app.ui.agenda;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.mediseguimiento.app.core.AppExecutors;
import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.repo.CitaRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Estado de la agenda. El filtrado y agrupado por día se calcula en el grupo de cómputo;
 * un contador de generación descarta resultados viejos si el usuario cambia el filtro
 * antes de que termine el cálculo anterior (evita mostrar datos desactualizados).
 */
public class AgendaViewModel extends AndroidViewModel {

    public enum Filtro { PROXIMAS, PASADAS, CANCELADAS }

    /** Resultado listo para pintar. */
    public static final class Estado {
        public final List<ItemAgenda> items;
        public final Set<Long> diasConCitas;
        public final Filtro filtro;
        @Nullable public final Long dia;

        Estado(List<ItemAgenda> items, Set<Long> diasConCitas, Filtro filtro, @Nullable Long dia) {
            this.items = items;
            this.diasConCitas = diasConCitas;
            this.filtro = filtro;
            this.dia = dia;
        }
    }

    private final LiveData<List<Cita>> citas;
    private final MutableLiveData<Filtro> filtro = new MutableLiveData<>(Filtro.PROXIMAS);
    private final MutableLiveData<Long> dia = new MutableLiveData<>(null);
    private final MediatorLiveData<Estado> estado = new MediatorLiveData<>();
    private final AtomicInteger generacion = new AtomicInteger();

    public AgendaViewModel(@NonNull Application app) {
        super(app);
        citas = new CitaRepository(app).observarTodas();
        estado.addSource(citas, c -> recalcular());
        estado.addSource(filtro, f -> recalcular());
        estado.addSource(dia, d -> recalcular());
    }

    public LiveData<Estado> estado() { return estado; }

    public Filtro filtro() { return filtro.getValue(); }

    public void setFiltro(Filtro f) {
        if (f != filtro.getValue()) {
            if (f != Filtro.PROXIMAS) dia.setValue(null);
            filtro.setValue(f);
        }
    }

    /** Selecciona un día (epochDay); tocar el mismo día de nuevo lo deselecciona. */
    public void alternarDia(long epochDay) {
        Long actual = dia.getValue();
        dia.setValue(actual != null && actual == epochDay ? null : epochDay);
        if (filtro.getValue() != Filtro.PROXIMAS) filtro.setValue(Filtro.PROXIMAS);
    }

    /** Fuerza un recálculo (p. ej. al volver a la pantalla, por si cambió el día). */
    public void refrescar() { recalcular(); }

    private void recalcular() {
        final List<Cita> fuente = citas.getValue();
        if (fuente == null) return;
        final List<Cita> copia = new ArrayList<>(fuente);
        final Filtro f = filtro.getValue();
        final Long d = dia.getValue();
        final int gen = generacion.incrementAndGet();
        AppExecutors.get().computo().execute(() -> {
            Estado e = construir(copia, f, d, System.currentTimeMillis());
            // Solo se publica si nadie pidió un cálculo más reciente.
            if (gen == generacion.get()) estado.postValue(e);
        });
    }

    static Estado construir(List<Cita> todas, Filtro f, @Nullable Long dia, long ahora) {
        long hoy = Fechas.inicioDelDia(ahora);
        Set<Long> dias = new HashSet<>();
        List<Cita> sel = new ArrayList<>();
        for (Cita c : todas) {
            boolean programada = Cita.ESTADO_PROGRAMADA.equals(c.estado);
            if (programada && c.fechaHora >= hoy) dias.add(Fechas.aFecha(c.fechaHora).toEpochDay());
            boolean incluir;
            switch (f) {
                case PROXIMAS: incluir = programada && c.fechaHora >= hoy; break;
                case PASADAS:
                    incluir = Cita.ESTADO_COMPLETADA.equals(c.estado) || (programada && c.fechaHora < hoy);
                    break;
                default: incluir = Cita.ESTADO_CANCELADA.equals(c.estado);
            }
            if (incluir && dia != null) incluir = Fechas.aFecha(c.fechaHora).toEpochDay() == dia;
            if (incluir) sel.add(c);
        }
        // Próximas en orden ascendente; pasadas y canceladas, lo más reciente primero.
        sel.sort((a, b) -> Long.compare(a.fechaHora, b.fechaHora));
        if (f != Filtro.PROXIMAS) Collections.reverse(sel);

        List<ItemAgenda> items = new ArrayList<>();
        long diaActual = Long.MIN_VALUE;
        for (Cita c : sel) {
            long ed = Fechas.aFecha(c.fechaHora).toEpochDay();
            if (ed != diaActual) {
                diaActual = ed;
                items.add(ItemAgenda.encabezado(ed, Fechas.encabezadoDia(c.fechaHora, ahora)));
            }
            items.add(ItemAgenda.de(c));
        }
        return new Estado(items, dias, f, dia);
    }
}

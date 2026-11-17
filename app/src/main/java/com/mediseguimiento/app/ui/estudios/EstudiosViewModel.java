package com.mediseguimiento.app.ui.estudios;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.mediseguimiento.app.core.AppExecutors;
import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.db.entity.Estudio;
import com.mediseguimiento.app.data.repo.CitaRepository;
import com.mediseguimiento.app.data.repo.EstudioRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Estudios agrupados por mes; se combinan con las citas para mostrar el vínculo. */
public class EstudiosViewModel extends AndroidViewModel {

    /** Elemento de lista: encabezado de mes o estudio. */
    public static final class Item {
        public static final int MES = 0, ESTUDIO = 1;
        public final int tipo;
        public final long id;
        public final String titulo;
        public final Estudio estudio;
        public final String cita;

        Item(int tipo, long id, String titulo, Estudio estudio, String cita) {
            this.tipo = tipo;
            this.id = id;
            this.titulo = titulo;
            this.estudio = estudio;
            this.cita = cita;
        }
    }

    private final LiveData<List<Estudio>> estudios;
    private final LiveData<List<Cita>> citas;
    private final MediatorLiveData<List<Item>> items = new MediatorLiveData<>();
    private final AtomicInteger generacion = new AtomicInteger();

    public EstudiosViewModel(@NonNull Application app) {
        super(app);
        estudios = new EstudioRepository(app).observarTodos();
        citas = new CitaRepository(app).observarTodas();
        items.addSource(estudios, x -> recalcular());
        items.addSource(citas, x -> recalcular());
    }

    public LiveData<List<Item>> items() { return items; }

    private void recalcular() {
        List<Estudio> es = estudios.getValue();
        if (es == null) return;
        List<Estudio> copiaE = new ArrayList<>(es);
        List<Cita> copiaC = citas.getValue() == null ? new ArrayList<>() : new ArrayList<>(citas.getValue());
        int gen = generacion.incrementAndGet();
        AppExecutors.get().computo().execute(() -> {
            Map<Long, String> nombres = new HashMap<>();
            for (Cita c : copiaC) nombres.put(c.id, c.especialidad + " · " + Fechas.fechaCorta(c.fechaHora));
            List<Item> out = new ArrayList<>();
            String mesActual = null;
            for (Estudio e : copiaE) { // ya vienen ordenados por fecha descendente
                String mes = Fechas.mesAnio(Fechas.aFecha(e.fecha));
                if (!mes.equals(mesActual)) {
                    mesActual = mes;
                    out.add(new Item(Item.MES, -(Fechas.aFecha(e.fecha).withDayOfMonth(1).toEpochDay()) - 1, mes, null, null));
                }
                out.add(new Item(Item.ESTUDIO, e.id, null, e, e.citaId == null ? null : nombres.get(e.citaId)));
            }
            if (gen == generacion.get()) items.postValue(out);
        });
    }
}

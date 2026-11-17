package com.mediseguimiento.app.ui.historial;

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
import com.mediseguimiento.app.data.db.entity.Estudio;
import com.mediseguimiento.app.data.db.entity.PreparacionConsulta;
import com.mediseguimiento.app.data.repo.CitaRepository;
import com.mediseguimiento.app.data.repo.EstudioRepository;
import com.mediseguimiento.app.data.repo.PreparacionRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Historial cronológico (RF-05). Las consultas y los estudios se transforman en
 * elementos de la línea de tiempo <b>en paralelo</b> (dos tareas en el grupo de cómputo);
 * después se combinan, se ordenan por fecha y se agrupan por mes.
 */
public class HistorialViewModel extends AndroidViewModel {

    public enum Filtro { TODO, CONSULTAS, ESTUDIOS }

    /** Elemento de la línea de tiempo. */
    public static final class Item {
        public static final int MES = 0, CITA = 1, ESTUDIO = 2;
        public final int tipo;
        public final long id;
        public final long fecha;
        public final String titulo;
        @Nullable public final String cuerpo;
        @Nullable public final String etiqueta;
        public final boolean futura;
        @Nullable public final String clave; // especialidad o nombre, para el color

        Item(int tipo, long id, long fecha, String titulo, @Nullable String cuerpo,
             @Nullable String etiqueta, boolean futura, @Nullable String clave) {
            this.tipo = tipo;
            this.id = id;
            this.fecha = fecha;
            this.titulo = titulo;
            this.cuerpo = cuerpo;
            this.etiqueta = etiqueta;
            this.futura = futura;
            this.clave = clave;
        }
    }

    private final LiveData<List<Cita>> citas;
    private final LiveData<List<Estudio>> estudios;
    private final LiveData<List<PreparacionConsulta>> preps;
    private final MutableLiveData<Filtro> filtro = new MutableLiveData<>(Filtro.TODO);
    private final MediatorLiveData<List<Item>> items = new MediatorLiveData<>();
    private final AtomicInteger generacion = new AtomicInteger();

    public HistorialViewModel(@NonNull Application app) {
        super(app);
        citas = new CitaRepository(app).observarTodas();
        estudios = new EstudioRepository(app).observarTodos();
        preps = new PreparacionRepository(app).observarTodas();
        items.addSource(citas, x -> recalcular());
        items.addSource(estudios, x -> recalcular());
        items.addSource(preps, x -> recalcular());
        items.addSource(filtro, x -> recalcular());
    }

    public LiveData<List<Item>> items() { return items; }

    public void setFiltro(Filtro f) { if (f != filtro.getValue()) filtro.setValue(f); }

    private void recalcular() {
        if (citas.getValue() == null || estudios.getValue() == null) return;
        List<Cita> cs = new ArrayList<>(citas.getValue());
        List<Estudio> es = new ArrayList<>(estudios.getValue());
        List<PreparacionConsulta> ps = preps.getValue() == null ? new ArrayList<>() : new ArrayList<>(preps.getValue());
        Filtro f = filtro.getValue();
        long ahora = System.currentTimeMillis();
        int gen = generacion.incrementAndGet();
        AppExecutors ex = AppExecutors.get();

        CompletableFuture<List<Item>> fCitas = CompletableFuture.supplyAsync(
                () -> f == Filtro.ESTUDIOS ? new ArrayList<>() : deCitas(cs, ps, ahora), ex.computo());
        CompletableFuture<List<Item>> fEstudios = CompletableFuture.supplyAsync(
                () -> f == Filtro.CONSULTAS ? new ArrayList<>() : deEstudios(es), ex.computo());

        fCitas.thenCombine(fEstudios, (a, b) -> {
            List<Item> todos = new ArrayList<>(a);
            todos.addAll(b);
            todos.sort((x, y) -> Long.compare(y.fecha, x.fecha));
            return agruparPorMes(todos);
        }).thenAccept(lista -> {
            if (gen == generacion.get()) items.postValue(lista);
        });
    }

    static List<Item> deCitas(List<Cita> cs, List<PreparacionConsulta> ps, long ahora) {
        Map<Long, PreparacionConsulta> porCita = new HashMap<>();
        for (PreparacionConsulta p : ps) porCita.put(p.citaId, p);
        List<Item> out = new ArrayList<>();
        for (Cita c : cs) {
            if (Cita.ESTADO_CANCELADA.equals(c.estado)) continue;
            PreparacionConsulta p = porCita.get(c.id);
            String cuerpo = p != null && p.motivo != null ? p.motivo : c.medico;
            boolean futura = c.fechaHora >= ahora && Cita.ESTADO_PROGRAMADA.equals(c.estado);
            int n = PreparacionRepository.completados(p);
            String etiqueta = futura ? "Próxima · Preparación " + n + "/" + PreparacionRepository.APARTADOS
                    : Cita.ESTADO_COMPLETADA.equals(c.estado) ? "Completada"
                    : n > 0 ? "Preparación " + n + "/" + PreparacionRepository.APARTADOS : null;
            out.add(new Item(Item.CITA, c.id, c.fechaHora, c.especialidad, cuerpo, etiqueta, futura, c.especialidad));
        }
        return out;
    }

    static List<Item> deEstudios(List<Estudio> es) {
        List<Item> out = new ArrayList<>();
        for (Estudio e : es) {
            String cuerpo = e.resultado != null ? e.resultado : e.laboratorio;
            out.add(new Item(Item.ESTUDIO, e.id, e.fecha, e.nombre, cuerpo, e.laboratorio, false, e.nombre));
        }
        return out;
    }

    static List<Item> agruparPorMes(List<Item> ordenados) {
        List<Item> out = new ArrayList<>();
        String mes = null;
        for (Item it : ordenados) {
            String m = Fechas.mesAnio(Fechas.aFecha(it.fecha));
            if (!m.equals(mes)) {
                mes = m;
                long clave = Fechas.aFecha(it.fecha).withDayOfMonth(1).toEpochDay();
                out.add(new Item(Item.MES, -clave - 1, it.fecha, m, null, null, false, null));
            }
            out.add(it);
        }
        return out;
    }
}

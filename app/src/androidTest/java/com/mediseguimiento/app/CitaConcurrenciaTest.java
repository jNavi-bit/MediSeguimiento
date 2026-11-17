package com.mediseguimiento.app;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mediseguimiento.app.data.db.AppDatabase;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.db.entity.Paciente;
import com.mediseguimiento.app.data.repo.CitaRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Pruebas de concurrencia sobre Room real (RNF-04, PI-05): varias tareas intentan
 * modificar la misma información al mismo tiempo y los datos deben quedar consistentes.
 */
@RunWith(AndroidJUnit4.class)
public class CitaConcurrenciaTest {

    private AppDatabase db;
    private CitaRepository repo;
    private ExecutorService pool;

    @Before
    public void preparar() {
        Context ctx = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase.class).build();
        Paciente p = new Paciente();
        p.nombre = "Paciente de prueba";
        p.correo = "prueba@correo.com";
        p.passwordHash = "x";
        p.sal = "y";
        long id = db.pacienteDao().insertar(p);
        repo = new CitaRepository(db, id);
        pool = Executors.newFixedThreadPool(8);
    }

    @After
    public void cerrar() {
        pool.shutdownNow();
        db.close();
    }

    private static Cita cita(String especialidad, long fecha) {
        Cita c = new Cita();
        c.especialidad = especialidad;
        c.fechaHora = fecha;
        return c;
    }

    /** Ocho hilos crean la misma cita a la vez: solo una debe guardarse. */
    @Test
    public void creacionesSimultaneas_noDuplican() throws Exception {
        long fecha = System.currentTimeMillis() + 86_400_000L;
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> fs = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            fs.add(pool.submit(() -> {
                salida.await();
                try {
                    repo.crearSincrono(cita("Cardiología", fecha));
                    return true;
                } catch (CitaRepository.ErrorCita e) {
                    assertEquals(CitaRepository.Falla.DUPLICADA, e.falla);
                    return false;
                }
            }));
        }
        salida.countDown(); // todos arrancan al mismo tiempo
        int exitos = 0;
        for (Future<Boolean> f : fs) if (f.get(10, TimeUnit.SECONDS)) exitos++;
        assertEquals(1, exitos);
        assertEquals(1, db.citaDao().listarCronologico(repo == null ? 0 : db.pacienteDao()
                .buscarPorCorreo("prueba@correo.com").id).size());
    }

    /** Dos ediciones parten de la misma versión: una gana y la otra detecta el conflicto. */
    @Test
    public void edicionesSimultaneas_controlOptimista() throws Exception {
        long id = repo.crearSincrono(cita("Nefrología", System.currentTimeMillis() + 86_400_000L));
        long pid = db.pacienteDao().buscarPorCorreo("prueba@correo.com").id;
        Cita leida1 = db.citaDao().obtener(id, pid);
        Cita leida2 = db.citaDao().obtener(id, pid);
        leida1.notas = "Edición A";
        leida2.notas = "Edición B";

        CountDownLatch salida = new CountDownLatch(1);
        Future<String> a = pool.submit(() -> { salida.await(); return intentar(leida1); });
        Future<String> b = pool.submit(() -> { salida.await(); return intentar(leida2); });
        salida.countDown();
        String ra = a.get(10, TimeUnit.SECONDS), rb = b.get(10, TimeUnit.SECONDS);

        int ok = (ra.equals("OK") ? 1 : 0) + (rb.equals("OK") ? 1 : 0);
        int conflictos = (ra.equals("CONFLICTO_VERSION") ? 1 : 0) + (rb.equals("CONFLICTO_VERSION") ? 1 : 0);
        assertEquals(1, ok);
        assertEquals(1, conflictos);

        Cita fin = db.citaDao().obtener(id, pid);
        assertEquals(1, fin.version);
        // La nota guardada es exactamente la del ganador: no hay mezcla ni pérdida silenciosa.
        assertEquals(ra.equals("OK") ? "Edición A" : "Edición B", fin.notas);
    }

    private String intentar(Cita c) {
        try {
            repo.actualizarSincrono(c);
            return "OK";
        } catch (CitaRepository.ErrorCita e) {
            return e.falla.name();
        }
    }
}

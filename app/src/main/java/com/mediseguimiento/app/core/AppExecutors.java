package com.mediseguimiento.app.core;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ejecutores compartidos de la aplicación (RNF-03: nada de E/S en el hilo principal).
 *
 * <ul>
 *   <li>{@link #io()}: grupo fijo de hilos para lecturas y escrituras en Room.</li>
 *   <li>{@link #computo()}: grupo para tareas de cálculo, como la generación de resúmenes,
 *       separado de E/S para que un resumen largo no retrase el guardado de formularios.</li>
 *   <li>{@link #main()}: publica resultados en el hilo de la interfaz.</li>
 * </ul>
 */
public final class AppExecutors {

    private static volatile AppExecutors instancia;

    private final ExecutorService io;
    private final ExecutorService computo;
    private final Executor main;

    private AppExecutors() {
        io = Executors.newFixedThreadPool(3, new FabricaHilos("medi-io"));
        int nucleos = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        computo = Executors.newFixedThreadPool(nucleos, new FabricaHilos("medi-computo"));
        main = new MainThreadExecutor();
    }

    public static AppExecutors get() {
        AppExecutors local = instancia;
        if (local == null) {
            synchronized (AppExecutors.class) {
                local = instancia;
                if (local == null) {
                    local = new AppExecutors();
                    instancia = local;
                }
            }
        }
        return local;
    }

    public ExecutorService io() { return io; }

    public ExecutorService computo() { return computo; }

    public Executor main() { return main; }

    private static final class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            handler.post(command);
        }
    }

    /** Asigna nombres legibles a los hilos para facilitar la depuración. */
    private static final class FabricaHilos implements ThreadFactory {
        private final String prefijo;
        private final AtomicInteger contador = new AtomicInteger(1);

        FabricaHilos(String prefijo) { this.prefijo = prefijo; }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(r, prefijo + "-" + contador.getAndIncrement());
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        }
    }
}

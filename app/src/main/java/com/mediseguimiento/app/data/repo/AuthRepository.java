package com.mediseguimiento.app.data.repo;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.annotation.NonNull;

import com.mediseguimiento.app.core.AppExecutors;
import com.mediseguimiento.app.core.Resultado;
import com.mediseguimiento.app.core.Validaciones;
import com.mediseguimiento.app.core.seguridad.ControlIntentos;
import com.mediseguimiento.app.core.seguridad.PasswordHasher;
import com.mediseguimiento.app.data.db.AppDatabase;
import com.mediseguimiento.app.data.db.dao.PacienteDao;
import com.mediseguimiento.app.data.db.entity.Paciente;

import java.util.Arrays;

/**
 * Registro e inicio de sesión locales (RF-01).
 *
 * <p>Cada operación es una pequeña cadena de tareas: la derivación de la contraseña
 * (costosa en CPU) corre en el grupo de cómputo, el acceso a Room en el grupo de E/S
 * y el resultado se publica en el hilo principal. Así la interfaz nunca se congela (RNF-03).</p>
 */
public class AuthRepository {

    public interface Callback<T> {
        void alTerminar(@NonNull Resultado<T> resultado);
    }

    /** Resultado de un inicio de sesión fallido con información para la interfaz. */
    public static final class Rechazo extends RuntimeException {
        public final long bloqueoMs;
        public final int intentosRestantes;

        Rechazo(String mensaje, long bloqueoMs, int intentosRestantes) {
            super(mensaje);
            this.bloqueoMs = bloqueoMs;
            this.intentosRestantes = intentosRestantes;
        }
    }

    private static final class CorreoEnUso extends RuntimeException {}

    private static final String MSG_CREDENCIALES = "Correo o contraseña incorrectos";
    /**
     * Hash ficticio para igualar el tiempo de respuesta cuando el correo no existe.
     * Se inicializa de forma perezosa y segura entre hilos (idioma "holder"): la JVM
     * garantiza que la clase se inicializa una sola vez, y ocurre en un hilo de cómputo.
     */
    private static final class Senuelo {
        static final PasswordHasher.Derivado VALOR =
                PasswordHasher.derivar("senuelo-medi".toCharArray());
    }

    private final AppDatabase db;
    private final PacienteDao dao;
    private final AppExecutors ex;
    private final ControlIntentos intentos;

    public AuthRepository(Context context) {
        db = AppDatabase.get(context);
        dao = db.pacienteDao();
        ex = AppExecutors.get();
        intentos = new ControlIntentos(context);
    }

    public void registrar(String nombre, String correo, char[] password, Callback<Long> cb) {
        final String correoN = Validaciones.normalizarCorreo(correo);
        final String nombreN = nombre.trim();
        ex.computo().execute(() -> {
            PasswordHasher.Derivado d = PasswordHasher.derivar(password);
            Arrays.fill(password, '\0');
            ex.io().execute(() -> {
                Resultado<Long> r;
                try {
                    // Transacción: comprobar y crear de forma atómica evita cuentas duplicadas
                    // aunque dos solicitudes lleguen al mismo tiempo (RNF-04).
                    long id = db.runInTransaction(() -> {
                        if (dao.buscarPorCorreo(correoN) != null) throw new CorreoEnUso();
                        Paciente p = new Paciente();
                        p.nombre = nombreN;
                        p.correo = correoN;
                        p.passwordHash = d.hash;
                        p.sal = d.sal;
                        p.creadoEn = System.currentTimeMillis();
                        return dao.insertar(p);
                    });
                    r = Resultado.exito(id);
                } catch (CorreoEnUso | SQLiteConstraintException e) {
                    r = Resultado.error("Ya existe una cuenta con este correo");
                } catch (RuntimeException e) {
                    r = Resultado.error("No se pudo crear la cuenta. Intenta de nuevo.");
                }
                Resultado<Long> fin = r;
                ex.main().execute(() -> cb.alTerminar(fin));
            });
        });
    }

    public void iniciarSesion(String correo, char[] password, Callback<Long> cb) {
        final String correoN = Validaciones.normalizarCorreo(correo);
        ex.io().execute(() -> {
            long ahora = System.currentTimeMillis();
            long bloqueo = intentos.bloqueoRestante(correoN, ahora);
            if (bloqueo > 0) {
                Arrays.fill(password, '\0');
                entregarRechazo(cb, new Rechazo("Demasiados intentos", bloqueo, 0));
                return;
            }
            Paciente p = dao.buscarPorCorreo(correoN);
            ex.computo().execute(() -> {
                boolean ok;
                if (p != null) {
                    ok = PasswordHasher.verificar(password, p.passwordHash, p.sal);
                } else {
                    PasswordHasher.verificar(password, Senuelo.VALOR.hash, Senuelo.VALOR.sal);
                    ok = false;
                }
                Arrays.fill(password, '\0');
                if (ok) {
                    intentos.reiniciar(correoN);
                    ex.main().execute(() -> cb.alTerminar(Resultado.exito(p.id)));
                } else {
                    long t = System.currentTimeMillis();
                    int restantes = intentos.registrarFallo(correoN, t);
                    long b = intentos.bloqueoRestante(correoN, t);
                    entregarRechazo(cb, new Rechazo(MSG_CREDENCIALES, b, restantes));
                }
            });
        });
    }

    private void entregarRechazo(Callback<Long> cb, Rechazo r) {
        ex.main().execute(() -> cb.alTerminar(Resultado.error(r.getMessage(), r)));
    }
}

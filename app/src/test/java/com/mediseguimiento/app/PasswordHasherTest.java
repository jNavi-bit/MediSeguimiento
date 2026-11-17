package com.mediseguimiento.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.mediseguimiento.app.core.seguridad.PasswordHasher;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PasswordHasherTest {

    @Test
    public void verifica_contrasenaCorrecta() {
        PasswordHasher.Derivado d = PasswordHasher.derivar("clave1234".toCharArray());
        assertTrue(PasswordHasher.verificar("clave1234".toCharArray(), d.hash, d.sal));
        assertFalse(PasswordHasher.verificar("clave1235".toCharArray(), d.hash, d.sal));
    }

    @Test
    public void salUnica_generaHashesDistintos() {
        PasswordHasher.Derivado a = PasswordHasher.derivar("misma".toCharArray());
        PasswordHasher.Derivado b = PasswordHasher.derivar("misma".toCharArray());
        assertNotEquals(a.sal, b.sal);
        assertNotEquals(a.hash, b.hash);
    }

    /** El hasher no comparte estado mutable: es seguro usarlo desde varios hilos a la vez. */
    @Test
    public void seguroEnParalelo() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Boolean>> fs = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                final String pw = "clave" + i + "abc";
                fs.add(pool.submit((Callable<Boolean>) () -> {
                    PasswordHasher.Derivado d = PasswordHasher.derivar(pw.toCharArray());
                    return PasswordHasher.verificar(pw.toCharArray(), d.hash, d.sal);
                }));
            }
            int ok = 0;
            for (Future<Boolean> f : fs) if (f.get()) ok++;
            assertEquals(8, ok);
        } finally {
            pool.shutdownNow();
        }
    }
}

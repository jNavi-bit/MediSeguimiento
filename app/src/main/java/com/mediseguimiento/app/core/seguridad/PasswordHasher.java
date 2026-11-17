package com.mediseguimiento.app.core.seguridad;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Derivación de contraseñas con PBKDF2-HMAC-SHA256 y sal aleatoria por usuario (RNF-05).
 * Es una operación deliberadamente costosa: siempre se ejecuta fuera del hilo principal.
 */
public final class PasswordHasher {

    public static final int ITERACIONES = 120_000;
    private static final int LONGITUD_BITS = 256;
    private static final int LONGITUD_SAL = 16;
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    /** Resultado de derivar una contraseña: hash y sal en Base64. */
    public static final class Derivado {
        public final String hash;
        public final String sal;

        Derivado(String hash, String sal) {
            this.hash = hash;
            this.sal = sal;
        }
    }

    public static Derivado derivar(char[] password) {
        byte[] sal = new byte[LONGITUD_SAL];
        RANDOM.nextBytes(sal);
        byte[] hash = pbkdf2(password, sal);
        Base64.Encoder enc = Base64.getEncoder();
        return new Derivado(enc.encodeToString(hash), enc.encodeToString(sal));
    }

    /** Compara en tiempo constante para no filtrar información por temporización. */
    public static boolean verificar(char[] password, String hashB64, String salB64) {
        Base64.Decoder dec = Base64.getDecoder();
        byte[] esperado = dec.decode(hashB64);
        byte[] calculado = pbkdf2(password, dec.decode(salB64));
        return MessageDigest.isEqual(esperado, calculado);
    }

    private static byte[] pbkdf2(char[] password, byte[] sal) {
        PBEKeySpec spec = new PBEKeySpec(password, sal, ITERACIONES, LONGITUD_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 no disponible", e);
        } finally {
            spec.clearPassword();
        }
    }
}

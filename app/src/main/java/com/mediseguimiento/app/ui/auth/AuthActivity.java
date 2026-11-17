package com.mediseguimiento.app.ui.auth;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;
import com.google.android.material.textfield.TextInputLayout;
import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Resultado;
import com.mediseguimiento.app.core.Sesion;
import com.mediseguimiento.app.core.Validaciones;
import com.mediseguimiento.app.core.seguridad.ControlIntentos;
import com.mediseguimiento.app.data.repo.AuthRepository;
import com.mediseguimiento.app.databinding.ActivityAuthBinding;
import com.mediseguimiento.app.ui.MainActivity;
import com.mediseguimiento.app.ui.common.Animaciones;
import com.mediseguimiento.app.ui.common.Ventana;

/**
 * Inicio de sesión y registro en una sola pantalla (RF-01), con transición animada
 * entre ambos modos, validación en vivo y medidor de seguridad de la contraseña.
 */
public class AuthActivity extends AppCompatActivity {

    public static final String EXTRA_REGISTRO = "registro";

    private ActivityAuthBinding b;
    private AuthViewModel vm;
    private boolean intentado;
    private boolean primerModo = true;
    private CountDownTimer temporizadorBloqueo;
    private final View[] barras = new View[4];
    private final int[] colorBarra = new int[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        b = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(AuthViewModel.class);
        if (savedInstanceState == null) {
            vm.setModoRegistro(getIntent().getBooleanExtra(EXTRA_REGISTRO, false));
        }

        Ventana.iconosBarraEstado(this, true);
        Ventana.aplicarPaddingSuperior(b.encabezado);
        Ventana.aplicarPaddingInferior(b.contenido);

        b.btnVolver.setVisibility(isTaskRoot() ? View.INVISIBLE : View.VISIBLE);
        b.btnVolver.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        b.tabLogin.setOnClickListener(v -> cambiarModo(false));
        b.tabRegistro.setOnClickListener(v -> cambiarModo(true));

        barras[0] = b.f1; barras[1] = b.f2; barras[2] = b.f3; barras[3] = b.f4;
        int inactivo = MaterialColors.getColor(b.f1, com.google.android.material.R.attr.colorSurfaceContainerHigh);
        for (int i = 0; i < 4; i++) {
            barras[i].getBackground().mutate().setTint(inactivo);
            colorBarra[i] = inactivo;
        }

        configurarCampos();
        b.btnAccion.setOnClickListener(v -> enviar());
        configurarDemo();

        vm.modoRegistro().observe(this, this::aplicarModo);
        vm.estado().observe(this, this::render);

        if (savedInstanceState == null) {
            Animaciones.entrar(b.tarjeta, 120);
            Animaciones.entrar(b.pie, 260);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (temporizadorBloqueo != null) temporizadorBloqueo.cancel();
    }

    // ================= Cuenta de demostración =================

    /** Solo en la versión de prueba: crea (o reinicia) la cuenta demo y entra con ella. */
    private void configurarDemo() {
        if (!com.mediseguimiento.app.BuildConfig.DEBUG) return;
        b.btnDemo.setVisibility(View.VISIBLE);
        b.demoCredenciales.setVisibility(View.VISIBLE);
        Animaciones.alinearIcono(b.btnDemo);
        b.btnDemo.setOnClickListener(v -> {
            if (vm.ocupado()) return;
            com.mediseguimiento.app.ui.common.Botones.cargando(b.btnDemo, getString(R.string.demo_cargando));
            com.mediseguimiento.app.core.AppExecutors ex = com.mediseguimiento.app.core.AppExecutors.get();
            ex.io().execute(() -> {
                long id = com.mediseguimiento.app.core.demo.DatosDemo.crear(this);
                ex.main().execute(() -> {
                    new Sesion(this).iniciar(id);
                    Animaciones.vibrarExito(b.btnDemo);
                    irAInicio();
                });
            });
        });
    }

    // ================= Modo (login / registro) =================

    private void cambiarModo(boolean registro) {
        if (vm.ocupado()) return;
        Animaciones.vibrarToque(b.segmento);
        vm.setModoRegistro(registro);
    }

    private void aplicarModo(boolean registro) {
        boolean animar = !primerModo;
        primerModo = false;
        if (animar) {
            AutoTransition t = new AutoTransition();
            t.setDuration(280);
            t.setInterpolator(Animaciones.ENFATIZADA);
            TransitionManager.beginDelayedTransition(b.formulario, t);
        }
        int vis = registro ? View.VISIBLE : View.GONE;
        b.tilNombre.setVisibility(vis);
        b.medidor.setVisibility(vis);
        b.tilConfirmar.setVisibility(vis);
        b.cbConsentimiento.setVisibility(vis);
        b.banner.setVisibility(View.GONE);
        b.tilPassword.setHelperText(registro ? getString(R.string.ayuda_password) : null);

        // El último campo cierra el teclado y envía el formulario.
        b.etPassword.setImeOptions(registro ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
        b.etPassword.setAutofillHints(registro ? "newPassword" : View.AUTOFILL_HINT_PASSWORD);

        intentado = false;
        limpiarErrores();
        b.btnAccion.setText(registro ? R.string.btn_crear : R.string.btn_entrar);
        b.btnAccion.setIcon(null);

        cambiarTexto(b.titulo, getString(registro ? R.string.auth_titulo_registro : R.string.auth_titulo_login), animar);
        cambiarTexto(b.subtitulo, getString(registro ? R.string.auth_sub_registro : R.string.auth_sub_login), animar);

        int activo = MaterialColors.getColor(b.tabLogin, com.google.android.material.R.attr.colorOnSurface);
        int inactivo = MaterialColors.getColor(b.tabLogin, com.google.android.material.R.attr.colorOnSurfaceVariant);
        b.tabLogin.setTextColor(registro ? inactivo : activo);
        b.tabRegistro.setTextColor(registro ? activo : inactivo);
        b.tabLogin.setSelected(!registro);
        b.tabRegistro.setSelected(registro);

        b.segmento.post(() -> moverIndicador(registro, animar));
        if (registro) actualizarMedidor();
    }

    /** Desliza la "píldora" del control segmentado con un pequeño rebote. */
    private void moverIndicador(boolean registro, boolean animar) {
        int interior = b.segmento.getWidth() - b.segmento.getPaddingLeft() - b.segmento.getPaddingRight();
        int mitad = interior / 2;
        if (b.segmentoThumb.getLayoutParams().width != mitad) {
            b.segmentoThumb.getLayoutParams().width = mitad;
            b.segmentoThumb.requestLayout();
        }
        float destino = registro ? mitad : 0;
        if (animar) {
            b.segmentoThumb.animate().translationX(destino).setDuration(360)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.1f)).start();
        } else {
            b.segmentoThumb.setTranslationX(destino);
        }
    }

    private static void cambiarTexto(TextView tv, CharSequence texto, boolean animar) {
        if (!animar) {
            tv.setText(texto);
            return;
        }
        tv.animate().alpha(0f).translationY(-8f).setDuration(140).withEndAction(() -> {
            tv.setText(texto);
            tv.setTranslationY(8f);
            tv.animate().alpha(1f).translationY(0f).setDuration(260)
                    .setInterpolator(Animaciones.ENFATIZADA).start();
        }).start();
    }

    // ================= Campos y validación =================

    private void configurarCampos() {
        TextWatcher revalidar = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int bf, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (intentado) validar(false);
                if (b.banner.getVisibility() == View.VISIBLE && temporizadorBloqueo == null) ocultarBanner();
            }
        };
        b.etNombre.addTextChangedListener(revalidar);
        b.etCorreo.addTextChangedListener(revalidar);
        b.etConfirmar.addTextChangedListener(revalidar);
        b.etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int bf, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (esRegistro()) actualizarMedidor();
                revalidar.afterTextChanged(s);
            }
        });
        b.cbConsentimiento.setOnCheckedChangeListener((v, c) -> {
            if (intentado) validar(false);
        });

        TextView.OnEditorActionListener alTerminar = (v, id, ev) -> {
            if (id == EditorInfo.IME_ACTION_DONE) {
                enviar();
                return true;
            }
            return false;
        };
        b.etPassword.setOnEditorActionListener(alTerminar);
        b.etConfirmar.setOnEditorActionListener(alTerminar);
    }

    private boolean esRegistro() {
        return Boolean.TRUE.equals(vm.modoRegistro().getValue());
    }

    private void limpiarErrores() {
        for (TextInputLayout t : new TextInputLayout[]{b.tilNombre, b.tilCorreo, b.tilPassword, b.tilConfirmar}) {
            t.setError(null);
            t.setErrorEnabled(false);
        }
        b.cbConsentimiento.setErrorShown(false);
    }

    /** Valida todos los campos visibles; marca errores y opcionalmente enfoca el primero. */
    private boolean validar(boolean enfocar) {
        boolean registro = esRegistro();
        View primero = null;

        if (registro) primero = marcar(b.tilNombre, Validaciones.nombre(texto(b.etNombre)), primero);
        primero = marcar(b.tilCorreo, Validaciones.correo(texto(b.etCorreo)), primero);
        primero = marcar(b.tilPassword, registro
                ? Validaciones.passwordNueva(b.etPassword.getText())
                : Validaciones.passwordIngresada(b.etPassword.getText()), primero);
        if (registro) {
            primero = marcar(b.tilConfirmar,
                    Validaciones.confirmacion(b.etPassword.getText(), b.etConfirmar.getText()), primero);
            boolean ok = b.cbConsentimiento.isChecked();
            b.cbConsentimiento.setErrorAccessibilityLabel(getString(R.string.error_consentimiento));
            b.cbConsentimiento.setErrorShown(!ok);
            if (!ok && primero == null) primero = b.cbConsentimiento;
        }
        if (enfocar && primero != null) primero.requestFocus();
        return primero == null;
    }

    @Nullable
    private static View marcar(TextInputLayout til, @Nullable String error, @Nullable View primero) {
        til.setError(error);
        til.setErrorEnabled(error != null);
        return primero == null && error != null ? til : primero;
    }

    private static String texto(TextView tv) {
        return tv.getText() == null ? "" : tv.getText().toString();
    }

    private static char[] caracteres(@Nullable Editable e) {
        if (e == null) return new char[0];
        char[] c = new char[e.length()];
        e.getChars(0, e.length(), c, 0);
        return c;
    }

    // ================= Medidor de seguridad =================

    private void actualizarMedidor() {
        int fuerza = Validaciones.fuerzaPassword(b.etPassword.getText());
        int inactivo = MaterialColors.getColor(b.f1, com.google.android.material.R.attr.colorSurfaceContainerHigh);
        int color;
        int etiqueta;
        switch (fuerza) {
            case 1: color = MaterialColors.getColor(b.f1, androidx.appcompat.R.attr.colorError); etiqueta = R.string.fuerza_debil; break;
            case 2: color = MaterialColors.getColor(b.f1, com.google.android.material.R.attr.colorTertiary); etiqueta = R.string.fuerza_regular; break;
            case 3: color = ContextCompat.getColor(this, R.color.fuerza_media); etiqueta = R.string.fuerza_buena; break;
            case 4: color = ContextCompat.getColor(this, R.color.accent_historial); etiqueta = R.string.fuerza_excelente; break;
            default: color = inactivo; etiqueta = 0;
        }
        for (int i = 0; i < 4; i++) animarColor(i, i < fuerza ? color : inactivo, i * 40L);
        b.fuerzaTexto.setText(etiqueta == 0 ? "" : getString(etiqueta));
        b.fuerzaTexto.setTextColor(color);
    }

    private void animarColor(int i, @ColorInt int hasta, long retraso) {
        int desde = colorBarra[i];
        if (desde == hasta) return;
        colorBarra[i] = hasta;
        ValueAnimator va = ValueAnimator.ofObject(new ArgbEvaluator(), desde, hasta);
        va.setDuration(240);
        va.setStartDelay(retraso);
        va.addUpdateListener(a -> barras[i].getBackground().setTint((int) a.getAnimatedValue()));
        va.start();
    }

    // ================= Envío y resultado =================

    private void enviar() {
        if (vm.ocupado() || temporizadorBloqueo != null) return;
        intentado = true;
        // El aviso anterior se conserva durante la verificación; solo se actualiza su texto
        // al llegar la respuesta. Ocultarlo aquí competía con la animación de mostrarlo.
        if (!validar(true)) {
            Animaciones.sacudir(b.tarjeta);
            Animaciones.vibrarError(b.tarjeta);
            return;
        }
        WindowCompat.getInsetsController(getWindow(), b.getRoot()).hide(WindowInsetsCompat.Type.ime());
        String correo = texto(b.etCorreo);
        char[] password = caracteres(b.etPassword.getText());
        if (esRegistro()) {
            vm.registrar(texto(b.etNombre), correo, password);
        } else {
            vm.iniciarSesion(correo, password);
        }
    }

    private void render(@Nullable Resultado<Long> r) {
        if (r == null) return;
        switch (r.estado) {
            case CARGANDO:
                setCargando(true);
                break;
            case EXITO:
                vm.consumir();
                exito(r.datos == null ? -1 : r.datos);
                break;
            case ERROR:
                vm.consumir();
                setCargando(false);
                error(r);
                break;
        }
    }

    private void setCargando(boolean cargando) {
        boolean registro = esRegistro();
        for (View v : new View[]{b.etNombre, b.etCorreo, b.etPassword, b.etConfirmar,
                b.cbConsentimiento, b.tabLogin, b.tabRegistro}) {
            v.setEnabled(!cargando);
        }
        b.btnAccion.setClickable(!cargando);
        if (cargando) {
            CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(this, null, 0,
                    com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall);
            spec.indicatorColors = new int[]{MaterialColors.getColor(b.btnAccion,
                    com.google.android.material.R.attr.colorOnPrimary)};
            b.btnAccion.setIcon(IndeterminateDrawable.createCircularDrawable(this, spec));
            b.btnAccion.setText(registro ? R.string.btn_creando : R.string.btn_verificando);
        } else {
            b.btnAccion.setIcon(null);
            b.btnAccion.setText(registro ? R.string.btn_crear : R.string.btn_entrar);
        }
    }

    private void exito(long pacienteId) {
        b.btnAccion.setIconResource(R.drawable.ic_check);
        b.btnAccion.setText(R.string.btn_listo);
        b.btnAccion.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.accent_historial)));
        Animaciones.vibrarExito(b.btnAccion);
        new Sesion(this).iniciar(pacienteId);
        b.btnAccion.postDelayed(this::irAInicio, 550);
    }

    private void irAInicio() {
        Intent i = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i, ActivityOptionsCompat.makeCustomAnimation(this,
                R.anim.fade_entrar, R.anim.fade_salir).toBundle());
        finish();
    }

    private void error(Resultado<Long> r) {
        Animaciones.sacudir(b.tarjeta);
        Animaciones.vibrarError(b.tarjeta);
        if (r.detalle instanceof AuthRepository.Rechazo) {
            AuthRepository.Rechazo rr = (AuthRepository.Rechazo) r.detalle;
            if (rr.bloqueoMs > 0) {
                iniciarBloqueo(rr.bloqueoMs);
                return;
            }
            if (rr.intentosRestantes < ControlIntentos.MAX_INTENTOS - 1) {
                mostrarBanner(getString(R.string.intentos_restantes, rr.intentosRestantes));
                return;
            }
        }
        mostrarBanner(r.mensaje);
    }

    private void iniciarBloqueo(long ms) {
        b.btnAccion.setEnabled(false);
        mostrarBanner(getString(R.string.bloqueo_cuenta, (int) Math.ceil(ms / 1000.0)));
        temporizadorBloqueo = new CountDownTimer(ms, 1000) {
            @Override
            public void onTick(long restante) {
                b.bannerTexto.setText(getString(R.string.bloqueo_cuenta, (int) Math.ceil(restante / 1000.0)));
            }

            @Override
            public void onFinish() {
                temporizadorBloqueo = null;
                b.btnAccion.setEnabled(true);
                ocultarBanner();
            }
        }.start();
    }

    private void mostrarBanner(@Nullable String mensaje) {
        b.bannerTexto.setText(mensaje);
        // Termina cualquier transición en curso: al finalizar, una transición de salida
        // reaplica su visibilidad final (GONE) y ocultaría el aviso recién mostrado.
        TransitionManager.endTransitions(b.formulario);
        if (b.banner.getVisibility() != View.VISIBLE) {
            AutoTransition t = new AutoTransition();
            t.setDuration(240);
            TransitionManager.beginDelayedTransition(b.formulario, t);
            b.banner.setVisibility(View.VISIBLE);
        }
    }

    private void ocultarBanner() {
        if (b.banner.getVisibility() == View.GONE) return;
        TransitionManager.endTransitions(b.formulario);
        AutoTransition t = new AutoTransition();
        t.setDuration(200);
        TransitionManager.beginDelayedTransition(b.formulario, t);
        b.banner.setVisibility(View.GONE);
    }
}

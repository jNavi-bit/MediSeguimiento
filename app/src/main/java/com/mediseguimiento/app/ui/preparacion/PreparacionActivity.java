package com.mediseguimiento.app.ui.preparacion;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.core.Resultado;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.db.entity.PreparacionConsulta;
import com.mediseguimiento.app.data.repo.CitaRepository;
import com.mediseguimiento.app.data.repo.PreparacionRepository;
import com.mediseguimiento.app.databinding.ActivityPreparacionBinding;
import com.mediseguimiento.app.databinding.ItemCampoPreparacionBinding;
import com.mediseguimiento.app.ui.common.Animaciones;
import com.mediseguimiento.app.ui.common.Botones;
import com.mediseguimiento.app.ui.common.Ventana;

/**
 * Registro previo a la consulta (RF-03): motivo, síntomas, cambios recientes,
 * tratamientos y preguntas. El anillo muestra cuántos apartados están completos.
 */
public class PreparacionActivity extends AppCompatActivity {

    public static final String EXTRA_CITA = "cita_id";
    public static final String RESULTADO_MENSAJE = "mensaje";

    private ActivityPreparacionBinding b;
    private PreparacionRepository repo;
    private long citaId;
    private ItemCampoPreparacionBinding[] campos;
    private String inicial = "";
    private boolean cargado;
    private boolean guardando;
    private int ultimoProgreso = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        Ventana.animacionCierre(this, R.anim.quieto, R.anim.hoja_salir, false);
        b = ActivityPreparacionBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        repo = new PreparacionRepository(this);
        citaId = getIntent().getLongExtra(EXTRA_CITA, -1);

        Ventana.iconosBarraEstado(this, false);
        Ventana.aplicarPaddingSuperior(b.barraSuperior);
        Ventana.aplicarPaddingInferior(b.barraInferior);

        campos = new ItemCampoPreparacionBinding[]{b.campoMotivo, b.campoSintomas, b.campoCambios,
                b.campoTratamientos, b.campoPreguntas};
        campo(b.campoMotivo, R.string.prep_motivo, R.string.prep_motivo_ayuda, R.drawable.ic_medical,
                R.color.accent_agenda, R.color.accent_agenda_bg);
        campo(b.campoSintomas, R.string.prep_sintomas, R.string.prep_sintomas_ayuda, R.drawable.ic_favorite,
                R.color.accent_prep, R.color.accent_prep_bg);
        campo(b.campoCambios, R.string.prep_cambios, R.string.prep_cambios_ayuda, R.drawable.ic_trending,
                R.color.accent_estudios, R.color.accent_estudios_bg);
        campo(b.campoTratamientos, R.string.prep_tratamientos, R.string.prep_tratamientos_ayuda,
                R.drawable.ic_medicamento, R.color.accent_historial, R.color.accent_historial_bg);
        campo(b.campoPreguntas, R.string.prep_preguntas, R.string.prep_preguntas_ayuda, R.drawable.ic_help,
                R.color.accent_agenda, R.color.accent_agenda_bg);

        b.scroll.setOnScrollChangeListener((View.OnScrollChangeListener) (v, x, y, ox, oy) ->
                b.tituloBarra.setAlpha(Math.min(1f, y / (float) Math.max(1, b.titulo.getBottom()))));
        b.btnCerrar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        b.btnGuardar.setOnClickListener(v -> guardar());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { salir(); }
        });

        // Encabezado con los datos de la cita.
        new CitaRepository(this).obtener(citaId, r -> {
            Cita c = r.datos;
            if (c == null) { finish(); return; }
            b.citaResumen.setText((c.especialidad + " · " + Fechas.fechaCorta(c.fechaHora)).toUpperCase(Fechas.ES));
        });

        // Carga única de la preparación existente (si la hay).
        LiveData<PreparacionConsulta> ld = repo.observar(citaId);
        ld.observe(this, p -> {
            if (cargado) return;
            cargado = true;
            if (p != null && savedInstanceState == null) {
                b.campoMotivo.et.setText(p.motivo);
                b.campoSintomas.et.setText(p.sintomas);
                b.campoCambios.et.setText(p.cambiosRecientes);
                b.campoTratamientos.et.setText(p.tratamientos);
                b.campoPreguntas.et.setText(p.preguntas);
            }
            inicial = instantanea();
            actualizarProgreso(false);
        });
        actualizarProgreso(false);

        if (savedInstanceState == null) {
            Animaciones.entrar(b.tarjetaProgreso, 40);
            for (int i = 0; i < campos.length; i++) Animaciones.entrar(campos[i].getRoot(), 100 + i * 60L);
        }
    }

    private void campo(ItemCampoPreparacionBinding c, @StringRes int titulo, @StringRes int ayuda,
                       @DrawableRes int icono, @ColorRes int color, @ColorRes int fondo) {
        c.titulo.setText(titulo);
        c.ayuda.setText(ayuda);
        c.icono.setImageResource(icono);
        c.icono.setImageTintList(ContextCompat.getColorStateList(this, color));
        c.insignia.setBackgroundTintList(ContextCompat.getColorStateList(this, fondo));
        c.et.setContentDescription(getString(titulo));
        c.et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int b2, int d) {}
            @Override public void afterTextChanged(Editable s) { actualizarProgreso(true); }
        });
    }

    private static boolean lleno(ItemCampoPreparacionBinding c) {
        return c.et.getText() != null && !c.et.getText().toString().trim().isEmpty();
    }

    private void actualizarProgreso(boolean animar) {
        int n = 0;
        for (ItemCampoPreparacionBinding c : campos) {
            boolean ok = lleno(c);
            if (ok) n++;
            int vis = ok ? View.VISIBLE : View.INVISIBLE;
            if (c.listo.getVisibility() != vis) {
                c.listo.setVisibility(vis);
                if (ok && animar) Animaciones.aparecerEscalando(c.listo, 0);
            }
        }
        if (n == ultimoProgreso) return;
        if (animar && n > ultimoProgreso && ultimoProgreso >= 0) Animaciones.vibrarToque(b.anillo);
        ultimoProgreso = n;
        b.anillo.setProgressCompat(n, animar);
        b.progresoNumero.setText(getString(R.string.prep_progreso_corto, n, PreparacionRepository.APARTADOS));
        b.progresoTexto.setText(getString(R.string.prep_progreso, n, PreparacionRepository.APARTADOS));
    }

    @Nullable
    private static String valor(ItemCampoPreparacionBinding c) {
        String t = c.et.getText() == null ? "" : c.et.getText().toString().trim();
        return t.isEmpty() ? null : t;
    }

    private String instantanea() {
        StringBuilder sb = new StringBuilder();
        for (ItemCampoPreparacionBinding c : campos) sb.append(valor(c)).append('|');
        return sb.toString();
    }

    private void guardar() {
        if (guardando) return;
        guardando = true;
        WindowCompat.getInsetsController(getWindow(), b.getRoot()).hide(WindowInsetsCompat.Type.ime());
        Botones.cargando(b.btnGuardar, getString(R.string.guardando));
        PreparacionConsulta p = new PreparacionConsulta();
        p.citaId = citaId;
        p.motivo = valor(b.campoMotivo);
        p.sintomas = valor(b.campoSintomas);
        p.cambiosRecientes = valor(b.campoCambios);
        p.tratamientos = valor(b.campoTratamientos);
        p.preguntas = valor(b.campoPreguntas);
        repo.guardar(p, r -> {
            guardando = false;
            Botones.normal(b.btnGuardar, getString(R.string.guardar_preparacion), 0);
            if (r.estado == Resultado.Estado.EXITO) {
                Animaciones.vibrarExito(b.btnGuardar);
                setResult(RESULT_OK, new android.content.Intent()
                        .putExtra(RESULTADO_MENSAJE, getString(R.string.prep_guardada)));
                cerrar();
            } else {
                Animaciones.vibrarError(b.btnGuardar);
                new MaterialAlertDialogBuilder(this).setMessage(r.mensaje)
                        .setPositiveButton(android.R.string.ok, null).show();
            }
        });
    }

    private void salir() {
        if (guardando) return;
        if (cargado && !TextUtils.equals(instantanea(), inicial)) {
            new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Medi_Dialogo_Destructivo)
                    .setTitle(R.string.descartar_titulo)
                    .setMessage(R.string.descartar_cuerpo)
                    .setNegativeButton(R.string.seguir_editando, null)
                    .setPositiveButton(R.string.descartar, (d, w) -> cerrar())
                    .show();
        } else {
            cerrar();
        }
    }

    private void cerrar() {
        finish();
        Ventana.animacionCierre(this, R.anim.quieto, R.anim.hoja_salir, true);
    }
}

package com.mediseguimiento.app.ui.resumen;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;
import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Resultado;
import com.mediseguimiento.app.core.resumen.ResumenGenerador;
import com.mediseguimiento.app.data.repo.ResumenRepository;
import com.mediseguimiento.app.databinding.ActivityResumenBinding;
import com.mediseguimiento.app.databinding.ItemLineaResumenBinding;
import com.mediseguimiento.app.databinding.ViewSeccionResumenBinding;
import com.mediseguimiento.app.ui.common.Animaciones;
import com.mediseguimiento.app.ui.common.Ventana;

/**
 * Resumen estructurado de una cita (RF-05) que el paciente puede revisar y, solo con
 * su consentimiento explícito, copiar o compartir (RF-07).
 */
public class ResumenActivity extends AppCompatActivity {

    public static final String EXTRA_CITA = "cita_id";

    private ActivityResumenBinding b;
    private ResumenRepository repo;
    private long citaId;
    @Nullable private ResumenGenerador.Resumen resumen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        Ventana.animacionCierre(this, R.anim.deslizar_volver, R.anim.deslizar_salir, false);
        b = ActivityResumenBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        repo = new ResumenRepository(this);
        citaId = getIntent().getLongExtra(EXTRA_CITA, -1);

        Ventana.iconosBarraEstado(this, true);
        Ventana.aplicarPaddingSuperior(b.hero);
        Ventana.aplicarPaddingInferior(b.barra);
        b.encabezado.setText(R.string.resumen_compartir_titulo);
        b.btnVolver.setOnClickListener(v -> cerrar());
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { cerrar(); }
        });

        b.cbConsentimiento.setOnCheckedChangeListener((v, marcado) -> {
            b.tilDestinatario.setVisibility(marcado ? View.VISIBLE : View.GONE);
            actualizarBotones();
            if (marcado) Animaciones.vibrarToque(v);
        });
        b.btnCopiar.setOnClickListener(v -> accion(false));
        b.btnCompartir.setOnClickListener(v -> accion(true));
        actualizarBotones();

        repo.generar(citaId, DateFormat.is24HourFormat(this), this::mostrar);
    }

    private void actualizarBotones() {
        boolean listo = resumen != null && b.cbConsentimiento.isChecked();
        // Se mantienen visibles pero atenuados para explicar por qué no están disponibles.
        b.btnCompartir.setAlpha(listo ? 1f : 0.45f);
        b.btnCopiar.setAlpha(listo ? 1f : 0.45f);
    }

    private void mostrar(Resultado<ResumenGenerador.Resumen> r) {
        if (r.estado != Resultado.Estado.EXITO || r.datos == null) {
            b.cargando.setVisibility(View.GONE);
            Snackbar.make(b.raiz, r.mensaje == null ? "" : r.mensaje, Snackbar.LENGTH_LONG).show();
            return;
        }
        resumen = r.datos;
        b.encabezado.setText(resumen.titulo.toUpperCase());
        b.titulo.setText(resumen.subtitulo);
        b.generado.setText(resumen.generado);
        b.completitud.setText(getString(R.string.resumen_completitud, resumen.apartadosCompletos));
        b.completitud.setVisibility(View.VISIBLE);
        Animaciones.aparecerEscalando(b.completitud, 100);

        b.cargando.setVisibility(View.GONE);
        b.secciones.removeAllViews();
        LayoutInflater inf = getLayoutInflater();
        int i = 0;
        for (ResumenGenerador.Seccion s : resumen.secciones) {
            ViewSeccionResumenBinding sb = ViewSeccionResumenBinding.inflate(inf, b.secciones, false);
            sb.titulo.setText(s.titulo);
            int[] estilo = estilo(s.id);
            sb.icono.setImageResource(estilo[0]);
            sb.icono.setImageTintList(ContextCompat.getColorStateList(this, estilo[1]));
            sb.insignia.setBackgroundTintList(ContextCompat.getColorStateList(this, estilo[2]));
            for (ResumenGenerador.Linea l : s.lineas) {
                ItemLineaResumenBinding lb = ItemLineaResumenBinding.inflate(inf, sb.lineas, false);
                lb.etiqueta.setText(l.etiqueta);
                lb.valor.setText(l.valor);
                if (l.vacia) {
                    lb.valor.setTypeface(lb.valor.getTypeface(), Typeface.ITALIC);
                    lb.valor.setTextColor(MaterialColors.getColor(lb.valor,
                            com.google.android.material.R.attr.colorOutline));
                }
                sb.lineas.addView(lb.getRoot());
            }
            b.secciones.addView(sb.getRoot());
            Animaciones.entrar(sb.getRoot(), 60L * i++);
        }
        b.pie.setVisibility(View.VISIBLE);
        actualizarBotones();
    }

    private static int[] estilo(String id) {
        switch (id) {
            case "paciente": return new int[]{R.drawable.ic_person, R.color.accent_agenda, R.color.accent_agenda_bg};
            case "consulta": return new int[]{R.drawable.ic_medical, R.color.accent_estudios, R.color.accent_estudios_bg};
            case "preparacion": return new int[]{R.drawable.ic_edit_note, R.color.accent_prep, R.color.accent_prep_bg};
            case "estudios": return new int[]{R.drawable.ic_study, R.color.accent_estudios, R.color.accent_estudios_bg};
            default: return new int[]{R.drawable.ic_history, R.color.accent_historial, R.color.accent_historial_bg};
        }
    }

    /** Copiar o compartir: exige consentimiento y lo registra antes de enviar (RF-07). */
    private void accion(boolean compartir) {
        if (resumen == null) return;
        if (!b.cbConsentimiento.isChecked()) {
            Animaciones.sacudir(b.cbConsentimiento);
            Animaciones.vibrarError(b.cbConsentimiento);
            Snackbar.make(b.raiz, R.string.resumen_requiere_consentimiento, Snackbar.LENGTH_SHORT)
                    .setAnchorView(b.barra).show();
            return;
        }
        String destinatario = b.etDestinatario.getText() == null ? null : b.etDestinatario.getText().toString();
        String texto = resumen.texto;
        repo.registrarConsentimiento(citaId, destinatario, r -> {
            if (r.estado != Resultado.Estado.EXITO) {
                Snackbar.make(b.raiz, r.mensaje == null ? "" : r.mensaje, Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (compartir) {
                Intent envio = new Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, resumen.titulo + " · " + resumen.subtitulo)
                        .putExtra(Intent.EXTRA_TEXT, texto);
                startActivity(Intent.createChooser(envio, getString(R.string.resumen_compartir_titulo)));
            } else {
                ClipboardManager cm = getSystemService(ClipboardManager.class);
                ClipData clip = ClipData.newPlainText(resumen.titulo, texto);
                if (Build.VERSION.SDK_INT >= 24) {
                    android.os.PersistableBundle extras = new android.os.PersistableBundle();
                    // Marca el contenido como sensible para que no se muestre en la vista previa (Android 13+).
                    extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
                    clip.getDescription().setExtras(extras);
                }
                cm.setPrimaryClip(clip);
                Animaciones.vibrarExito(b.btnCopiar);
                Snackbar.make(b.raiz, R.string.resumen_copiado, Snackbar.LENGTH_SHORT).setAnchorView(b.barra).show();
            }
        });
    }

    private void cerrar() {
        finish();
        Ventana.animacionCierre(this, R.anim.deslizar_volver, R.anim.deslizar_salir, true);
    }

}

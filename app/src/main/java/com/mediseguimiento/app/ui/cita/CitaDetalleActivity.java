package com.mediseguimiento.app.ui.cita;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.core.Resultado;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.core.recordatorios.Recordatorios;
import com.mediseguimiento.app.data.repo.CitaRepository;
import com.mediseguimiento.app.data.repo.PreparacionRepository;
import com.mediseguimiento.app.databinding.ActivityCitaDetalleBinding;
import com.mediseguimiento.app.ui.preparacion.PreparacionActivity;
import com.mediseguimiento.app.ui.resumen.ResumenActivity;
import com.mediseguimiento.app.databinding.ItemFilaInfoBinding;
import com.mediseguimiento.app.ui.common.Acentos;
import com.mediseguimiento.app.ui.common.Animaciones;
import com.mediseguimiento.app.ui.common.Ventana;

/** Detalle de una cita con acciones según su estado (RF-02, RF-08). */
public class CitaDetalleActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "cita_id";
    public static final String RESULTADO_MENSAJE = "mensaje";

    private ActivityCitaDetalleBinding b;
    private CitaDetalleViewModel vm;
    @Nullable private Cita actual;
    private boolean eliminando;

    /** Formulario y preparación devuelven un mensaje que se confirma con Snackbar. */
    private final ActivityResultLauncher<Intent> editar = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                    snack(r.getData().getStringExtra(CitaFormActivity.RESULTADO_MENSAJE), null, null);
                }
            });
    private int recordatorioMin = Cita.RECORDATORIO_NINGUNO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        Ventana.animacionCierre(this, R.anim.deslizar_volver, R.anim.deslizar_salir, false);
        b = ActivityCitaDetalleBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(CitaDetalleViewModel.class);

        Ventana.iconosBarraEstado(this, true);
        Ventana.aplicarPaddingSuperior(b.hero);
        Ventana.aplicarPaddingInferior(b.barraAcciones);

        b.btnVolver.setOnClickListener(v -> cerrar());
        b.btnMenu.setOnClickListener(this::menu);
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { cerrar(); }
        });

        long id = getIntent().getLongExtra(EXTRA_ID, -1);
        new PreparacionRepository(this).observar(id).observe(this, p -> {
            int n = PreparacionRepository.completados(p);
            b.prepAnillo.setProgressCompat(n, true);
            b.prepNumero.setText(getString(R.string.prep_progreso_corto, n, PreparacionRepository.APARTADOS));
            b.prepTexto.setText(getString(R.string.prep_progreso, n, PreparacionRepository.APARTADOS));
            b.btnPreparar.setText(n == 0 ? R.string.preparar
                    : n < PreparacionRepository.APARTADOS ? R.string.continuar_preparacion : R.string.editar_preparacion);
            Animaciones.alinearIcono(b.btnPreparar);
        });
        b.btnPreparar.setOnClickListener(v -> editar.launch(
                new Intent(this, PreparacionActivity.class).putExtra(PreparacionActivity.EXTRA_CITA, id),
                ActivityOptionsCompat.makeCustomAnimation(this, R.anim.hoja_entrar, R.anim.quieto)));
        b.btnResumen.setOnClickListener(v -> startActivity(
                new Intent(this, ResumenActivity.class).putExtra(ResumenActivity.EXTRA_CITA, id),
                ActivityOptionsCompat.makeCustomAnimation(this, R.anim.deslizar_entrar, R.anim.deslizar_salir_atras).toBundle()));
        Animaciones.alinearIcono(b.btnResumen);
        vm.cita(id).observe(this, c -> {
            if (c == null) {
                if (!eliminando) cerrar();
                return;
            }
            boolean primera = actual == null;
            actual = c;
            pintar(c);
            new CitaRepository(this).obtener(c.id, r -> {
                if (r.datos != null && actual != null) {
                    recordatorioMin = r.datos.recordatorioMin;
                    pintar(actual);
                }
            });
            if (primera && savedInstanceState == null) {
                Animaciones.entrar(b.contenido, 120);
            }
        });
    }

    private void pintar(Cita c) {
        long ahora = System.currentTimeMillis();
        boolean h24 = DateFormat.is24HourFormat(this);
        boolean cancelada = Cita.ESTADO_CANCELADA.equals(c.estado);
        boolean completada = Cita.ESTADO_COMPLETADA.equals(c.estado);
        boolean pasada = c.fechaHora < Fechas.inicioDelDia(ahora);

        b.especialidad.setText(c.especialidad);
        b.subtitulo.setText(TextUtils.isEmpty(c.medico) ? getString(R.string.aviso_titulo) : c.medico);
        b.subtitulo.setVisibility(TextUtils.isEmpty(c.medico) ? View.GONE : View.VISIBLE);
        b.fecha.setText(Fechas.fechaLarga(c.fechaHora, ahora));
        b.hora.setText(Fechas.horaCompleta(c.fechaHora, h24));

        String estado = cancelada ? getString(R.string.estado_cancelada)
                : completada ? getString(R.string.estado_completada)
                : getString(R.string.estado_programada) + " · " + Fechas.cuentaRegresiva(c.fechaHora, ahora);
        b.estado.setText(estado);

        int acento = Acentos.color(c.especialidad), fondo = Acentos.fondo(c.especialidad);
        fila(b.filaMedico, R.drawable.ic_person, R.string.detalle_medico, c.medico, acento, fondo);
        fila(b.filaLugar, R.drawable.ic_place, R.string.detalle_lugar, c.lugar, acento, fondo);
        fila(b.filaNotas, R.drawable.ic_edit_note, R.string.detalle_notas, c.notas, acento, fondo);
        boolean conAviso = !cancelada && !completada && recordatorioMin >= 0;
        fila(b.filaRecordatorio, R.drawable.ic_bell, R.string.detalle_recordatorio,
                conAviso ? Recordatorios.etiqueta(recordatorioMin) : null, acento, fondo);
        boolean sinDatos = TextUtils.isEmpty(c.medico) && TextUtils.isEmpty(c.lugar)
                && TextUtils.isEmpty(c.notas) && !conAviso;
        b.sinDatos.setVisibility(sinDatos ? View.VISIBLE : View.GONE);
        b.tarjetaPrep.setVisibility(cancelada ? View.GONE : View.VISIBLE);

        configurarAcciones(c, cancelada, completada, pasada);
    }

    private void fila(ItemFilaInfoBinding f, @DrawableRes int icono, @StringRes int etiqueta,
                      @Nullable String valor, int color, int fondo) {
        boolean hay = !TextUtils.isEmpty(valor);
        f.getRoot().setVisibility(hay ? View.VISIBLE : View.GONE);
        if (!hay) return;
        f.icono.setImageResource(icono);
        f.icono.setImageTintList(ContextCompat.getColorStateList(this, color));
        f.insignia.setBackgroundTintList(ContextCompat.getColorStateList(this, fondo));
        f.titulo.setText(etiqueta);
        f.titulo.setTextAppearance(R.style.TextAppearance_Medi_Overline);
        f.titulo.setTextColor(MaterialColors.getColor(f.titulo, com.google.android.material.R.attr.colorOnSurfaceVariant));
        f.cuerpo.setText(valor);
        f.cuerpo.setTextAppearance(R.style.TextAppearance_Medi_Body);
        f.cuerpo.setTextColor(MaterialColors.getColor(f.cuerpo, com.google.android.material.R.attr.colorOnSurface));
    }

    /** Las acciones disponibles dependen del estado de la cita. */
    private void configurarAcciones(Cita c, boolean cancelada, boolean completada, boolean pasada) {
        int error = MaterialColors.getColor(b.btnSecundario, androidx.appcompat.R.attr.colorError);
        int normal = MaterialColors.getColor(b.btnSecundario, com.google.android.material.R.attr.colorOnSurface);
        b.btnSecundario.setVisibility(View.VISIBLE);
        if (cancelada) {
            boton(b.btnSecundario, R.string.eliminar, R.drawable.ic_delete, error, v -> confirmarEliminar());
            boton(b.btnPrincipal, R.string.reprogramar, R.drawable.ic_replay, 0,
                    v -> cambiar(Cita.ESTADO_PROGRAMADA, R.string.cita_reactivada, null));
        } else if (completada) {
            b.btnSecundario.setVisibility(View.GONE);
            boton(b.btnPrincipal, R.string.editar, R.drawable.ic_edit, 0, v -> abrirEdicion());
        } else if (pasada) {
            boton(b.btnSecundario, R.string.editar, R.drawable.ic_edit, normal, v -> abrirEdicion());
            boton(b.btnPrincipal, R.string.marcar_completada, R.drawable.ic_check, 0,
                    v -> cambiar(Cita.ESTADO_COMPLETADA, R.string.cita_completada, null));
        } else {
            boton(b.btnSecundario, R.string.cancelar_cita, R.drawable.ic_event_busy, error, v -> confirmarCancelar());
            boton(b.btnPrincipal, R.string.editar, R.drawable.ic_edit, 0, v -> abrirEdicion());
        }
        Animaciones.alinearIcono(b.btnPrincipal);
        Animaciones.alinearIcono(b.btnSecundario);
    }

    private void boton(com.google.android.material.button.MaterialButton btn, @StringRes int texto,
                       @DrawableRes int icono, int color, View.OnClickListener l) {
        btn.setText(texto);
        btn.setIconResource(icono);
        if (color != 0) {
            btn.setTextColor(color);
            btn.setIconTint(ColorStateList.valueOf(color));
        }
        btn.setOnClickListener(l);
    }

    // ================= Acciones =================

    private void menu(View ancla) {
        PopupMenu m = new PopupMenu(this, ancla);
        m.inflate(R.menu.menu_cita);
        m.setForceShowIcon(true);
        m.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.accion_editar) abrirEdicion();
            else if (item.getItemId() == R.id.accion_eliminar) confirmarEliminar();
            return true;
        });
        m.show();
    }

    private void abrirEdicion() {
        if (actual == null) return;
        Intent i = new Intent(this, CitaFormActivity.class).putExtra(CitaFormActivity.EXTRA_ID, actual.id);
        editar.launch(i, ActivityOptionsCompat.makeCustomAnimation(this, R.anim.hoja_entrar, R.anim.quieto));
    }

    private void confirmarCancelar() {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Medi_Dialogo_Destructivo)
                .setIcon(R.drawable.ic_event_busy)
                .setTitle(R.string.cancelar_cita_titulo)
                .setMessage(R.string.cancelar_cita_cuerpo)
                .setNegativeButton(R.string.no_cancelar, null)
                .setPositiveButton(R.string.cancelar_cita, (d, w) -> {
                    String anterior = actual != null ? actual.estado : Cita.ESTADO_PROGRAMADA;
                    cambiar(Cita.ESTADO_CANCELADA, R.string.cita_cancelada, anterior);
                })
                .show();
    }

    private void confirmarEliminar() {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Medi_Dialogo_Destructivo)
                .setIcon(R.drawable.ic_delete)
                .setTitle(R.string.eliminar_cita_titulo)
                .setMessage(R.string.eliminar_cita_cuerpo)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.eliminar, (d, w) -> {
                    eliminando = true;
                    vm.eliminar(r -> {
                        if (r.estado == Resultado.Estado.EXITO) {
                            setResult(RESULT_OK, new Intent().putExtra(RESULTADO_MENSAJE, getString(R.string.cita_eliminada)));
                            cerrar();
                        } else {
                            eliminando = false;
                            snack(r.mensaje, null, null);
                        }
                    });
                })
                .show();
    }

    /** Cambia el estado; si se indica un estado anterior, ofrece "Deshacer". */
    private void cambiar(String estado, @StringRes int mensaje, @Nullable String anterior) {
        vm.cambiarEstado(estado, r -> {
            if (r.estado == Resultado.Estado.EXITO) {
                Animaciones.vibrarExito(b.btnPrincipal);
                snack(getString(mensaje), anterior == null ? null : getString(R.string.deshacer),
                        anterior == null ? null : v -> vm.cambiarEstado(anterior, x -> { }));
            } else {
                snack(r.mensaje, null, null);
            }
        });
    }

    private void snack(@Nullable String texto, @Nullable String accion, @Nullable View.OnClickListener l) {
        if (texto == null) return;
        Snackbar s = Snackbar.make(b.raiz, texto, Snackbar.LENGTH_LONG).setAnchorView(b.barraAcciones);
        if (accion != null && l != null) {
            // Con "Deshacer" se da más tiempo para reaccionar.
            s.setAction(accion, l).setDuration(5000);
        }
        s.show();
    }

    private void cerrar() {
        finish();
        Ventana.animacionCierre(this, R.anim.deslizar_volver, R.anim.deslizar_salir, true);
    }
}

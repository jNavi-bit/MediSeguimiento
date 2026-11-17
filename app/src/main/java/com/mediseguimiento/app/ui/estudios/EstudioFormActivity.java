package com.mediseguimiento.app.ui.estudios;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.core.Resultado;
import com.mediseguimiento.app.core.Validaciones;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.db.entity.Estudio;
import com.mediseguimiento.app.data.repo.EstudioRepository;
import com.mediseguimiento.app.databinding.ActivityEstudioFormBinding;
import com.mediseguimiento.app.ui.common.Animaciones;
import com.mediseguimiento.app.ui.common.Botones;
import com.mediseguimiento.app.ui.common.Ventana;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Registrar o editar un estudio y su resultado (RF-04, RF-08). */
public class EstudioFormActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "estudio_id";
    public static final String EXTRA_CITA = "cita_id";
    public static final String RESULTADO_MENSAJE = "mensaje";

    private ActivityEstudioFormBinding b;
    private EstudioRepository repo;
    private boolean nuevo;
    private long estudioId;
    private LocalDate fecha = LocalDate.now(Fechas.zona());
    @Nullable private Long citaVinculada;
    private final List<Cita> citas = new ArrayList<>();
    private String inicial = "";
    private boolean ocupado;
    private boolean intentado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        Ventana.animacionCierre(this, R.anim.quieto, R.anim.hoja_salir, false);
        b = ActivityEstudioFormBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        repo = new EstudioRepository(this);
        estudioId = getIntent().getLongExtra(EXTRA_ID, -1);
        nuevo = estudioId < 0;
        long citaExtra = getIntent().getLongExtra(EXTRA_CITA, -1);
        if (citaExtra >= 0) citaVinculada = citaExtra;

        Ventana.iconosBarraEstado(this, false);
        Ventana.aplicarPaddingSuperior(b.barraSuperior);
        Ventana.aplicarPaddingInferior(b.barraInferior);

        int titulo = nuevo ? R.string.nuevo_estudio : R.string.editar_estudio;
        b.titulo.setText(titulo);
        b.tituloBarra.setText(titulo);
        b.btnGuardar.setText(nuevo ? R.string.guardar_estudio : R.string.guardar_cambios);
        b.btnEliminar.setVisibility(nuevo ? View.GONE : View.VISIBLE);
        b.etNombre.setSimpleItems(getResources().getStringArray(R.array.estudios_comunes));

        b.scroll.setOnScrollChangeListener((View.OnScrollChangeListener) (v, x, y, ox, oy) ->
                b.tituloBarra.setAlpha(Math.min(1f, y / (float) Math.max(1, b.titulo.getBottom()))));
        b.etFecha.setOnClickListener(v -> elegirFecha());
        b.btnCerrar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        b.btnGuardar.setOnClickListener(v -> guardar());
        b.btnEliminar.setOnClickListener(v -> confirmarEliminar());
        b.etCita.setOnItemClickListener((p, v, pos, id) -> citaVinculada = pos == 0 ? null : citas.get(pos - 1).id);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { salir(); }
        });
        android.text.TextWatcher revalidar = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void afterTextChanged(android.text.Editable s) { if (intentado) validar(); }
        };
        b.etNombre.addTextChangedListener(revalidar);

        pintarFecha();
        repo.citasVinculables(r -> {
            citas.clear();
            if (r.datos != null) citas.addAll(r.datos);
            pintarCitas();
            if (nuevo) inicial = instantanea();
        });
        if (!nuevo) {
            repo.obtener(estudioId, r -> {
                Estudio e = r.datos;
                if (e == null) { finish(); return; }
                b.etNombre.setText(e.nombre, false);
                b.etLaboratorio.setText(e.laboratorio);
                b.etResultado.setText(e.resultado);
                b.etNotas.setText(e.notas);
                fecha = Fechas.aFecha(e.fecha);
                citaVinculada = e.citaId;
                pintarFecha();
                pintarCitas();
                inicial = instantanea();
            });
        }
        if (savedInstanceState == null) {
            Animaciones.entrar(b.tarjetaQue, 60);
            Animaciones.entrar(b.tarjetaResultados, 140);
            Animaciones.entrar(b.tarjetaVincular, 220);
        }
    }

    private void pintarFecha() {
        b.etFecha.setText(Fechas.fechaLarga(Fechas.aMillis(fecha, LocalTime.NOON), System.currentTimeMillis()));
    }

    private void pintarCitas() {
        List<String> opciones = new ArrayList<>();
        opciones.add(getString(R.string.sin_vincular));
        String sel = opciones.get(0);
        for (Cita c : citas) {
            String t = c.especialidad + " · " + Fechas.fechaCorta(c.fechaHora);
            opciones.add(t);
            if (citaVinculada != null && c.id == citaVinculada) sel = t;
        }
        b.etCita.setSimpleItems(opciones.toArray(new String[0]));
        b.etCita.setText(sel, false);
    }

    private void elegirFecha() {
        MaterialDatePicker<Long> p = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.campo_fecha_estudio)
                .setSelection(fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setEnd(LocalDate.now(Fechas.zona()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                        .setValidator(DateValidatorPointBackward.now()).build())
                .build();
        p.addOnPositiveButtonClickListener(sel -> {
            fecha = Instant.ofEpochMilli(sel).atZone(ZoneOffset.UTC).toLocalDate();
            pintarFecha();
            if (intentado) validar();
        });
        p.show(getSupportFragmentManager(), "fecha");
    }

    private static String texto(TextView tv) {
        return tv.getText() == null ? "" : tv.getText().toString().trim();
    }

    @Nullable
    private static String opcional(TextView tv) {
        String t = texto(tv);
        return t.isEmpty() ? null : t;
    }

    private boolean validar() {
        String eNombre = texto(b.etNombre).isEmpty() ? getString(R.string.error_estudio_nombre) : null;
        String eFecha = fecha.isAfter(LocalDate.now(Fechas.zona())) ? getString(R.string.error_estudio_futuro) : null;
        String eRes = Validaciones.opcional(texto(b.etResultado), 1000);
        marcar(b.tilNombre, eNombre);
        marcar(b.tilFecha, eFecha);
        marcar(b.tilResultado, eRes);
        if (eNombre != null) b.etNombre.requestFocus();
        return eNombre == null && eFecha == null && eRes == null;
    }

    private static void marcar(TextInputLayout t, @Nullable String e) {
        t.setError(e);
        t.setErrorEnabled(e != null);
    }

    private void guardar() {
        if (ocupado) return;
        intentado = true;
        if (!validar()) {
            Animaciones.vibrarError(b.btnGuardar);
            Animaciones.sacudir(b.btnGuardar);
            return;
        }
        ocupado = true;
        WindowCompat.getInsetsController(getWindow(), b.getRoot()).hide(WindowInsetsCompat.Type.ime());
        Botones.cargando(b.btnGuardar, getString(R.string.guardando));
        Estudio e = new Estudio();
        if (!nuevo) e.id = estudioId;
        e.nombre = texto(b.etNombre);
        e.laboratorio = opcional(b.etLaboratorio);
        e.resultado = opcional(b.etResultado);
        e.notas = opcional(b.etNotas);
        e.fecha = Fechas.aMillis(fecha, LocalTime.NOON);
        e.citaId = citaVinculada;
        repo.guardar(e, r -> {
            ocupado = false;
            Botones.normal(b.btnGuardar, getString(nuevo ? R.string.guardar_estudio : R.string.guardar_cambios), 0);
            if (r.estado == Resultado.Estado.EXITO) {
                Animaciones.vibrarExito(b.btnGuardar);
                terminar(getString(nuevo ? R.string.estudio_guardado : R.string.cita_actualizada));
            } else {
                new MaterialAlertDialogBuilder(this).setMessage(r.mensaje)
                        .setPositiveButton(android.R.string.ok, null).show();
            }
        });
    }

    private void confirmarEliminar() {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Medi_Dialogo_Destructivo)
                .setIcon(R.drawable.ic_delete)
                .setTitle(R.string.eliminar_estudio_titulo)
                .setMessage(R.string.eliminar_estudio_cuerpo)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.eliminar, (d, w) -> repo.eliminar(estudioId, r -> {
                    if (r.estado == Resultado.Estado.EXITO) terminar(getString(R.string.estudio_eliminado));
                }))
                .show();
    }

    private String instantanea() {
        return texto(b.etNombre) + "|" + texto(b.etLaboratorio) + "|" + texto(b.etResultado) + "|"
                + texto(b.etNotas) + "|" + fecha + "|" + citaVinculada;
    }

    private void salir() {
        if (ocupado) return;
        if (!TextUtils.equals(instantanea(), inicial)) {
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

    private void terminar(String mensaje) {
        setResult(RESULT_OK, new Intent().putExtra(RESULTADO_MENSAJE, mensaje));
        cerrar();
    }

    private void cerrar() {
        finish();
        Ventana.animacionCierre(this, R.anim.quieto, R.anim.hoja_salir, true);
    }
}

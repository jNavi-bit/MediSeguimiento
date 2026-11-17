package com.mediseguimiento.app.ui.cita;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.core.Resultado;
import com.mediseguimiento.app.core.Validaciones;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.repo.CitaRepository;
import com.mediseguimiento.app.databinding.ActivityCitaFormBinding;
import com.mediseguimiento.app.ui.common.Animaciones;
import com.mediseguimiento.app.ui.common.Botones;
import com.mediseguimiento.app.ui.common.Ventana;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

/** Crear o editar una cita de la agenda personal (RF-02, RNF-04). */
public class CitaFormActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "cita_id";
    public static final String EXTRA_DIA = "dia";
    /** Minutos desde medianoche: hora propuesta al tocar un hueco del calendario. */
    public static final String EXTRA_MINUTO = "minuto";
    public static final String RESULTADO_MENSAJE = "mensaje";

    private ActivityCitaFormBinding b;
    private CitaFormViewModel vm;
    private boolean nueva;
    private boolean intentado;
    private boolean h24;
    /** Evita pedir permisos cuando la selección del chip la hace el código, no el usuario. */
    private boolean marcandoChip;

    /** Permiso de notificaciones (Android 13+), solicitado solo al elegir un aviso. */
    private final androidx.activity.result.ActivityResultLauncher<String> permiso =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                    concedido -> {
                        if (!concedido) {
                            com.google.android.material.snackbar.Snackbar.make(b.getRoot(),
                                    R.string.recordatorio_permiso_negado,
                                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                                    .setAnchorView(b.barraInferior).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        Ventana.animacionCierre(this, R.anim.quieto, R.anim.hoja_salir, false);
        b = ActivityCitaFormBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(CitaFormViewModel.class);
        h24 = DateFormat.is24HourFormat(this);

        long id = getIntent().getLongExtra(EXTRA_ID, -1);
        nueva = id < 0;

        Ventana.iconosBarraEstado(this, false);
        Ventana.aplicarPaddingSuperior(b.barraSuperior);
        Ventana.aplicarPaddingInferior(b.barraInferior);

        int titulo = nueva ? R.string.nueva_cita : R.string.editar_cita;
        b.titulo.setText(titulo);
        b.tituloBarra.setText(titulo);
        b.btnGuardar.setText(nueva ? R.string.guardar_cita : R.string.guardar_cambios);
        b.etEspecialidad.setSimpleItems(getResources().getStringArray(R.array.especialidades));

        // El título pequeño aparece en la barra al desplazar, como en las apps de Google.
        b.scroll.setOnScrollChangeListener((View.OnScrollChangeListener) (v, x, y, ox, oy) -> {
            float p = Math.min(1f, y / (float) Math.max(1, b.titulo.getBottom()));
            b.tituloBarra.setAlpha(p);
        });

        b.etFecha.setOnClickListener(v -> elegirFecha());
        b.etHora.setOnClickListener(v -> elegirHora());
        b.chipManana.setOnClickListener(v -> fijarFecha(LocalDate.now(Fechas.zona()).plusDays(1)));
        b.chipSemana.setOnClickListener(v -> fijarFecha(LocalDate.now(Fechas.zona()).plusWeeks(1)));
        b.chipMes.setOnClickListener(v -> fijarFecha(LocalDate.now(Fechas.zona()).plusMonths(1)));

        b.grupoDuracion.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            int chip = ids.get(0);
            vm.duracionMin = chip == R.id.dur_15 ? 15 : chip == R.id.dur_45 ? 45 : chip == R.id.dur_60 ? 60
                    : chip == R.id.dur_90 ? 90 : chip == R.id.dur_120 ? 120 : 30;
        });

        b.grupoRecordatorio.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            int chip = ids.get(0);
            vm.recordatorioMin = chip == R.id.rec_15 ? 15 : chip == R.id.rec_60 ? 60
                    : chip == R.id.rec_dia ? 24 * 60 : Cita.RECORDATORIO_NINGUNO;
            explicarRecordatorio();
            if (vm.recordatorioMin >= 0 && !marcandoChip) pedirPermisoSiHaceFalta();
        });

        b.btnCerrar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        b.btnGuardar.setOnClickListener(v -> guardar());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { salir(); }
        });

        TextWatcher revalidar = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void afterTextChanged(Editable s) { if (intentado) validar(false); }
        };
        b.etEspecialidad.addTextChangedListener(revalidar);
        b.etMedico.addTextChangedListener(revalidar);
        b.etLugar.addTextChangedListener(revalidar);
        b.etNotas.addTextChangedListener(revalidar);

        vm.guardado().observe(this, this::render);

        if (nueva) {
            if (!vm.inicializado) inicializarNueva();
            pintarFechaHora();
            vm.instantaneaInicial = vm.instantaneaInicial == null ? instantanea() : vm.instantaneaInicial;
        } else {
            vm.cargada().observe(this, c -> {
                if (c == null) {
                    finish();
                    return;
                }
                if (!vm.inicializado) {
                    rellenar(c);
                    vm.instantaneaInicial = instantanea();
                    vm.inicializado = true;
                }
                pintarFechaHora();
            });
            if (vm.original() == null) vm.cargar(id);
        }

        if (savedInstanceState == null) {
            Animaciones.entrar(b.tarjetaQuien, 60);
            Animaciones.entrar(b.tarjetaCuando, 140);
            Animaciones.entrar(b.tarjetaRecordatorio, 220);
            Animaciones.entrar(b.tarjetaNotas, 300);
        }
    }

    private void inicializarNueva() {
        long diaExtra = getIntent().getLongExtra(EXTRA_DIA, Long.MIN_VALUE);
        LocalDate hoy = LocalDate.now(Fechas.zona());
        vm.fecha = diaExtra != Long.MIN_VALUE ? LocalDate.ofEpochDay(diaExtra) : hoy.plusDays(1);
        int minuto = getIntent().getIntExtra(EXTRA_MINUTO, -1);
        vm.hora = minuto >= 0 ? LocalTime.of(minuto / 60, minuto % 60) : LocalTime.of(9, 0);
        // Si la fecha es hoy y las 9:00 ya pasaron, se propone la siguiente hora en punto.
        if (vm.fecha.equals(hoy) && minuto < 0) {
            LocalTime siguiente = LocalTime.now(Fechas.zona()).plusHours(1).withMinute(0).withSecond(0).withNano(0);
            if (siguiente.isAfter(vm.hora)) vm.hora = siguiente;
        }
        // Aviso por defecto: la mayor anticipación que todavía quede en el futuro.
        long cita = Fechas.aMillis(vm.fecha, vm.hora), ahora = System.currentTimeMillis();
        vm.recordatorioMin = Cita.RECORDATORIO_NINGUNO;
        for (int min : new int[]{24 * 60, 60, 15}) {
            if (com.mediseguimiento.app.core.recordatorios.Recordatorios.disparo(cita, min) > ahora) {
                vm.recordatorioMin = min;
                break;
            }
        }
        vm.inicializado = true;
    }

    private void rellenar(Cita c) {
        b.etEspecialidad.setText(c.especialidad, false);
        b.etMedico.setText(c.medico);
        b.etLugar.setText(c.lugar);
        b.etNotas.setText(c.notas);
        vm.fecha = Fechas.aFecha(c.fechaHora);
        vm.hora = Fechas.aLocal(c.fechaHora).toLocalTime().withSecond(0).withNano(0);
        vm.duracionMin = c.duracionMin;
        vm.recordatorioMin = c.recordatorioMin == Cita.RECORDATORIO_SIN_CAMBIO
                ? Cita.RECORDATORIO_NINGUNO : c.recordatorioMin;
    }

    // ================= Fecha y hora =================

    private void elegirFecha() {
        long seleccion = vm.fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.seleccionar_fecha)
                .setSelection(seleccion);
        if (nueva) {
            long hoyUtc = LocalDate.now(Fechas.zona()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
            builder.setCalendarConstraints(new CalendarConstraints.Builder()
                    .setStart(hoyUtc)
                    .setValidator(DateValidatorPointForward.from(hoyUtc)).build());
        }
        MaterialDatePicker<Long> p = builder.build();
        p.addOnPositiveButtonClickListener(sel ->
                fijarFecha(Instant.ofEpochMilli(sel).atZone(ZoneOffset.UTC).toLocalDate()));
        p.show(getSupportFragmentManager(), "fecha");
    }

    private void elegirHora() {
        MaterialTimePicker p = new MaterialTimePicker.Builder()
                .setTimeFormat(h24 ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(vm.hora.getHour())
                .setMinute(vm.hora.getMinute())
                .setTitleText(R.string.seleccionar_hora)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build();
        p.addOnPositiveButtonClickListener(v -> {
            vm.hora = LocalTime.of(p.getHour(), p.getMinute());
            pintarFechaHora();
            resaltar(b.tilHora);
        });
        p.show(getSupportFragmentManager(), "hora");
    }

    private void fijarFecha(LocalDate d) {
        Animaciones.vibrarToque(b.tilFecha);
        vm.fecha = d;
        pintarFechaHora();
        resaltar(b.tilFecha);
    }

    private void pintarFechaHora() {
        if (vm.fecha == null || vm.hora == null) return;
        long ms = Fechas.aMillis(vm.fecha, vm.hora);
        b.etFecha.setText(Fechas.fechaCorta(ms) + (vm.fecha.getYear() != LocalDate.now().getYear()
                ? " " + vm.fecha.getYear() : ""));
        b.etHora.setText(Fechas.horaCompleta(ms, h24));
        marcarRecordatorio();
        marcarDuracion();
        explicarRecordatorio();
        if (intentado) validar(false);
    }

    private void marcarDuracion() {
        int m = vm.duracionMin;
        int chip = m <= 15 ? R.id.dur_15 : m <= 30 ? R.id.dur_30 : m <= 45 ? R.id.dur_45
                : m <= 60 ? R.id.dur_60 : m <= 90 ? R.id.dur_90 : R.id.dur_120;
        if (b.grupoDuracion.getCheckedChipId() != chip) b.grupoDuracion.check(chip);
    }

    private void marcarRecordatorio() {
        int chip = vm.recordatorioMin == 15 ? R.id.rec_15 : vm.recordatorioMin == 60 ? R.id.rec_60
                : vm.recordatorioMin >= 24 * 60 ? R.id.rec_dia : R.id.rec_ninguno;
        if (b.grupoRecordatorio.getCheckedChipId() != chip) {
            marcandoChip = true;
            b.grupoRecordatorio.check(chip);
            marcandoChip = false;
        }
    }

    /** Explica cuándo llegará el aviso, o por qué no llegará. */
    private void explicarRecordatorio() {
        if (vm.fecha == null || vm.hora == null) return;
        if (vm.recordatorioMin < 0) {
            b.recExplicacion.setText(R.string.recordatorio_sin);
            return;
        }
        long disparo = com.mediseguimiento.app.core.recordatorios.Recordatorios.disparo(fechaHora(), vm.recordatorioMin);
        if (disparo <= System.currentTimeMillis()) {
            b.recExplicacion.setText(R.string.recordatorio_pasado);
        } else {
            // "a. m." ya termina en punto: se evita el doble punto al final.
            b.recExplicacion.setText(getString(R.string.recordatorio_aviso,
                    Fechas.fechaLarga(disparo, System.currentTimeMillis()).toLowerCase(Fechas.ES),
                    Fechas.horaCompleta(disparo, h24)).replace("..", "."));
        }
    }

    private void pedirPermisoSiHaceFalta() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permiso.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private static void resaltar(View v) {
        v.animate().scaleX(1.03f).scaleY(1.03f).setDuration(110)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(180).start()).start();
    }

    // ================= Validación y guardado =================

    private long fechaHora() { return Fechas.aMillis(vm.fecha, vm.hora); }

    private boolean validar(boolean enfocar) {
        View primero = null;
        primero = marcar(b.tilEspecialidad, Validaciones.especialidad(texto(b.etEspecialidad)), primero);
        primero = marcar(b.tilMedico, Validaciones.opcional(texto(b.etMedico), 60), primero);
        primero = marcar(b.tilLugar, Validaciones.opcional(texto(b.etLugar), 80), primero);
        primero = marcar(b.tilNotas, Validaciones.opcional(texto(b.etNotas), 500), primero);
        String errFecha = Validaciones.fechaCita(fechaHora(), System.currentTimeMillis(), nueva);
        mostrarErrorFecha(errFecha);
        if (errFecha != null && primero == null) primero = b.tilFecha;
        if (enfocar && primero != null) {
            View destino = primero;
            b.scroll.post(() -> b.scroll.smoothScrollTo(0, Math.max(0, destino.getTop() - 48)));
            if (primero instanceof TextInputLayout && ((TextInputLayout) primero).getEditText() != null
                    && ((TextInputLayout) primero).getEditText().isFocusable()) {
                ((TextInputLayout) primero).getEditText().requestFocus();
            }
        }
        return primero == null;
    }

    private void mostrarErrorFecha(@Nullable String error) {
        boolean visible = error != null;
        boolean antes = b.errorFecha.getVisibility() == View.VISIBLE;
        if (visible != antes) TransitionManager.beginDelayedTransition(b.tarjetaCuando, new AutoTransition().setDuration(200));
        b.errorFecha.setText(error);
        b.errorFecha.setVisibility(visible ? View.VISIBLE : View.GONE);
        int color = visible
                ? com.google.android.material.color.MaterialColors.getColor(b.tilFecha, androidx.appcompat.R.attr.colorError)
                : 0;
        for (TextInputLayout t : new TextInputLayout[]{b.tilFecha, b.tilHora}) {
            if (visible) t.setBoxStrokeColorStateList(android.content.res.ColorStateList.valueOf(color));
            else t.setBoxStrokeColorStateList(getColorStateList(R.color.text_input_stroke));
        }
    }

    @Nullable
    private static View marcar(TextInputLayout til, @Nullable String error, @Nullable View primero) {
        til.setError(error);
        til.setErrorEnabled(error != null);
        return primero == null && error != null ? til : primero;
    }

    private static String texto(TextView tv) {
        return tv.getText() == null ? "" : tv.getText().toString().trim();
    }

    @Nullable
    private static String opcional(TextView tv) {
        String t = texto(tv);
        return t.isEmpty() ? null : t;
    }

    private void guardar() {
        if (vm.ocupado()) return;
        intentado = true;
        if (!validar(true)) {
            Animaciones.vibrarError(b.btnGuardar);
            Animaciones.sacudir(b.btnGuardar);
            return;
        }
        WindowCompat.getInsetsController(getWindow(), b.getRoot()).hide(WindowInsetsCompat.Type.ime());
        Cita c = new Cita();
        Cita original = vm.original();
        if (!nueva && original != null) {
            c.id = original.id;
            c.version = original.version;
            c.estado = original.estado;
            c.pacienteId = original.pacienteId;
        }
        c.especialidad = texto(b.etEspecialidad);
        c.medico = opcional(b.etMedico);
        c.lugar = opcional(b.etLugar);
        c.notas = opcional(b.etNotas);
        c.fechaHora = fechaHora();
        c.recordatorioMin = vm.recordatorioMin;
        c.duracionMin = vm.duracionMin;
        if (vm.recordatorioMin >= 0) pedirPermisoSiHaceFalta();
        vm.guardar(c, nueva);
    }

    private void render(@Nullable Resultado<Long> r) {
        if (r == null) return;
        if (r.estado == Resultado.Estado.CARGANDO) {
            Botones.cargando(b.btnGuardar, getString(R.string.guardando));
            return;
        }
        vm.consumir();
        Botones.normal(b.btnGuardar, getString(nueva ? R.string.guardar_cita : R.string.guardar_cambios), 0);
        if (r.estado == Resultado.Estado.EXITO) {
            Animaciones.vibrarExito(b.btnGuardar);
            setResult(RESULT_OK, new Intent().putExtra(RESULTADO_MENSAJE,
                    getString(nueva ? R.string.cita_guardada : R.string.cita_actualizada))
                    .putExtra(EXTRA_ID, r.datos == null ? -1L : r.datos));
            cerrar();
            return;
        }
        Animaciones.vibrarError(b.btnGuardar);
        CitaRepository.Falla f = CitaRepository.falla(r);
        if (f == CitaRepository.Falla.DUPLICADA) {
            mostrarErrorFecha(r.mensaje);
            Animaciones.sacudir(b.tarjetaCuando);
        } else if (f == CitaRepository.Falla.CONFLICTO_VERSION) {
            new MaterialAlertDialogBuilder(this)
                    .setIcon(R.drawable.ic_info)
                    .setTitle(R.string.conflicto_titulo)
                    .setMessage(r.mensaje)
                    .setNegativeButton(R.string.cancelar, null)
                    .setPositiveButton(R.string.recargar, (d, w) -> {
                        vm.inicializado = false;
                        vm.cargar(getIntent().getLongExtra(EXTRA_ID, -1));
                    })
                    .show();
        } else {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(r.mensaje)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    // ================= Salida =================

    private String instantanea() {
        return texto(b.etEspecialidad) + "|" + texto(b.etMedico) + "|" + texto(b.etLugar) + "|"
                + texto(b.etNotas) + "|" + vm.fecha + "|" + vm.hora + "|" + vm.recordatorioMin + "|" + vm.duracionMin;
    }

    private void salir() {
        if (vm.ocupado()) return;
        if (!TextUtils.equals(instantanea(), vm.instantaneaInicial)) {
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

package com.mediseguimiento.app.ui.agenda;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.databinding.FragmentAgendaBinding;
import com.mediseguimiento.app.ui.cita.CitaDetalleActivity;
import com.mediseguimiento.app.ui.cita.CitaFormActivity;
import com.mediseguimiento.app.ui.common.Animaciones;
import com.mediseguimiento.app.ui.common.Ventana;

import java.time.LocalDate;

/** Agenda personal: tira de días, filtros y lista agrupada por fecha (RF-02). */
public class AgendaFragment extends Fragment {

    private FragmentAgendaBinding b;
    private AgendaViewModel vm;
    private CitaAdapter citas;
    private DiaAdapter dias;
    private AgendaViewModel.Filtro filtroPintado;

    /** Al volver del formulario o del detalle se confirma la acción con un Snackbar. */
    private final ActivityResultLauncher<Intent> abrir = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != android.app.Activity.RESULT_OK || r.getData() == null || b == null) return;
                String m = r.getData().getStringExtra(CitaFormActivity.RESULTADO_MENSAJE);
                if (m == null) m = r.getData().getStringExtra(CitaDetalleActivity.RESULTADO_MENSAJE);
                if (m != null) Snackbar.make(b.getRoot(), m, Snackbar.LENGTH_SHORT).setAnchorView(b.fab).show();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentAgendaBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Ventana.aplicarPaddingSuperior(b.encabezado);
        vm = new ViewModelProvider(this).get(AgendaViewModel.class);

        dias = new DiaAdapter(ed -> {
            Animaciones.vibrarToque(b.dias);
            vm.alternarDia(ed);
        });
        b.dias.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        b.dias.setAdapter(dias);
        b.dias.setItemAnimator(null);
        b.dias.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) { actualizarMes(); }
        });

        citas = new CitaAdapter((c, v) -> abrirDetalle(c.id));
        b.lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.lista.setAdapter(citas);
        DefaultItemAnimator anim = new DefaultItemAnimator();
        anim.setSupportsChangeAnimations(false);
        b.lista.setItemAnimator(anim);

        // El botón flotante se contrae al desplazarse hacia abajo y se extiende al subir.
        b.lista.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 6 && b.fab.isExtended()) b.fab.shrink();
                else if (dy < -6 && !b.fab.isExtended()) b.fab.extend();
            }
        });

        b.filtros.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            vm.setFiltro(id == R.id.chip_pasadas ? AgendaViewModel.Filtro.PASADAS
                    : id == R.id.chip_canceladas ? AgendaViewModel.Filtro.CANCELADAS
                    : AgendaViewModel.Filtro.PROXIMAS);
        });

        b.btnVistaLista.setOnClickListener(v -> cambiarVista(false));
        b.btnVistaCalendario.setOnClickListener(v -> cambiarVista(true));
        b.vistaDia.setAlTocar(new VistaDiaView.AlTocar() {
            @Override public void cita(com.mediseguimiento.app.data.db.entity.Cita c) { abrirDetalle(c.id); }
            @Override public void hueco(int minuto) { nuevaCita(minuto); }
        });

        b.fab.setOnClickListener(v -> nuevaCita());
        b.vacio.accion.setOnClickListener(v -> nuevaCita());

        vm.estado().observe(getViewLifecycleOwner(), this::pintar);
        actualizarMes();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            Ventana.iconosBarraEstado(requireActivity(), false);
            vm.refrescar();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            Ventana.iconosBarraEstado(requireActivity(), false);
            vm.refrescar();
        }
    }

    private long diaPintado = Long.MIN_VALUE;

    private void cambiarVista(boolean calendario) {
        if (calendario == vm.esCalendario()) return;
        Animaciones.vibrarToque(b.selectorVista);
        vm.setCalendario(calendario);
    }

    /** Selector tipo píldora: la opción activa lleva fondo claro y el ícono en color primario. */
    private void pintarSelector(boolean calendario) {
        int activo = com.google.android.material.color.MaterialColors.getColor(b.selectorVista, androidx.appcompat.R.attr.colorPrimary);
        int inactivo = com.google.android.material.color.MaterialColors.getColor(b.selectorVista,
                com.google.android.material.R.attr.colorOnSurfaceVariant);
        b.btnVistaLista.setBackgroundResource(calendario ? R.drawable.ripple_pill : R.drawable.bg_segmento_sel);
        b.btnVistaCalendario.setBackgroundResource(calendario ? R.drawable.bg_segmento_sel : R.drawable.ripple_pill);
        b.btnVistaLista.setImageTintList(ColorStateList.valueOf(calendario ? inactivo : activo));
        b.btnVistaCalendario.setImageTintList(ColorStateList.valueOf(calendario ? activo : inactivo));
        b.btnVistaLista.setSelected(!calendario);
        b.btnVistaCalendario.setSelected(calendario);
    }

    private void pintarCalendario(AgendaViewModel.Estado e) {
        LocalDate d = LocalDate.ofEpochDay(e.diaCalendario);
        long ahora = System.currentTimeMillis();
        long ms = Fechas.aMillis(d, java.time.LocalTime.NOON);
        String fecha = Fechas.encabezadoDia(ms, ahora);
        String cuantas = e.delDia.isEmpty() ? getString(R.string.calendario_sin_citas)
                : e.delDia.size() == 1 ? "1 cita" : e.delDia.size() + " citas";
        b.calendarioTitulo.setText(getString(R.string.calendario_dia_resumen, fecha, cuantas));
        b.vistaDia.setDatos(d, e.delDia, android.text.format.DateFormat.is24HourFormat(requireContext()));
        if (diaPintado != e.diaCalendario) {
            diaPintado = e.diaCalendario;
            b.calendarioScroll.post(() -> {
                if (b != null) b.calendarioScroll.smoothScrollTo(0, b.vistaDia.posicionInicial());
            });
        }
    }

    private void pintar(AgendaViewModel.Estado e) {
        pintarSelector(e.calendario);
        if (e.calendario) {
            dias.actualizar(e.diasConCitas, e.diaCalendario);
            b.filtrosScroll.setVisibility(View.GONE);
            b.lista.setVisibility(View.GONE);
            b.vacioContenedor.setVisibility(View.GONE);
            if (b.calendarioContenedor.getVisibility() != View.VISIBLE) {
                b.calendarioContenedor.setVisibility(View.VISIBLE);
                Animaciones.entrar(b.calendarioContenedor, 0);
                diaPintado = Long.MIN_VALUE;
            }
            if (!b.fab.isShown()) b.fab.show();
            pintarCalendario(e);
            actualizarMes();
            return;
        }
        if (b.calendarioContenedor.getVisibility() == View.VISIBLE) {
            b.calendarioContenedor.setVisibility(View.GONE);
            b.filtrosScroll.setVisibility(View.VISIBLE);
            b.lista.setVisibility(View.VISIBLE);
            Animaciones.entrar(b.lista, 0);
        }
        int chip = e.filtro == AgendaViewModel.Filtro.PASADAS ? R.id.chip_pasadas
                : e.filtro == AgendaViewModel.Filtro.CANCELADAS ? R.id.chip_canceladas : R.id.chip_proximas;
        if (b.filtros.getCheckedChipId() != chip) b.filtros.check(chip);

        dias.actualizar(e.diasConCitas, e.dia);
        boolean cambioFiltro = filtroPintado != e.filtro;
        filtroPintado = e.filtro;
        citas.submitList(e.items, () -> {
            if (cambioFiltro && b != null) b.lista.scrollToPosition(0);
        });

        boolean vacio = e.items.isEmpty();
        if (vacio) configurarVacio(e);
        if (vacio != (b.vacioContenedor.getVisibility() == View.VISIBLE)) {
            b.vacioContenedor.setVisibility(vacio ? View.VISIBLE : View.GONE);
            if (vacio) Animaciones.aparecerEscalando(b.vacio.getRoot(), 0);
        }
        b.lista.setVisibility(vacio ? View.INVISIBLE : View.VISIBLE);
        // Si el estado vacío ya ofrece "Agregar cita", el botón flotante sobra.
        boolean vacioConAccion = vacio && b.vacio.accion.getVisibility() == View.VISIBLE;
        if (vacioConAccion && b.fab.isShown()) b.fab.hide();
        else if (!vacioConAccion && !b.fab.isShown()) b.fab.show();
        actualizarMes();
    }

    private void configurarVacio(AgendaViewModel.Estado e) {
        @StringRes int t, c;
        int icono, color, fondo;
        boolean accion = false;
        if (e.filtro == AgendaViewModel.Filtro.PASADAS) {
            t = R.string.agenda_pasadas_vacio_titulo; c = R.string.agenda_pasadas_vacio_cuerpo;
            icono = R.drawable.ic_history; color = R.color.accent_historial; fondo = R.color.accent_historial_bg;
        } else if (e.filtro == AgendaViewModel.Filtro.CANCELADAS) {
            t = R.string.agenda_canceladas_vacio_titulo; c = R.string.agenda_canceladas_vacio_cuerpo;
            icono = R.drawable.ic_event_busy; color = R.color.accent_prep; fondo = R.color.accent_prep_bg;
        } else if (e.dia != null) {
            t = R.string.agenda_dia_vacio_titulo; c = R.string.agenda_dia_vacio_cuerpo;
            icono = R.drawable.ic_calendar; color = R.color.accent_agenda; fondo = R.color.accent_agenda_bg;
            accion = true;
        } else {
            t = R.string.agenda_vacia_titulo; c = R.string.agenda_vacia_cuerpo;
            icono = R.drawable.ic_calendar; color = R.color.accent_agenda; fondo = R.color.accent_agenda_bg;
            accion = true;
        }
        b.vacio.titulo.setText(t);
        b.vacio.cuerpo.setText(c);
        b.vacio.icono.setImageResource(icono);
        b.vacio.icono.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color)));
        b.vacio.circulo.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), fondo)));
        b.vacio.accion.setVisibility(accion ? View.VISIBLE : View.GONE);
        b.vacio.accion.setText(R.string.agregar_cita);
        Animaciones.alinearIcono(b.vacio.accion);
    }

    /** El mes mostrado sigue al primer día visible de la tira. */
    private void actualizarMes() {
        if (b == null) return;
        LinearLayoutManager lm = (LinearLayoutManager) b.dias.getLayoutManager();
        int pos = lm == null ? 0 : Math.max(0, lm.findFirstCompletelyVisibleItemPosition());
        b.mes.setText(Fechas.mesAnio(LocalDate.ofEpochDay(dias.diaEn(pos))));
    }

    private void nuevaCita() { nuevaCita(-1); }

    /** Nueva cita; en modo calendario usa el día mostrado y, si se tocó un hueco, esa hora. */
    private void nuevaCita(int minuto) {
        Intent i = new Intent(requireContext(), CitaFormActivity.class);
        AgendaViewModel.Estado e = vm.estado().getValue();
        if (e != null && e.calendario) i.putExtra(CitaFormActivity.EXTRA_DIA, e.diaCalendario);
        else if (e != null && e.dia != null) i.putExtra(CitaFormActivity.EXTRA_DIA, e.dia);
        if (minuto >= 0) i.putExtra(CitaFormActivity.EXTRA_MINUTO, minuto);
        abrir.launch(i, ActivityOptionsCompat.makeCustomAnimation(requireContext(),
                R.anim.hoja_entrar, R.anim.quieto));
    }

    private void abrirDetalle(long id) {
        Intent i = new Intent(requireContext(), CitaDetalleActivity.class).putExtra(CitaDetalleActivity.EXTRA_ID, id);
        abrir.launch(i, ActivityOptionsCompat.makeCustomAnimation(requireContext(),
                R.anim.deslizar_entrar, R.anim.deslizar_salir_atras));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

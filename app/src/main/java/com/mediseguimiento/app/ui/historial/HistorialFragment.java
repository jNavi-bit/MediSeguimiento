package com.mediseguimiento.app.ui.historial;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mediseguimiento.app.R;
import com.mediseguimiento.app.databinding.FragmentHistorialBinding;
import com.mediseguimiento.app.ui.cita.CitaDetalleActivity;
import com.mediseguimiento.app.ui.common.Animaciones;
import com.mediseguimiento.app.ui.common.Ventana;
import com.mediseguimiento.app.ui.estudios.EstudioFormActivity;

/** Línea de tiempo con consultas y estudios (RF-05). */
public class HistorialFragment extends Fragment {

    private FragmentHistorialBinding b;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentHistorialBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Ventana.aplicarPaddingSuperior(b.encabezado);
        b.mes.setVisibility(View.GONE);
        HistorialViewModel vm = new ViewModelProvider(this).get(HistorialViewModel.class);
        HistorialAdapter adapter = new HistorialAdapter(this::abrir);
        b.lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.lista.setAdapter(adapter);

        b.filtros.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            vm.setFiltro(id == R.id.chip_consultas ? HistorialViewModel.Filtro.CONSULTAS
                    : id == R.id.chip_estudios ? HistorialViewModel.Filtro.ESTUDIOS : HistorialViewModel.Filtro.TODO);
        });

        b.vacio.titulo.setText(R.string.historial_vacio_titulo);
        b.vacio.cuerpo.setText(R.string.historial_vacio_cuerpo);
        b.vacio.icono.setImageResource(R.drawable.ic_history);
        b.vacio.icono.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.accent_historial)));
        b.vacio.circulo.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.accent_historial_bg)));

        vm.items().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            boolean vacio = items.isEmpty();
            if (vacio != (b.vacioContenedor.getVisibility() == View.VISIBLE)) {
                b.vacioContenedor.setVisibility(vacio ? View.VISIBLE : View.GONE);
                if (vacio) Animaciones.aparecerEscalando(b.vacio.getRoot(), 0);
            }
            b.lista.setVisibility(vacio ? View.INVISIBLE : View.VISIBLE);
        });
    }

    private void abrir(HistorialViewModel.Item it) {
        Intent i = it.tipo == HistorialViewModel.Item.CITA
                ? new Intent(requireContext(), CitaDetalleActivity.class).putExtra(CitaDetalleActivity.EXTRA_ID, it.id)
                : new Intent(requireContext(), EstudioFormActivity.class).putExtra(EstudioFormActivity.EXTRA_ID, it.id);
        boolean cita = it.tipo == HistorialViewModel.Item.CITA;
        startActivity(i, ActivityOptionsCompat.makeCustomAnimation(requireContext(),
                cita ? R.anim.deslizar_entrar : R.anim.hoja_entrar,
                cita ? R.anim.deslizar_salir_atras : R.anim.quieto).toBundle());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) Ventana.iconosBarraEstado(requireActivity(), false);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) Ventana.iconosBarraEstado(requireActivity(), false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

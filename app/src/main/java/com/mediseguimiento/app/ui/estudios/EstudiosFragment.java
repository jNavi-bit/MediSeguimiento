package com.mediseguimiento.app.ui.estudios;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.mediseguimiento.app.R;
import com.mediseguimiento.app.databinding.FragmentEstudiosBinding;
import com.mediseguimiento.app.ui.common.Animaciones;
import com.mediseguimiento.app.ui.common.Ventana;

/** Lista de estudios registrados por el paciente (RF-04). */
public class EstudiosFragment extends Fragment {

    private FragmentEstudiosBinding b;
    private EstudioAdapter adapter;

    private final ActivityResultLauncher<Intent> abrir = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != Activity.RESULT_OK || r.getData() == null || b == null) return;
                String m = r.getData().getStringExtra(EstudioFormActivity.RESULTADO_MENSAJE);
                if (m != null) Snackbar.make(b.getRoot(), m, Snackbar.LENGTH_SHORT).setAnchorView(b.fab).show();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentEstudiosBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Ventana.aplicarPaddingSuperior(b.encabezado);
        b.mes.setVisibility(View.GONE);
        adapter = new EstudioAdapter(e -> abrirFormulario(e.id));
        b.lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.lista.setAdapter(adapter);
        b.lista.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 6 && b.fab.isExtended()) b.fab.shrink();
                else if (dy < -6 && !b.fab.isExtended()) b.fab.extend();
            }
        });
        b.fab.setOnClickListener(v -> abrirFormulario(-1));

        b.vacio.titulo.setText(R.string.estudios_vacio_titulo);
        b.vacio.cuerpo.setText(R.string.estudios_vacio_cuerpo);
        b.vacio.icono.setImageResource(R.drawable.ic_study);
        b.vacio.icono.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.accent_estudios)));
        b.vacio.circulo.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.accent_estudios_bg)));
        b.vacio.accion.setVisibility(View.VISIBLE);
        b.vacio.accion.setText(R.string.agregar_estudio);
        b.vacio.accion.setOnClickListener(v -> abrirFormulario(-1));
        Animaciones.alinearIcono(b.vacio.accion);

        new ViewModelProvider(this).get(EstudiosViewModel.class).items().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            boolean vacio = items.isEmpty();
            if (vacio != (b.vacioContenedor.getVisibility() == View.VISIBLE)) {
                b.vacioContenedor.setVisibility(vacio ? View.VISIBLE : View.GONE);
                if (vacio) Animaciones.aparecerEscalando(b.vacio.getRoot(), 0);
            }
            b.lista.setVisibility(vacio ? View.INVISIBLE : View.VISIBLE);
            if (vacio && b.fab.isShown()) b.fab.hide();
            else if (!vacio && !b.fab.isShown()) b.fab.show();
        });
    }

    private void abrirFormulario(long id) {
        Intent i = new Intent(requireContext(), EstudioFormActivity.class);
        if (id >= 0) i.putExtra(EstudioFormActivity.EXTRA_ID, id);
        abrir.launch(i, ActivityOptionsCompat.makeCustomAnimation(requireContext(), R.anim.hoja_entrar, R.anim.quieto));
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

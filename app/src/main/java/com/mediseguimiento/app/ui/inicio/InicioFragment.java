package com.mediseguimiento.app.ui.inicio;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.content.res.ColorStateList;

import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.ui.cita.CitaDetalleActivity;
import com.mediseguimiento.app.ui.cita.CitaFormActivity;
import com.mediseguimiento.app.databinding.FragmentInicioBinding;
import com.mediseguimiento.app.databinding.ItemAccesoRapidoBinding;
import com.mediseguimiento.app.ui.MainActivity;
import com.mediseguimiento.app.ui.common.Animaciones;
import com.mediseguimiento.app.ui.common.PacienteViewModel;
import com.mediseguimiento.app.ui.common.Ventana;

import java.util.Calendar;

/** Pantalla de inicio: saludo, próxima cita, accesos rápidos y aviso de uso. */
public class InicioFragment extends Fragment {

    private FragmentInicioBinding binding;
    private InicioViewModel inicio;
    @Nullable private Cita proxima;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentInicioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Ventana.aplicarPaddingSuperior(binding.hero);
        binding.saludo.setText(saludoSegunHora());
        new ViewModelProvider(requireActivity()).get(PacienteViewModel.class).paciente()
                .observe(getViewLifecycleOwner(), p -> {
                    String nombre = PacienteViewModel.primerNombre(p);
                    String saludo = getString(saludoSegunHora());
                    binding.saludo.setText(nombre.isEmpty() ? saludo : saludo + ",\n" + nombre);
                });
        Animaciones.entrar(binding.tarjetaProxima, 120);

        inicio = new ViewModelProvider(this).get(InicioViewModel.class);
        inicio.proxima().observe(getViewLifecycleOwner(), this::mostrarProxima);
        binding.tarjetaProxima.setOnClickListener(v -> {
            Intent i = proxima != null
                    ? new Intent(requireContext(), CitaDetalleActivity.class).putExtra(CitaDetalleActivity.EXTRA_ID, proxima.id)
                    : new Intent(requireContext(), CitaFormActivity.class);
            int entrar = proxima != null ? R.anim.deslizar_entrar : R.anim.hoja_entrar;
            int salir = proxima != null ? R.anim.deslizar_salir_atras : R.anim.quieto;
            startActivity(i, ActivityOptionsCompat.makeCustomAnimation(requireContext(), entrar, salir).toBundle());
        });

        acceso(binding.accesoAgenda, R.string.accion_agenda, R.string.accion_agenda_desc,
                R.drawable.ic_calendar, R.color.accent_agenda, R.color.accent_agenda_bg, R.id.nav_agenda);
        acceso(binding.accesoEstudios, R.string.accion_estudios, R.string.accion_estudios_desc,
                R.drawable.ic_study, R.color.accent_estudios, R.color.accent_estudios_bg, R.id.nav_estudios);
        acceso(binding.accesoHistorial, R.string.accion_historial, R.string.accion_historial_desc,
                R.drawable.ic_history, R.color.accent_historial, R.color.accent_historial_bg, R.id.nav_historial);
        acceso(binding.accesoPerfil, R.string.accion_perfil, R.string.accion_perfil_desc,
                R.drawable.ic_shield, R.color.accent_prep, R.color.accent_prep_bg, R.id.nav_perfil);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            Ventana.iconosBarraEstado(requireActivity(), true);
            inicio.refrescar();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            Ventana.iconosBarraEstado(requireActivity(), true);
            inicio.refrescar();
        }
    }

    /** Pinta la próxima cita (o la invitación a crear una) en la tarjeta translúcida. */
    private void mostrarProxima(@Nullable Cita c) {
        proxima = c;
        if (binding == null) return;
        if (c == null) {
            binding.proximaTitulo.setText(R.string.inicio_sin_cita_titulo);
            binding.proximaDetalle.setText(R.string.inicio_sin_cita_cuerpo);
            binding.proximaCuenta.setVisibility(View.GONE);
            binding.proximaIcono.setImageResource(R.drawable.ic_add);
            binding.tarjetaProxima.setContentDescription(getString(R.string.nueva_cita));
            return;
        }
        long ahora = System.currentTimeMillis();
        boolean h24 = DateFormat.is24HourFormat(requireContext());
        binding.proximaTitulo.setText(c.especialidad);
        String detalle = Fechas.fechaCorta(c.fechaHora) + " · " + Fechas.horaCompleta(c.fechaHora, h24);
        if (!TextUtils.isEmpty(c.medico)) detalle += "\n" + c.medico;
        binding.proximaDetalle.setText(detalle);
        binding.proximaCuenta.setText(Fechas.cuentaRegresiva(c.fechaHora, ahora));
        binding.proximaCuenta.setVisibility(View.VISIBLE);
        binding.proximaIcono.setImageResource(R.drawable.ic_calendar);
        binding.tarjetaProxima.setContentDescription(getString(R.string.inicio_ver_detalle) + ": "
                + c.especialidad + ", " + Fechas.fechaLarga(c.fechaHora, ahora));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void acceso(ItemAccesoRapidoBinding item, @StringRes int titulo, @StringRes int desc,
                        @DrawableRes int icono, @ColorRes int color, @ColorRes int fondo,
                        @IdRes int destino) {
        item.titulo.setText(titulo);
        item.descripcion.setText(desc);
        item.icono.setImageResource(icono);
        item.icono.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color)));
        item.insignia.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), fondo)));
        item.getRoot().setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).irA(destino);
        });
    }

    @StringRes
    private static int saludoSegunHora() {
        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hora >= 5 && hora < 12) return R.string.saludo_manana;
        if (hora >= 12 && hora < 19) return R.string.saludo_tarde;
        return R.string.saludo_noche;
    }
}

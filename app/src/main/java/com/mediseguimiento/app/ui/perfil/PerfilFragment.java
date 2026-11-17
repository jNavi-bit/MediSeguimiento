package com.mediseguimiento.app.ui.perfil;

import android.content.Intent;
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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mediseguimiento.app.BuildConfig;
import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Sesion;
import com.mediseguimiento.app.data.db.entity.Paciente;
import com.mediseguimiento.app.databinding.FragmentPerfilBinding;
import com.mediseguimiento.app.databinding.ItemFilaInfoBinding;
import com.mediseguimiento.app.ui.auth.AuthActivity;
import com.mediseguimiento.app.ui.common.Animaciones;
import com.mediseguimiento.app.ui.common.PacienteViewModel;
import com.mediseguimiento.app.ui.common.Ventana;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Perfil del paciente, resumen de privacidad y cierre de sesión. */
public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding b;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentPerfilBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Ventana.aplicarPaddingSuperior(b.encabezado);
        PacienteViewModel vm = new ViewModelProvider(requireActivity()).get(PacienteViewModel.class);
        vm.paciente().observe(getViewLifecycleOwner(), this::mostrar);

        // Estadísticas en vivo: cada conteo es una consulta observable de Room.
        long hoy = com.mediseguimiento.app.core.Fechas.inicioDelDia(System.currentTimeMillis());
        com.mediseguimiento.app.data.db.AppDatabase db = com.mediseguimiento.app.data.db.AppDatabase.get(requireContext());
        long pid = new Sesion(requireContext()).pacienteId();
        db.citaDao().contarProximas(pid, hoy).observe(getViewLifecycleOwner(), n -> contar(b.statProximas, n));
        new com.mediseguimiento.app.data.repo.EstudioRepository(requireContext()).contar()
                .observe(getViewLifecycleOwner(), n -> contar(b.statEstudios, n));
        new com.mediseguimiento.app.data.repo.PreparacionRepository(requireContext()).contar()
                .observe(getViewLifecycleOwner(), n -> contar(b.statPreparadas, n));
        b.btnCerrarSesion.setOnClickListener(v -> confirmarCierre());
        Animaciones.alinearIcono(b.btnCerrarSesion);
        b.version.setText(getString(R.string.version_pie, BuildConfig.VERSION_NAME));
        fila(b.filaLocal, R.drawable.ic_phone, R.string.perfil_local_titulo, R.string.perfil_local_cuerpo,
                R.color.accent_agenda, R.color.accent_agenda_bg);
        fila(b.filaHash, R.drawable.ic_key, R.string.perfil_hash_titulo, R.string.perfil_hash_cuerpo,
                R.color.accent_estudios, R.color.accent_estudios_bg);
        fila(b.filaConsent, R.drawable.ic_shield, R.string.perfil_consent_titulo, R.string.perfil_consent_cuerpo,
                R.color.accent_historial, R.color.accent_historial_bg);
    }

    /** Cuenta animada desde el valor actual hasta el nuevo. */
    private static void contar(android.widget.TextView tv, Integer n) {
        int hasta = n == null ? 0 : n;
        int desde;
        try { desde = Integer.parseInt(tv.getText().toString()); } catch (NumberFormatException e) { desde = 0; }
        android.animation.ValueAnimator va = android.animation.ValueAnimator.ofInt(desde, hasta);
        va.setDuration(500);
        va.addUpdateListener(a -> tv.setText(String.valueOf(a.getAnimatedValue())));
        va.start();
    }

    private void fila(ItemFilaInfoBinding f, int icono, int titulo, int cuerpo, int color, int fondo) {
        f.icono.setImageResource(icono);
        f.icono.setImageTintList(ContextCompat.getColorStateList(requireContext(), color));
        f.insignia.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), fondo));
        f.titulo.setText(titulo);
        f.cuerpo.setText(cuerpo);
    }

    private void mostrar(@Nullable Paciente p) {
        if (p == null) return;
        b.avatar.setText(PacienteViewModel.iniciales(p));
        b.nombre.setText(p.nombre);
        b.correo.setText(p.correo);
        String fecha = new SimpleDateFormat("MMMM yyyy", new Locale("es", "MX")).format(new Date(p.creadoEn));
        b.miembro.setText(getString(R.string.perfil_miembro_desde, fecha));
    }

    private void confirmarCierre() {
        androidx.appcompat.app.AlertDialog d = new MaterialAlertDialogBuilder(requireContext(),
                R.style.ThemeOverlay_Medi_Dialogo_Destructivo)
                .setIcon(R.drawable.ic_logout)
                .setTitle(R.string.cerrar_sesion_titulo)
                .setMessage(R.string.cerrar_sesion_cuerpo)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.cerrar_sesion, (di, w) -> cerrarSesion())
                .show();
    }

    private void cerrarSesion() {
        new Sesion(requireContext()).cerrar();
        Intent i = new Intent(requireContext(), AuthActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i, ActivityOptionsCompat.makeCustomAnimation(requireContext(),
                R.anim.fade_entrar, R.anim.fade_salir).toBundle());
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

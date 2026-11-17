package com.mediseguimiento.app.ui.auth;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Sesion;
import com.mediseguimiento.app.databinding.ActivityBienvenidaBinding;
import com.mediseguimiento.app.databinding.ItemChipFlotanteBinding;
import com.mediseguimiento.app.databinding.ItemSlideBinding;
import com.mediseguimiento.app.ui.common.Animaciones;
import com.mediseguimiento.app.ui.common.Ventana;

import java.util.ArrayList;
import java.util.List;

/**
 * Carrusel de bienvenida con ilustraciones animadas, indicadores de página
 * y paralaje al deslizar.
 */
public class BienvenidaActivity extends AppCompatActivity {

    private ActivityBienvenidaBinding b;
    private final List<View> puntos = new ArrayList<>();
    /** Páginas visibles con animaciones en curso (para pausarlas en segundo plano). */
    private final java.util.Set<SlideVH> activas = new java.util.HashSet<>();

    private static final Slide[] SLIDES = {
            new Slide(R.drawable.ic_calendar, R.string.slide1_titulo, R.string.slide1_cuerpo,
                    R.drawable.ic_clock, R.string.slide1_chip1, R.drawable.ic_bell, R.string.slide1_chip2),
            new Slide(R.drawable.ic_edit_note, R.string.slide2_titulo, R.string.slide2_cuerpo,
                    R.drawable.ic_check, R.string.slide2_chip1, R.drawable.ic_summary, R.string.slide2_chip2),
            new Slide(R.drawable.ic_shield, R.string.slide3_titulo, R.string.slide3_cuerpo,
                    R.drawable.ic_phone, R.string.slide3_chip1, R.drawable.ic_lock, R.string.slide3_chip2),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        b = ActivityBienvenidaBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        Ventana.iconosBarraEstado(this, true);
        Ventana.aplicarPaddingSuperior(b.barraSuperior);
        Ventana.aplicarPaddingInferior(b.panelInferior);

        b.paginas.setAdapter(new SlideAdapter());
        b.paginas.setOffscreenPageLimit(1);
        b.paginas.setPageTransformer(this::transformar);
        crearIndicadores();
        b.paginas.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                actualizarControles(position);
                Animaciones.vibrarToque(b.paginas);
            }
        });
        actualizarControles(0);
        Animaciones.alinearIcono(b.btnPrincipal);

        b.btnOmitir.setOnClickListener(v -> b.paginas.setCurrentItem(SLIDES.length - 1, true));
        b.btnPrincipal.setOnClickListener(v -> {
            int actual = b.paginas.getCurrentItem();
            if (actual < SLIDES.length - 1) b.paginas.setCurrentItem(actual + 1, true);
            else irAAcceso(true);
        });
        b.btnSecundario.setOnClickListener(v -> irAAcceso(false));

        Animaciones.entrar(b.panelInferior, 250);
        Animaciones.entrar(b.barraSuperior, 100);
    }

    @Override
    protected void onStart() {
        super.onStart();
        for (SlideVH h : activas) for (Animator a : h.animaciones) a.resume();
    }

    /** Las animaciones infinitas se pausan cuando la pantalla no está visible (batería). */
    @Override
    protected void onStop() {
        super.onStop();
        for (SlideVH h : activas) for (Animator a : h.animaciones) a.pause();
    }

    private void irAAcceso(boolean registro) {
        new Sesion(this).marcarOnboardingVisto();
        Intent i = new Intent(this, AuthActivity.class).putExtra(AuthActivity.EXTRA_REGISTRO, registro);
        ActivityOptionsCompat op = ActivityOptionsCompat.makeSceneTransitionAnimation(this, b.logo, "logo");
        startActivity(i, op.toBundle());
    }

    /** Paralaje: la ilustración se desplaza más lento que el texto y se desvanece. */
    private void transformar(@NonNull View page, float pos) {
        View ilus = page.findViewById(R.id.ilustracion);
        View titulo = page.findViewById(R.id.titulo);
        View cuerpo = page.findViewById(R.id.cuerpo);
        float abs = Math.min(1f, Math.abs(pos));
        float w = page.getWidth();
        ilus.setTranslationX(pos * w * 0.35f);
        ilus.setAlpha(1f - abs * 0.8f);
        ilus.setScaleX(1f - abs * 0.15f);
        ilus.setScaleY(1f - abs * 0.15f);
        titulo.setTranslationX(pos * w * 0.12f);
        cuerpo.setTranslationX(pos * w * 0.2f);
        titulo.setAlpha(1f - abs);
        cuerpo.setAlpha(1f - abs);
    }

    private void crearIndicadores() {
        float dp = getResources().getDisplayMetrics().density;
        for (int i = 0; i < SLIDES.length; i++) {
            View p = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) (8 * dp), (int) (8 * dp));
            lp.setMarginStart(i == 0 ? 0 : (int) (8 * dp));
            p.setLayoutParams(lp);
            p.setBackgroundResource(R.drawable.bg_pill);
            b.indicadores.addView(p);
            puntos.add(p);
        }
    }

    private void actualizarControles(int pos) {
        float dp = getResources().getDisplayMetrics().density;
        int blanco = 0xFFFFFFFF, tenue = 0x59FFFFFF;
        for (int i = 0; i < puntos.size(); i++) {
            View p = puntos.get(i);
            boolean activo = i == pos;
            int desde = p.getLayoutParams().width;
            int hasta = (int) ((activo ? 28 : 8) * dp);
            ValueAnimator va = ValueAnimator.ofInt(desde, hasta);
            va.setDuration(320);
            va.setInterpolator(Animaciones.ENFATIZADA);
            va.addUpdateListener(a -> {
                p.getLayoutParams().width = (int) a.getAnimatedValue();
                p.requestLayout();
            });
            va.start();
            p.getBackground().mutate().setTint(activo ? blanco : tenue);
        }
        boolean ultima = pos == SLIDES.length - 1;
        b.btnPrincipal.setText(ultima ? R.string.crear_cuenta : R.string.siguiente);
        Animaciones.alinearIcono(b.btnPrincipal);
        b.btnOmitir.animate().alpha(ultima ? 0f : 1f).setDuration(200).start();
        b.btnOmitir.setEnabled(!ultima);
        b.paginas.setContentDescription(getString(R.string.slide_n_de_m, pos + 1, SLIDES.length));
    }

    // ================= Adaptador =================

    private static final class Slide {
        final int icono, titulo, cuerpo, chip1Icono, chip1Texto, chip2Icono, chip2Texto;

        Slide(@DrawableRes int icono, @StringRes int titulo, @StringRes int cuerpo,
              @DrawableRes int chip1Icono, @StringRes int chip1Texto,
              @DrawableRes int chip2Icono, @StringRes int chip2Texto) {
            this.icono = icono;
            this.titulo = titulo;
            this.cuerpo = cuerpo;
            this.chip1Icono = chip1Icono;
            this.chip1Texto = chip1Texto;
            this.chip2Icono = chip2Icono;
            this.chip2Texto = chip2Texto;
        }
    }

    private static final class SlideVH extends RecyclerView.ViewHolder {
        final ItemSlideBinding vb;
        final List<Animator> animaciones = new ArrayList<>();

        SlideVH(ItemSlideBinding vb) {
            super(vb.getRoot());
            this.vb = vb;
        }

        void detener() {
            for (Animator a : animaciones) a.cancel();
            animaciones.clear();
        }
    }

    private final class SlideAdapter extends RecyclerView.Adapter<SlideVH> {

        @NonNull
        @Override
        public SlideVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new SlideVH(ItemSlideBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull SlideVH h, int position) {
            Slide s = SLIDES[position];
            h.vb.icono.setImageResource(s.icono);
            h.vb.titulo.setText(s.titulo);
            h.vb.cuerpo.setText(s.cuerpo);
            chip(h.vb.chip1, s.chip1Icono, s.chip1Texto, position * 2);
            chip(h.vb.chip2, s.chip2Icono, s.chip2Texto, position * 2 + 1);
        }

        private void chip(ItemChipFlotanteBinding c, int icono, int texto, int variante) {
            c.chipIcono.setImageResource(icono);
            c.chipTexto.setText(texto);
            int[][] paleta = {
                    {R.color.accent_agenda_bg, R.color.accent_agenda},
                    {R.color.accent_prep_bg, R.color.accent_prep},
                    {R.color.accent_historial_bg, R.color.accent_historial},
                    {R.color.accent_estudios_bg, R.color.accent_estudios},
            };
            int[] col = paleta[variante % paleta.length];
            c.chipInsignia.setBackgroundTintList(ContextCompat.getColorStateList(BienvenidaActivity.this, col[0]));
            c.chipIcono.setImageTintList(ContextCompat.getColorStateList(BienvenidaActivity.this, col[1]));
        }

        @Override
        public void onViewAttachedToWindow(@NonNull SlideVH h) {
            h.detener();
            h.animaciones.add(Animaciones.flotar(h.vb.chip1.getRoot(), 10, 2600, 0));
            h.animaciones.add(Animaciones.flotar(h.vb.chip2.getRoot(), 12, 3100, 400));
            h.animaciones.add(Animaciones.respirar(h.vb.anilloMedio, 1.06f, 2400));
            h.animaciones.add(Animaciones.girar(h.vb.anilloExterno, 40_000));
            h.animaciones.add(Animaciones.flotar(h.vb.disco, 6, 3400, 200));
            activas.add(h);
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull SlideVH h) {
            activas.remove(h);
            h.detener();
        }

        @Override
        public int getItemCount() { return SLIDES.length; }
    }

}

package com.mediseguimiento.app.ui.agenda;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.databinding.ItemDiaBinding;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

/** Tira horizontal con los próximos días; marca los que tienen citas con un punto. */
public class DiaAdapter extends RecyclerView.Adapter<DiaAdapter.VH> {

    public static final int DIAS = 45;

    public interface AlElegir { void dia(long epochDay); }

    private final AlElegir alElegir;
    private long hoy = LocalDate.now(Fechas.zona()).toEpochDay();
    @Nullable private Long seleccionado;
    private Set<Long> conCitas = Collections.emptySet();

    public DiaAdapter(AlElegir alElegir) {
        this.alElegir = alElegir;
        setHasStableIds(true);
    }

    public void actualizar(Set<Long> conCitas, @Nullable Long seleccionado) {
        this.hoy = LocalDate.now(Fechas.zona()).toEpochDay();
        this.conCitas = conCitas;
        this.seleccionado = seleccionado;
        notifyItemRangeChanged(0, DIAS);
    }

    public long diaEn(int posicion) { return hoy + posicion; }

    @Override
    public long getItemId(int position) { return hoy + position; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemDiaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        long ed = hoy + position;
        LocalDate d = LocalDate.ofEpochDay(ed);
        boolean sel = seleccionado != null && seleccionado == ed;
        boolean esHoy = position == 0;
        boolean tiene = conCitas.contains(ed);

        h.b.diaSemana.setText(esHoy ? "HOY" : Fechas.diaSemanaCorto(d));
        h.b.numero.setText(String.valueOf(d.getDayOfMonth()));
        h.b.raiz.setBackgroundResource(sel ? R.drawable.bg_dia_seleccionado
                : esHoy ? R.drawable.bg_dia_hoy : R.drawable.bg_dia_normal);

        View v = h.b.raiz;
        int principal = sel ? ContextCompat.getColor(v.getContext(), R.color.hero_on)
                : MaterialColors.getColor(v, com.google.android.material.R.attr.colorOnSurface);
        int secundario = sel ? ContextCompat.getColor(v.getContext(), R.color.hero_on_muted)
                : esHoy ? MaterialColors.getColor(v, androidx.appcompat.R.attr.colorPrimary)
                : MaterialColors.getColor(v, com.google.android.material.R.attr.colorOnSurfaceVariant);
        h.b.numero.setTextColor(principal);
        h.b.diaSemana.setTextColor(secundario);

        h.b.punto.setVisibility(tiene ? View.VISIBLE : View.INVISIBLE);
        h.b.punto.setBackgroundTintList(ColorStateList.valueOf(sel
                ? ContextCompat.getColor(v.getContext(), R.color.hero_on)
                : MaterialColors.getColor(v, com.google.android.material.R.attr.colorTertiary)));

        String desc = Fechas.fechaLarga(Fechas.aMillis(d, java.time.LocalTime.NOON), System.currentTimeMillis());
        v.setContentDescription(tiene ? v.getContext().getString(R.string.dia_con_citas, desc) : desc);
        v.setSelected(sel);
        v.setOnClickListener(x -> alElegir.dia(ed));
    }

    @Override
    public int getItemCount() { return DIAS; }

    static final class VH extends RecyclerView.ViewHolder {
        final ItemDiaBinding b;

        VH(ItemDiaBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}

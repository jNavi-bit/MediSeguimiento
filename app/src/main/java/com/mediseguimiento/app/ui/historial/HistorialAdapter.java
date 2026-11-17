package com.mediseguimiento.app.ui.historial;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.databinding.ItemEncabezadoDiaBinding;
import com.mediseguimiento.app.databinding.ItemTimelineBinding;
import com.mediseguimiento.app.ui.common.Acentos;

import java.util.Objects;

public class HistorialAdapter extends ListAdapter<HistorialViewModel.Item, RecyclerView.ViewHolder> {

    public interface AlTocar { void item(HistorialViewModel.Item it); }

    private final AlTocar alTocar;

    public HistorialAdapter(AlTocar alTocar) {
        super(new DiffUtil.ItemCallback<HistorialViewModel.Item>() {
            @Override
            public boolean areItemsTheSame(@NonNull HistorialViewModel.Item a, @NonNull HistorialViewModel.Item b) {
                return a.tipo == b.tipo && a.id == b.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull HistorialViewModel.Item a, @NonNull HistorialViewModel.Item b) {
                return a.fecha == b.fecha && Objects.equals(a.titulo, b.titulo) && Objects.equals(a.cuerpo, b.cuerpo)
                        && Objects.equals(a.etiqueta, b.etiqueta);
            }
        });
        this.alTocar = alTocar;
    }

    @Override
    public int getItemViewType(int position) { return getItem(position).tipo == HistorialViewModel.Item.MES ? 0 : 1; }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == 0) {
            ItemEncabezadoDiaBinding b = ItemEncabezadoDiaBinding.inflate(inf, parent, false);
            b.titulo.setPaddingRelative((int) (8 * parent.getResources().getDisplayMetrics().density),
                    b.titulo.getPaddingTop(), 0, b.titulo.getPaddingBottom());
            return new RecyclerView.ViewHolder(b.getRoot()) { };
        }
        return new VH(ItemTimelineBinding.inflate(inf, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        HistorialViewModel.Item it = getItem(position);
        if (!(h instanceof VH)) {
            ((TextView) h.itemView).setText(it.titulo);
            return;
        }
        boolean inicio = position == 0 || getItem(position - 1).tipo == HistorialViewModel.Item.MES;
        boolean fin = position == getItemCount() - 1 || getItem(position + 1).tipo == HistorialViewModel.Item.MES;
        ((VH) h).bind(it, inicio, fin);
    }

    final class VH extends RecyclerView.ViewHolder {
        final ItemTimelineBinding b;

        VH(ItemTimelineBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(HistorialViewModel.Item it, boolean inicio, boolean fin) {
            Context ctx = itemView.getContext();
            boolean cita = it.tipo == HistorialViewModel.Item.CITA;
            int color = ContextCompat.getColor(ctx, cita ? Acentos.color(it.clave) : R.color.accent_estudios);
            int fondo = ContextCompat.getColor(ctx, cita ? Acentos.fondo(it.clave) : R.color.accent_estudios_bg);

            b.lineaSuperior.setVisibility(inicio ? View.INVISIBLE : View.VISIBLE);
            b.lineaInferior.setVisibility(fin ? View.INVISIBLE : View.VISIBLE);
            b.punto.setBackgroundTintList(ColorStateList.valueOf(fondo));
            b.icono.setImageResource(cita ? R.drawable.ic_calendar : R.drawable.ic_study);
            b.icono.setImageTintList(ColorStateList.valueOf(color));

            b.tipo.setText(ctx.getString(cita ? R.string.tipo_consulta : R.string.tipo_estudio));
            b.tipo.setTextColor(color);
            b.titulo.setText(it.titulo);
            String fecha = Fechas.fechaLarga(it.fecha, System.currentTimeMillis());
            if (cita) fecha += " · " + Fechas.horaCompleta(it.fecha, DateFormat.is24HourFormat(ctx));
            b.fecha.setText(fecha);
            b.cuerpo.setVisibility(TextUtils.isEmpty(it.cuerpo) ? View.GONE : View.VISIBLE);
            b.cuerpo.setText(it.cuerpo);

            boolean conEtiqueta = cita && !TextUtils.isEmpty(it.etiqueta);
            b.etiqueta.setVisibility(conEtiqueta ? View.VISIBLE : View.GONE);
            if (conEtiqueta) {
                b.etiqueta.setText(it.etiqueta);
                b.etiqueta.setBackgroundTintList(ColorStateList.valueOf(it.futura
                        ? ContextCompat.getColor(ctx, R.color.hero_start) : fondo));
                b.etiqueta.setTextColor(it.futura ? ContextCompat.getColor(ctx, R.color.hero_on) : color);
            }
            b.tarjeta.setAlpha(it.futura ? 0.92f : 1f);
            b.tarjeta.setContentDescription(b.tipo.getText() + ": " + it.titulo + ", " + fecha);
            b.tarjeta.setOnClickListener(v -> alTocar.item(it));
        }
    }
}

package com.mediseguimiento.app.ui.agenda;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Paint;
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

import com.google.android.material.color.MaterialColors;
import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.databinding.ItemCitaBinding;
import com.mediseguimiento.app.databinding.ItemEncabezadoDiaBinding;
import com.mediseguimiento.app.ui.common.Acentos;

/** Lista agrupada de citas con encabezados por día. */
public class CitaAdapter extends ListAdapter<ItemAgenda, RecyclerView.ViewHolder> {

    public interface AlTocar { void cita(Cita c, View vista); }

    private final AlTocar alTocar;

    public CitaAdapter(AlTocar alTocar) {
        super(new DiffUtil.ItemCallback<ItemAgenda>() {
            @Override
            public boolean areItemsTheSame(@NonNull ItemAgenda a, @NonNull ItemAgenda b) { return a.id == b.id && a.tipo == b.tipo; }

            @Override
            public boolean areContentsTheSame(@NonNull ItemAgenda a, @NonNull ItemAgenda b) { return a.mismoContenido(b); }
        });
        this.alTocar = alTocar;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) { return getItem(position).id; }

    @Override
    public int getItemViewType(int position) { return getItem(position).tipo; }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == ItemAgenda.ENCABEZADO) {
            return new EncabezadoVH(ItemEncabezadoDiaBinding.inflate(inf, parent, false));
        }
        return new CitaVH(ItemCitaBinding.inflate(inf, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemAgenda item = getItem(position);
        if (holder instanceof EncabezadoVH) {
            ((EncabezadoVH) holder).b.titulo.setText(item.titulo);
        } else if (item.cita != null) {
            ((CitaVH) holder).bind(item.cita);
        }
    }

    static final class EncabezadoVH extends RecyclerView.ViewHolder {
        final ItemEncabezadoDiaBinding b;

        EncabezadoVH(ItemEncabezadoDiaBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }

    final class CitaVH extends RecyclerView.ViewHolder {
        final ItemCitaBinding b;

        CitaVH(ItemCitaBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(Cita c) {
            Context ctx = b.getRoot().getContext();
            long ahora = System.currentTimeMillis();
            boolean h24 = DateFormat.is24HourFormat(ctx);

            b.hora.setText(Fechas.hora(c.fechaHora, h24));
            b.periodo.setText(h24 ? "" : Fechas.periodo(c.fechaHora));
            b.periodo.setVisibility(h24 ? View.GONE : View.VISIBLE);
            b.especialidad.setText(c.especialidad);
            mostrar(b.filaMedico, b.medico, c.medico);
            mostrar(b.filaLugar, b.lugar, c.lugar);

            int acento = ContextCompat.getColor(ctx, Acentos.color(c.especialidad));
            int fondo = ContextCompat.getColor(ctx, Acentos.fondo(c.especialidad));
            b.barra.setBackgroundTintList(ColorStateList.valueOf(acento));

            boolean cancelada = Cita.ESTADO_CANCELADA.equals(c.estado);
            boolean completada = Cita.ESTADO_COMPLETADA.equals(c.estado);

            // Cuenta regresiva (solo para programadas)
            if (!cancelada && !completada) {
                b.cuenta.setVisibility(View.VISIBLE);
                b.cuenta.setText(Fechas.cuentaRegresiva(c.fechaHora, ahora));
                b.cuenta.setBackgroundTintList(ColorStateList.valueOf(fondo));
                b.cuenta.setTextColor(acento);
            } else {
                b.cuenta.setVisibility(View.GONE);
            }

            // Estado
            if (cancelada || completada || c.fechaHora < Fechas.inicioDelDia(ahora)) {
                b.estado.setVisibility(View.VISIBLE);
                int texto = cancelada ? R.string.estado_cancelada
                        : completada ? R.string.estado_completada : R.string.estado_programada;
                b.estado.setText(texto);
                int attrFondo = cancelada ? com.google.android.material.R.attr.colorErrorContainer
                        : com.google.android.material.R.attr.colorSurfaceContainerHigh;
                int attrTexto = cancelada ? com.google.android.material.R.attr.colorOnErrorContainer
                        : com.google.android.material.R.attr.colorOnSurfaceVariant;
                b.estado.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(b.estado, attrFondo)));
                b.estado.setTextColor(MaterialColors.getColor(b.estado, attrTexto));
            } else {
                b.estado.setVisibility(View.GONE);
            }

            b.especialidad.setPaintFlags(cancelada
                    ? b.especialidad.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                    : b.especialidad.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            b.getRoot().setAlpha(cancelada ? 0.72f : 1f);

            b.getRoot().setContentDescription(c.especialidad + ", "
                    + Fechas.fechaLarga(c.fechaHora, ahora) + ", " + Fechas.horaCompleta(c.fechaHora, h24));
            b.getRoot().setOnClickListener(v -> alTocar.cita(c, v));
        }

        private void mostrar(View fila, TextView tv, String valor) {
            boolean hay = !TextUtils.isEmpty(valor);
            fila.setVisibility(hay ? View.VISIBLE : View.GONE);
            tv.setText(hay ? valor : "");
        }
    }
}

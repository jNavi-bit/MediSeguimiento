package com.mediseguimiento.app.ui.estudios;

import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.data.db.entity.Estudio;
import com.mediseguimiento.app.databinding.ItemEncabezadoDiaBinding;
import com.mediseguimiento.app.databinding.ItemEstudioBinding;
import com.mediseguimiento.app.ui.common.Acentos;

import java.util.Objects;

public class EstudioAdapter extends ListAdapter<EstudiosViewModel.Item, RecyclerView.ViewHolder> {

    public interface AlTocar { void estudio(Estudio e); }

    private final AlTocar alTocar;

    public EstudioAdapter(AlTocar alTocar) {
        super(new DiffUtil.ItemCallback<EstudiosViewModel.Item>() {
            @Override
            public boolean areItemsTheSame(@NonNull EstudiosViewModel.Item a, @NonNull EstudiosViewModel.Item b) {
                return a.tipo == b.tipo && a.id == b.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull EstudiosViewModel.Item a, @NonNull EstudiosViewModel.Item b) {
                if (a.tipo == EstudiosViewModel.Item.MES) return Objects.equals(a.titulo, b.titulo);
                Estudio x = a.estudio, y = b.estudio;
                return Objects.equals(x.nombre, y.nombre) && x.fecha == y.fecha
                        && Objects.equals(x.resultado, y.resultado) && Objects.equals(x.laboratorio, y.laboratorio)
                        && Objects.equals(a.cita, b.cita);
            }
        });
        this.alTocar = alTocar;
    }

    @Override
    public int getItemViewType(int position) { return getItem(position).tipo; }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == EstudiosViewModel.Item.MES) {
            ItemEncabezadoDiaBinding b = ItemEncabezadoDiaBinding.inflate(inf, parent, false);
            return new RecyclerView.ViewHolder(b.getRoot()) { };
        }
        ItemEstudioBinding b = ItemEstudioBinding.inflate(inf, parent, false);
        return new EstudioVH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        EstudiosViewModel.Item it = getItem(position);
        if (h instanceof EstudioVH) ((EstudioVH) h).bind(it);
        else ((android.widget.TextView) h.itemView).setText(it.titulo);
    }

    final class EstudioVH extends RecyclerView.ViewHolder {
        final ItemEstudioBinding b;

        EstudioVH(ItemEstudioBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(EstudiosViewModel.Item it) {
            Estudio e = it.estudio;
            int color = ContextCompat.getColor(itemView.getContext(), Acentos.color(e.nombre));
            int fondo = ContextCompat.getColor(itemView.getContext(), Acentos.fondo(e.nombre));
            b.icono.setImageTintList(ColorStateList.valueOf(color));
            b.insignia.setBackgroundTintList(ColorStateList.valueOf(fondo));
            b.nombre.setText(e.nombre);
            String meta = Fechas.fechaLarga(e.fecha, System.currentTimeMillis());
            if (!TextUtils.isEmpty(e.laboratorio)) meta += " · " + e.laboratorio;
            b.meta.setText(meta);

            boolean hay = !TextUtils.isEmpty(e.resultado);
            b.resultado.setText(hay ? e.resultado : itemView.getContext().getString(R.string.sin_resultado));
            b.resultado.setTextColor(MaterialColors.getColor(b.resultado, hay
                    ? com.google.android.material.R.attr.colorOnSurface
                    : com.google.android.material.R.attr.colorOnSurfaceVariant));

            if (it.cita != null) {
                b.vinculo.setVisibility(View.VISIBLE);
                b.vinculo.setText(itemView.getContext().getString(R.string.vinculado_a, it.cita));
                b.vinculo.setBackgroundTintList(ColorStateList.valueOf(fondo));
                b.vinculo.setTextColor(color);
                TextViewCompat.setCompoundDrawableTintList(b.vinculo, ColorStateList.valueOf(color));
            } else {
                b.vinculo.setVisibility(View.GONE);
            }
            itemView.setContentDescription(e.nombre + ", " + meta + ". " + b.resultado.getText());
            itemView.setOnClickListener(v -> alTocar.estudio(e));
        }
    }
}

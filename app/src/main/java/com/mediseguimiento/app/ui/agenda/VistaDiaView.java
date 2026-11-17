package com.mediseguimiento.app.ui.agenda;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;
import com.mediseguimiento.app.R;
import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.ui.common.Acentos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Vista de un día por horas, dibujada con Canvas: columna de horas, una línea por hora,
 * cada cita como un bloque de color cuya altura corresponde a su duración, citas encimadas
 * lado a lado ({@link DisposicionDia}) y una línea de la hora actual.
 */
public class VistaDiaView extends View {

    public interface AlTocar {
        void cita(Cita c);
        void hueco(int minutoDelDia);
    }

    private static final class Bloque {
        final Cita cita;
        final RectF rect = new RectF();
        final int color, fondo;
        Bloque(Cita cita, int color, int fondo) { this.cita = cita; this.color = color; this.fondo = fondo; }
    }

    private final float dp = getResources().getDisplayMetrics().density;
    private final float altoHora = 92 * dp, margenHoras = 64 * dp, arriba = 14 * dp, abajo = 18 * dp;
    private final Paint linea = new Paint(Paint.ANTI_ALIAS_FLAG), lineaMedia = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG), ahora = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textoHora = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint titulo = new TextPaint(Paint.ANTI_ALIAS_FLAG), detalle = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final List<Bloque> bloques = new ArrayList<>();
    private final GestureDetector gestos;

    private LocalDate dia = LocalDate.now(Fechas.zona());
    private List<Cita> citas = new ArrayList<>();
    private boolean h24;
    private int horaIni = 7, horaFin = 21;
    @Nullable private AlTocar alTocar;
    @Nullable private Bloque presionado;
    private final int colorInk, colorQuiet, colorCoral;

    private final Runnable tic = new Runnable() {
        @Override public void run() { invalidate(); postDelayed(this, 60_000); }
    };

    public VistaDiaView(Context c) { this(c, null); }

    public VistaDiaView(Context c, @Nullable AttributeSet a) {
        super(c, a);
        int grid = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant);
        colorInk = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface);
        colorQuiet = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant);
        colorCoral = ContextCompat.getColor(c, R.color.accent_prep);
        linea.setColor(grid); linea.setStrokeWidth(dp);
        lineaMedia.setColor(grid); lineaMedia.setStrokeWidth(dp); lineaMedia.setAlpha(110);
        lineaMedia.setPathEffect(new DashPathEffect(new float[]{4 * dp, 4 * dp}, 0));
        textoHora.setColor(colorQuiet); textoHora.setTextSize(sp(11.5f)); textoHora.setTextAlign(Paint.Align.RIGHT);
        titulo.setColor(colorInk); titulo.setTextSize(sp(13.5f)); titulo.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        detalle.setColor(colorQuiet); detalle.setTextSize(sp(11.5f));
        ahora.setColor(colorCoral); ahora.setStrokeWidth(2 * dp);
        setClickable(true);
        gestos = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public void onShowPress(MotionEvent e) { presionado = enPunto(e.getX(), e.getY()); invalidate(); }
            @Override public boolean onSingleTapUp(MotionEvent e) {
                Bloque b = enPunto(e.getX(), e.getY());
                presionado = null;
                invalidate();
                performClick();
                if (alTocar == null) return true;
                if (b != null) alTocar.cita(b.cita);
                else if (e.getX() > margenHoras) alTocar.hueco(minutoEn(e.getY()));
                return true;
            }
        });
    }

    private float sp(float v) { return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics()); }

    public void setAlTocar(@Nullable AlTocar l) { alTocar = l; }

    /** Citas del día (ya sin canceladas); recalcula el rango de horas visible. */
    public void setDatos(LocalDate dia, List<Cita> citasDelDia, boolean h24) {
        this.dia = dia;
        this.citas = new ArrayList<>(citasDelDia);
        this.h24 = h24;
        int ini = 7, fin = 21;
        for (Cita c : citas) {
            LocalDateTime a = Fechas.aLocal(c.fechaHora), z = Fechas.aLocal(c.fin());
            ini = Math.min(ini, a.getHour());
            int finHora = z.toLocalDate().isAfter(dia) ? 24 : z.getHour() + (z.getMinute() > 0 ? 1 : 0);
            fin = Math.max(fin, finHora);
        }
        horaIni = Math.max(0, ini);
        horaFin = Math.min(24, Math.max(fin, horaIni + 1));
        StringBuilder desc = new StringBuilder();
        for (Cita c : citas) desc.append(c.especialidad).append(", ")
                .append(Fechas.rangoHoras(c.fechaHora, c.fin(), h24)).append(". ");
        setContentDescription(desc.length() == 0 ? getContext().getString(R.string.calendario_sin_citas) : desc);
        requestLayout();
        acomodar();
        invalidate();
    }

    /** Posición vertical (px) a la que conviene desplazar: primera cita, o la hora actual si es hoy. */
    public int posicionInicial() {
        if (!bloques.isEmpty()) {
            float min = Float.MAX_VALUE;
            for (Bloque b : bloques) min = Math.min(min, b.rect.top);
            return (int) Math.max(0, min - altoHora * 0.5f);
        }
        if (dia.equals(LocalDate.now(Fechas.zona()))) {
            LocalDateTime t = LocalDateTime.now(Fechas.zona());
            return (int) Math.max(0, yDe(t.getHour() * 60 + t.getMinute()) - altoHora);
        }
        return (int) Math.max(0, yDe(9 * 60) - altoHora * 0.5f);
    }

    private float yDe(int minutoDelDia) { return arriba + (minutoDelDia - horaIni * 60) / 60f * altoHora; }

    private int minutoEn(float y) {
        int m = (int) ((y - arriba) / altoHora * 60) + horaIni * 60;
        m = Math.max(horaIni * 60, Math.min(horaFin * 60 - 30, m));
        return m / 30 * 30;
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int h = (int) (arriba + (horaFin - horaIni) * altoHora + abajo);
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), wSpec), h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) { acomodar(); }

    private void acomodar() {
        bloques.clear();
        int n = citas.size();
        if (n == 0 || getWidth() == 0) return;
        int[] ini = new int[n], fin = new int[n];
        for (int i = 0; i < n; i++) {
            Cita c = citas.get(i);
            LocalDateTime a = Fechas.aLocal(c.fechaHora), z = Fechas.aLocal(c.fin());
            ini[i] = a.toLocalDate().isBefore(dia) ? 0 : a.getHour() * 60 + a.getMinute();
            fin[i] = z.toLocalDate().isAfter(dia) ? 24 * 60 : z.getHour() * 60 + z.getMinute();
        }
        float x0 = margenHoras + 4 * dp, ancho = getWidth() - x0 - 12 * dp;
        for (DisposicionDia.Lugar l : DisposicionDia.calcular(ini, fin)) {
            Cita c = citas.get(l.indice);
            Bloque b = new Bloque(c, ContextCompat.getColor(getContext(), Acentos.color(c.especialidad)),
                    ContextCompat.getColor(getContext(), Acentos.fondo(c.especialidad)));
            float cw = ancho / l.columnas;
            float top = yDe(l.inicio) + dp, bottom = Math.max(yDe(l.fin) - dp, top + 30 * dp);
            b.rect.set(x0 + l.columna * cw + 2 * dp, top, x0 + (l.columna + 1) * cw - 2 * dp, bottom);
            bloques.add(b);
        }
    }

    @Nullable
    private Bloque enPunto(float x, float y) {
        for (int i = bloques.size() - 1; i >= 0; i--) if (bloques.get(i).rect.contains(x, y)) return bloques.get(i);
        return null;
    }

    private String etiquetaHora(int h) {
        if (h24) return String.format(Fechas.ES, "%02d:00", h);
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return h12 + (h < 12 || h == 24 ? " a. m." : " p. m.");
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth();
        // Rejilla de horas
        for (int h = horaIni; h <= horaFin; h++) {
            float y = yDe(h * 60);
            cv.drawLine(margenHoras, y, w - 8 * dp, y, linea);
            if (h < horaFin) {
                float ym = y + altoHora / 2;
                cv.drawLine(margenHoras + 4 * dp, ym, w - 8 * dp, ym, lineaMedia);
            }
            cv.drawText(etiquetaHora(h), margenHoras - 10 * dp, y + 4 * dp, textoHora);
        }
        // Bloques de citas
        float r = 10 * dp;
        for (Bloque b : bloques) {
            boolean completada = Cita.ESTADO_COMPLETADA.equals(b.cita.estado);
            relleno.setColor(b.fondo);
            relleno.setAlpha(completada ? 150 : 255);
            cv.drawRoundRect(b.rect, r, r, relleno);
            relleno.setColor(b.color);
            relleno.setAlpha(completada ? 150 : 255);
            cv.drawRoundRect(b.rect.left, b.rect.top, b.rect.left + 4 * dp, b.rect.bottom, 2 * dp, 2 * dp, relleno);
            if (b == presionado) {
                relleno.setColor(colorInk);
                relleno.setAlpha(28);
                cv.drawRoundRect(b.rect, r, r, relleno);
            }
            float tx = b.rect.left + 12 * dp, disp = b.rect.width() - 18 * dp;
            if (disp <= 8 * dp) continue;
            float alto = b.rect.height();
            float y1 = b.rect.top + (alto < 40 * dp ? alto / 2 + 5 * dp : 19 * dp);
            titulo.setColor(colorInk);
            cv.drawText(TextUtils.ellipsize(b.cita.especialidad, titulo, disp, TextUtils.TruncateAt.END).toString(), tx, y1, titulo);
            if (alto >= 40 * dp) {
                String rango = Fechas.rangoHoras(b.cita.fechaHora, b.cita.fin(), h24);
                cv.drawText(TextUtils.ellipsize(rango, detalle, disp, TextUtils.TruncateAt.END).toString(), tx, y1 + 17 * dp, detalle);
            }
            if (alto >= 60 * dp && !TextUtils.isEmpty(b.cita.medico)) {
                cv.drawText(TextUtils.ellipsize(b.cita.medico, detalle, disp, TextUtils.TruncateAt.END).toString(), tx, y1 + 33 * dp, detalle);
            }
        }
        // Línea de la hora actual
        if (dia.equals(LocalDate.now(Fechas.zona()))) {
            LocalDateTime t = LocalDateTime.now(Fechas.zona());
            int m = t.getHour() * 60 + t.getMinute();
            if (m >= horaIni * 60 && m <= horaFin * 60) {
                float y = yDe(m);
                cv.drawLine(margenHoras, y, w - 8 * dp, y, ahora);
                relleno.setColor(colorCoral);
                relleno.setAlpha(255);
                cv.drawCircle(margenHoras, y, 5 * dp, relleno);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean r = gestos.onTouchEvent(e);
        if (e.getAction() == MotionEvent.ACTION_CANCEL || e.getAction() == MotionEvent.ACTION_UP) {
            if (presionado != null) { presionado = null; invalidate(); }
        }
        return r || super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() { return super.performClick(); }

    @Override
    protected void onAttachedToWindow() { super.onAttachedToWindow(); postDelayed(tic, 60_000); }

    @Override
    protected void onDetachedFromWindow() { removeCallbacks(tic); super.onDetachedFromWindow(); }
}

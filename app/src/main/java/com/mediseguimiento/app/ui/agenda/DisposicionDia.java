package com.mediseguimiento.app.ui.agenda;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Acomoda los intervalos de un día en columnas para que las citas encimadas se dibujen
 * lado a lado (como en un calendario). Java puro: se prueba sin emulador.
 *
 * <p>Algoritmo: se ordenan por inicio; los intervalos que se traslapan entre sí (directa o
 * indirectamente) forman un grupo; dentro de cada grupo, cada intervalo ocupa la primera
 * columna libre. El ancho de un intervalo es 1 / (columnas de su grupo).</p>
 */
public final class DisposicionDia {

    /** Resultado de un intervalo: su columna y cuántas columnas tiene su grupo. */
    public static final class Lugar {
        public final int indice;      // posición en la lista original
        public final int inicio, fin; // minutos desde medianoche
        public int columna;
        public int columnas;

        Lugar(int indice, int inicio, int fin) {
            this.indice = indice;
            this.inicio = inicio;
            this.fin = fin;
        }
    }

    private DisposicionDia() {}

    /**
     * @param inicios minutos desde medianoche de cada cita
     * @param fines   minutos de término (se fuerza un mínimo de 15 min para que se vea)
     */
    public static List<Lugar> calcular(int[] inicios, int[] fines) {
        List<Lugar> lugares = new ArrayList<>();
        for (int i = 0; i < inicios.length; i++) {
            lugares.add(new Lugar(i, inicios[i], Math.max(fines[i], inicios[i] + 15)));
        }
        List<Lugar> orden = new ArrayList<>(lugares);
        orden.sort(Comparator.<Lugar>comparingInt(l -> l.inicio).thenComparingInt(l -> -l.fin));

        List<Lugar> grupo = new ArrayList<>();
        List<Integer> finesColumna = new ArrayList<>();
        int finGrupo = -1;
        for (Lugar l : orden) {
            if (!grupo.isEmpty() && l.inicio >= finGrupo) {
                cerrarGrupo(grupo, finesColumna.size());
                grupo.clear();
                finesColumna.clear();
            }
            int col = -1;
            for (int c = 0; c < finesColumna.size(); c++) {
                if (finesColumna.get(c) <= l.inicio) { col = c; break; }
            }
            if (col < 0) { col = finesColumna.size(); finesColumna.add(l.fin); }
            else finesColumna.set(col, l.fin);
            l.columna = col;
            grupo.add(l);
            finGrupo = Math.max(finGrupo, l.fin);
        }
        if (!grupo.isEmpty()) cerrarGrupo(grupo, finesColumna.size());
        return lugares;
    }

    private static void cerrarGrupo(List<Lugar> grupo, int columnas) {
        for (Lugar l : grupo) l.columnas = columnas;
    }
}

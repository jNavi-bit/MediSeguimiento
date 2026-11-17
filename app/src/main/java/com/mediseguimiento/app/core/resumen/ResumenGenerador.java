package com.mediseguimiento.app.core.resumen;

import com.mediseguimiento.app.core.Fechas;
import com.mediseguimiento.app.data.db.entity.Cita;
import com.mediseguimiento.app.data.db.entity.Estudio;
import com.mediseguimiento.app.data.db.entity.PreparacionConsulta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construye el resumen estructurado de una cita (RF-05) a partir de la información que
 * registró el paciente. No interpreta resultados ni sugiere diagnósticos (límites TC6).
 * Java puro: se prueba sin emulador.
 */
public final class ResumenGenerador {

    public static final String SIN_REGISTRAR = "Sin registrar";

    /** Datos de entrada, reunidos en paralelo por el repositorio. */
    public static final class Datos {
        public String paciente;
        public String padecimiento;
        public Cita cita;
        public PreparacionConsulta preparacion;
        public List<Estudio> estudiosVinculados = Collections.emptyList();
        public List<Estudio> estudiosRecientes = Collections.emptyList();
        public List<Cita> anteriores = Collections.emptyList();
    }

    public static final class Linea {
        public final String etiqueta;
        public final String valor;
        public final boolean vacia;

        Linea(String etiqueta, String valor) {
            boolean v = valor == null || valor.trim().isEmpty();
            this.etiqueta = etiqueta;
            this.valor = v ? SIN_REGISTRAR : valor.trim();
            this.vacia = v;
        }
    }

    public static final class Seccion {
        public final String id;
        public final String titulo;
        public final List<Linea> lineas = new ArrayList<>();

        Seccion(String id, String titulo) {
            this.id = id;
            this.titulo = titulo;
        }
    }

    public static final class Resumen {
        public final String titulo;
        public final String subtitulo;
        public final String generado;
        public final List<Seccion> secciones;
        public final int apartadosCompletos;
        public final String texto;

        Resumen(String titulo, String subtitulo, String generado, List<Seccion> secciones,
                int apartadosCompletos, String texto) {
            this.titulo = titulo;
            this.subtitulo = subtitulo;
            this.generado = generado;
            this.secciones = secciones;
            this.apartadosCompletos = apartadosCompletos;
            this.texto = texto;
        }
    }

    private ResumenGenerador() {}

    public static Resumen generar(Datos d, long ahora, boolean h24) {
        Cita c = d.cita;
        List<Seccion> secciones = new ArrayList<>();

        Seccion paciente = new Seccion("paciente", "Paciente");
        paciente.lineas.add(new Linea("Nombre", d.paciente));
        if (d.padecimiento != null && !d.padecimiento.trim().isEmpty()) {
            paciente.lineas.add(new Linea("Padecimiento en seguimiento", d.padecimiento));
        }
        secciones.add(paciente);

        Seccion consulta = new Seccion("consulta", "Consulta");
        consulta.lineas.add(new Linea("Especialidad", c.especialidad));
        consulta.lineas.add(new Linea("Fecha", Fechas.fechaLarga(c.fechaHora, ahora) + ", "
                + Fechas.horaCompleta(c.fechaHora, h24)));
        consulta.lineas.add(new Linea("Médico", c.medico));
        consulta.lineas.add(new Linea("Lugar", c.lugar));
        secciones.add(consulta);

        PreparacionConsulta p = d.preparacion;
        Seccion prep = new Seccion("preparacion", "Lo que quiero comentar");
        prep.lineas.add(new Linea("Motivo de la consulta", p == null ? null : p.motivo));
        prep.lineas.add(new Linea("Síntomas", p == null ? null : p.sintomas));
        prep.lineas.add(new Linea("Cambios recientes", p == null ? null : p.cambiosRecientes));
        prep.lineas.add(new Linea("Tratamientos que tomo", p == null ? null : p.tratamientos));
        prep.lineas.add(new Linea("Preguntas para mi médico", p == null ? null : enLista(p.preguntas)));
        secciones.add(prep);
        int completos = 0;
        for (Linea l : prep.lineas) if (!l.vacia) completos++;

        // Estudios: primero los vinculados a esta cita, luego los recientes, sin repetir.
        Map<Long, Estudio> estudios = new LinkedHashMap<>();
        for (Estudio e : d.estudiosVinculados) estudios.put(e.id, e);
        for (Estudio e : d.estudiosRecientes) estudios.putIfAbsent(e.id, e);
        Seccion est = new Seccion("estudios", "Estudios recientes");
        for (Estudio e : estudios.values()) {
            String valor = Fechas.fechaCorta(e.fecha)
                    + (e.laboratorio == null || e.laboratorio.trim().isEmpty() ? "" : " · " + e.laboratorio.trim())
                    + "\n" + (e.resultado == null || e.resultado.trim().isEmpty() ? SIN_REGISTRAR : e.resultado.trim());
            est.lineas.add(new Linea(e.nombre, valor));
        }
        if (!est.lineas.isEmpty()) secciones.add(est);

        Seccion ant = new Seccion("anteriores", "Consultas anteriores");
        for (Cita a : d.anteriores) {
            String valor = Fechas.fechaLarga(a.fechaHora, ahora)
                    + (a.medico == null || a.medico.trim().isEmpty() ? "" : " · " + a.medico.trim());
            ant.lineas.add(new Linea(a.especialidad, valor));
        }
        if (!ant.lineas.isEmpty()) secciones.add(ant);

        String titulo = "Resumen de seguimiento";
        String subtitulo = c.especialidad + " · " + Fechas.fechaCorta(c.fechaHora);
        String generado = "Generado el " + Fechas.fechaCorta(ahora) + ", " + Fechas.horaCompleta(ahora, h24);
        return new Resumen(titulo, subtitulo, generado, secciones, completos,
                aTexto(titulo, subtitulo, generado, secciones));
    }

    /** Convierte cada renglón de las preguntas en viñetas. */
    static String enLista(String texto) {
        if (texto == null || texto.trim().isEmpty()) return texto;
        StringBuilder sb = new StringBuilder();
        for (String l : texto.split("\\r?\\n")) {
            String t = l.trim().replaceFirst("^[-•*]\\s*", "");
            if (t.isEmpty()) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append("• ").append(t);
        }
        return sb.toString();
    }

    /** Versión en texto plano para compartir (RF-07). */
    static String aTexto(String titulo, String subtitulo, String generado, List<Seccion> secciones) {
        StringBuilder sb = new StringBuilder();
        sb.append(titulo.toUpperCase(Fechas.ES)).append('\n').append(subtitulo).append('\n')
                .append(generado).append("\n");
        for (Seccion s : secciones) {
            sb.append("\n— ").append(s.titulo).append(" —\n");
            for (Linea l : s.lineas) {
                sb.append(l.etiqueta).append(": ");
                sb.append(l.valor.contains("\n") ? "\n" + l.valor : l.valor).append('\n');
            }
        }
        sb.append("\nInformación proporcionada por el paciente mediante MediSeguimiento. ")
                .append("No sustituye el expediente clínico ni la valoración médica.");
        return sb.toString();
    }
}

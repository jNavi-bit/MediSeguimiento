# MediSeguimiento

Aplicación Android nativa de **agenda y seguimiento de consultas médicas** para pacientes con enfermedades crónicas o padecimientos prolongados. Proyecto Integrador de *Programación paralela y en tiempo real* (Universidad de Guadalajara, CUCEI).

> Herramienta de apoyo: organiza la información que registra el paciente. No emite diagnósticos ni sustituye el expediente clínico ni la valoración médica.

## Funciones
| Requerimiento | Implementación |
|---|---|
| RF-01 Registro e inicio de sesión | Cuenta local; contraseña con PBKDF2 + sal; bloqueo temporal tras 5 intentos |
| RF-02 Agenda de citas | Crear, editar, cancelar, reactivar y completar; tira de días y filtros |
| RF-03 Preparación de la consulta | Motivo, síntomas, cambios, tratamientos y preguntas (progreso 0–5) |
| RF-04 Estudios | Registro de estudios y resultados, vinculables a una cita |
| RF-05 Historial y resumen | Línea de tiempo cronológica y resumen estructurado por cita |
| RF-06 Recordatorios | Avisos locales con WorkManager (15 min, 1 h o 1 día antes) |
| RF-07 Compartir con consentimiento | Copiar/compartir solo tras autorización explícita, que queda registrada |
| RF-08 Corrección y eliminación | Edición y borrado con confirmación; "Deshacer" al cancelar |

## Tecnología
Java 17 · XML · Android Studio · minSdk 26 (Android 8.0) · Room/SQLite · ViewModel/LiveData · WorkManager · Material Components 3.

## Arquitectura y concurrencia
- **Capas:** presentación (`ui/`), lógica (`core/`), persistencia (`data/`: Room, DAOs y repositorios).
- **Hilos:** `AppExecutors` separa un grupo de E/S (Room) y uno de cómputo (resúmenes y agrupaciones); los resultados vuelven al hilo principal.
- **Paralelismo:** el resumen ejecuta 5 consultas en paralelo con `invokeAll`; el historial transforma citas y estudios en paralelo con `CompletableFuture`.
- **Consistencia:** transacciones de Room, índices únicos y control de concurrencia optimista (`version`) en las citas.

## Compilar y probar
```bash
./gradlew assembleDebug          # APK en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # pruebas unitarias
./gradlew connectedDebugAndroidTest  # pruebas de concurrencia (emulador o dispositivo)
```

## Equipo
Axel Javier Navor García · Diego Gabriel de la Mora Villaseñor · Melanie Montserrat López Galván

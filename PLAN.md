# PLAN.md

# MI MÚSICA

Aplicación Android para gestión y reproducción local de música.

---

# OBJETIVO

Crear una aplicación Android moderna que permita al usuario gestionar audio descargado legalmente, almacenarlo localmente y reproducirlo sin conexión.

---

# OBJETIVOS PRINCIPALES

- Interfaz moderna.
- Descarga de contenido permitido.
- Almacenamiento local.
- Biblioteca musical.
- Reproductor integrado.
- Reproducción offline.
- Background playback.
- Metadatos.
- Carátulas.
- Gestión de descargas.
- Dark mode.
- Arquitectura mantenible.
- Buen rendimiento.

---

# FASE 0 — ANÁLISIS DEL ENTORNO

Estado: ✅ Completada (análisis realizado; sin implementación)

Objetivos:

- [x] Revisar sistema operativo. → Windows 10 (10.0.19045), 64-bit.
- [x] Revisar JDK. → Android Studio instala JBR (OpenJDK 25.0.3). Hay JDK 8 (jre1.8.0_511) y "latest" en C:\Program Files\Java. JAVA_HOME NO está configurado; `java` no está en PATH.
- [x] Revisar Android SDK. → Encontrado en C:\Users\x\AppData\Local\Android\Sdk. ANDROID_HOME/ANDROID_SDK_ROOT NO están configurados. Licencias presentes (carpeta licenses).
- [x] Revisar Gradle. → No hay instalación global de Gradle ni binaries standalone. Será necesario usar Gradle Wrapper (descargado por AGP automáticamente al crear el proyecto).
- [x] Revisar Kotlin. → No hay compilador Kotlin standalone; Kotlin se gestionará vía versión del plugin en build.gradle.
- [x] Revisar Android Studio. → Instalado en C:\Program Files\Android\Android Studio. Build AI-262.9437.185.2621.16467767. Asociado a AGP interno 8.1.0 — examinar la versión real de AGP al crear el proyecto.
- [x] Revisar dispositivos/emuladores. → adb 37.0.1-15733141 detectado, sin dispositivo físico conectado. AVD disponible: `Medium_Phone_API_37.0` (API 37/Android 16), imagen google_apis_playstore x86_64. system-images API 37 presente. Emulador instalado en SDK.
- [x] Revisar skills disponibles. → Ver "Skills detectadas" más abajo.
- [x] Revisar estructura inicial del proyecto. → Ver "Estado del proyecto" más abajo.

Resultado del análisis:

## Herramientas disponibles

| Herramienta | Estado | Detalle |
|---|---|---|
| SO | ✔ | Windows 10 Pro 64-bit (10.0.19045) |
| JDK | ✔ | JBR OpenJDK 25.0.3 (Android Studio), JDK 8 en Program Files |
| Android SDK | ✔ | C:\Users\x\AppData\Local\Android\Sdk |
| Platforms | ✔ | android-37.0 solamente |
| Build-tools | ✔ | 36.0.0 |
| Platform-tools (adb) | ✔ | 37.0.1-15733141 |
| Emulador | ✔ | Instalado + AVD Medium_Phone_API_37.0 |
| Gradle | ✖ | No instalado globalmente (usar wrapper) |
| Kotlin | ✖ | No instalado standalone (usar plugin en Gradle) |
| Dispositivo físico | ✖ | Ninguno conectado a este momento |

## Skills detectadas

Disponibles en el entorno: `anatomy`, `find-skills`, `graphify`, `opencode`, `report`, `reverse-engineer-anything`.

UI/UX Pro Max: ✖ NO está instalada/accesible.
Graphity: ✖ NO está instalada/accesible. (Existe `graphify`, pero es para análisis/arquitectura de repositorios mediante grafo de conocimiento, NO para diagramas; no reemplaza la intención de Graphity.)

Acción sugerida para la próxima fase: usar `find-skills` para localizar y (si existe) instalar una skill de UI/UX de diagramas, o proceder sin ellas siguiendo principios Material 3 manualmente.

## Estado del proyecto (estructura actual)

```text
H:\Proyectos code\Donwloader\
├── AGENTS.md
├── PLAN.md
└── ARCHITECTURE.md
```

- NO existe aún ningún proyecto Android (no hay settings.gradle, build.gradle, src/, app/, gradle.properties, .idea/ ni código Kotlin/Java/XML).
- No se encontró código fuente que analizar.

---

# FASE 1 — DISEÑO Y ARQUITECTURA

Estado: ✅ Completada (diseño; sin código de producción)

Entregables:

- [x] Arquitectura en capas (Presentation/Domain/Data) y reglas de dependencia → ARCHITECTURE.md §3 (diagrama Mermaid).
- [x] Flujos claves: reproducción en segundo plano (§3.1), ciclo de vida de descarga con estados (§3.2).
- [x] Navegación, pantallas, estados y componentes reutilizables → DESIGN.md.
- [x] Permisos (notificaciones, mediaPlayback, MediaStore/SAF), interrupciones de audio y recuperación de estado → ARCHITECTURE.md §3.1, DESIGN.md §5.
- [x] Identificación de formato/calidad sin inflar la calidad real → ARCHITECTURE.md §12.1.
- [x] Plan de pruebas por nivel (unit, ViewModel, Room, repository, Compose UI, instrumentación) → DESIGN.md §7.
- [x] Justificación de cada dependencia del stack → ARCHITECTURE.md §2.1.
- [x] Compatibilidad JDK/Gradle/AGP verificada con documentación oficial → ARCHITECTURE.md §22 y §22.4.
- [x] Estrategia de fuentes legales y restricciones (incluido YouTube: sin vía autorizada confirmada = sin soporte) → ARCHITECTURE.md §22.2.
- [x] DI decidida: manual, con criterio de migración a Hilt → ARCHITECTURE.md §18.

Criterios de aceptación:

- Los tres documentos (AGENTS/PLAN/ARCHITECTURE + DESIGN.md) describen un diseño completo y coherente.
- Ninguna dependencia propuesta sin justificación.
- Ninguna API/versión inventada (AGP/Gradle/JDK verificados contra developer.android.com).
- No existe código de producción todavía (correcto por diseño de la fase).

Decisión pendiente (recordatorio): versiones exactas de Kotlin/targetSdk/minSdk se fijan al crear el proyecto en FASE 3.

---

# FASE 2 — SISTEMA UI/UX

Estado: ✅ Completada (especificación; sin código de producción)

Herramienta de diseño especializada: NO disponible. `find-skills` consultado el 2026-10-27: las skills Android/UX existentes tienen ≤ 2 instalaciones (no confiables); se siguió la especificación oficial de Material 3 manualmente.

Entregables:

- [x] Paleta light/dark + colores semánticos → DESIGN_SYSTEM.md §1.2
- [x] Estrategia Dynamic Color con fallback → DESIGN_SYSTEM.md §1.1
- [x] Colores semánticos por estado de descarga → DESIGN_SYSTEM.md §1.3
- [x] Tipografía y comportamiento con texto largo → DESIGN_SYSTEM.md §2
- [x] Espaciado, forma, elevación e iconografía → DESIGN_SYSTEM.md §3
- [x] Accesibilidad y tamaños táctiles (48 dp) → DESIGN_SYSTEM.md §4
- [x] Componentes reutilizables con spec visual → DESIGN_SYSTEM.md §5
- [x] Wireframes textuales de los 5 destinos + Player + Añadir audio + Detalle playlist → DESIGN_SYSTEM.md §6
- [x] Matriz de estados por pantalla (Loading/Empty/Error/Success/Processing) → DESIGN_SYSTEM.md §7
- [x] Reglas de calidad de audio honesta → DESIGN_SYSTEM.md §8
- [x] DESIGN.md actualizado (remite al sistema visual; sin duplicación)

Criterios de aceptación (FASE 2):

- DESIGN_SYSTEM.md define tokens suficientes para implementar cualquier pantalla sin decisiones visuales improvisadas.
- Toda pantalla de DESIGN.md tiene especificación visual + estados.
- No existe código de producción; ningún wireframe se marca como implementado.

Pantallas:

- [ ] Inicio.
- [ ] Biblioteca.
- [ ] Descargas.
- [ ] Reproductor.
- [ ] Ajustes.

---

# FASE 3 — PROYECTO ANDROID BASE

Estado: ⬜ Pendiente

- [ ] Crear proyecto.
- [ ] Configurar Gradle.
- [ ] Configurar Kotlin.
- [ ] Configurar Compose.
- [ ] Configurar Material 3.
- [ ] Configurar navegación.
- [ ] Crear estructura de paquetes.
- [ ] Crear tema.
- [ ] Compilar proyecto inicial.

---

# FASE 4 — BASE DE DATOS

Estado: ⬜ Pendiente

- [ ] Configurar Room.
- [ ] SongEntity.
- [ ] SongDao.
- [ ] AppDatabase.
- [ ] Repository.
- [ ] Migraciones.
- [ ] Tests.

---

# FASE 5 — ALMACENAMIENTO

Estado: ⬜ Pendiente

- [ ] Integrar MediaStore.
- [ ] Gestión de URI.
- [ ] Nombres de archivos.
- [ ] Detección de archivos eliminados.
- [ ] Control de espacio.
- [ ] Archivos temporales.

---

# FASE 6 — ANALIZADOR DE FUENTES

Estado: ⬜ Pendiente

- [ ] Validación de URL.
- [ ] Arquitectura de providers.
- [ ] Modelo de metadata.
- [ ] Manejo de errores.
- [ ] Estados de análisis.

Las fuentes utilizadas deben permitir la descarga del contenido.

---

# FASE 7 — DESCARGAS

Estado: ⬜ Pendiente

- [ ] Download manager.
- [ ] Cola.
- [ ] Estados.
- [ ] Progreso.
- [ ] Cancelación.
- [ ] Reintento.
- [ ] Errores.
- [ ] Background downloads.
- [ ] Persistencia.

---

# FASE 8 — PROCESAMIENTO DE AUDIO

Estado: ⬜ Pendiente

- [ ] Determinar formato fuente.
- [ ] Determinar bitrate.
- [ ] Selección de calidad.
- [ ] Conversión cuando sea necesaria.
- [ ] Metadatos.
- [ ] Carátulas.
- [ ] Archivos temporales.

Evitar conversiones innecesarias.

---

# FASE 9 — BIBLIOTECA

Estado: ⬜ Pendiente

- [ ] Lista de canciones.
- [ ] Búsqueda.
- [ ] Ordenamiento.
- [ ] Filtros.
- [ ] Eliminar canción.
- [ ] Información.
- [ ] Empty state.

---

# FASE 10 — REPRODUCTOR

Estado: ⬜ Pendiente

- [ ] Media3.
- [ ] Player.
- [ ] MediaSession.
- [ ] Play/pause.
- [ ] Seek.
- [ ] Siguiente.
- [ ] Anterior.
- [ ] +10 segundos.
- [ ] -10 segundos.
- [ ] Cola.
- [ ] Mini-player.

---

# FASE 11 — BACKGROUND PLAYBACK

Estado: ⬜ Pendiente

- [ ] Foreground playback.
- [ ] Notification.
- [ ] MediaSession.
- [ ] Lock screen controls.
- [ ] Bluetooth.
- [ ] Auriculares.

---

# FASE 12 — AJUSTES

Estado: ⬜ Pendiente

- [ ] Calidad predeterminada.
- [ ] Wi-Fi solamente.
- [ ] Almacenamiento.
- [ ] Tema.
- [ ] Temporales.
- [ ] Información de almacenamiento.
- [ ] Acerca de.

---

# FASE 13 — TESTING

Estado: ⬜ Pendiente

- [ ] Unit tests.
- [ ] Repository tests.
- [ ] ViewModel tests.
- [ ] Database tests.
- [ ] Download tests.
- [ ] Player tests.
- [ ] UI tests.

---

# FASE 14 — OPTIMIZACIÓN

Estado: ⬜ Pendiente

- [ ] Memoria.
- [ ] Batería.
- [ ] Rendimiento.
- [ ] Startup.
- [ ] Descargas.
- [ ] Reproducción.

---

# FASE 15 — RELEASE

Estado: ⬜ Pendiente

- [ ] Release configuration.
- [ ] Signing.
- [ ] ProGuard/R8 cuando corresponda.
- [ ] APK debug.
- [ ] APK release.
- [ ] Pruebas en dispositivo real.
- [ ] Documentación.

---

# ESTADO ACTUAL

Fase:

FASE 2 (completada como especificación). Próxima fase pendiente: FASE 3 (Proyecto Android Base).

Último cambio:

FASE 2 — Creado DESIGN_SYSTEM.md con: paleta light/dark de respaldo (semilla verde-musgo), estrategia dynamic color con fallback, colores semánticos por estado de descarga, escala tipográfica M3 con reglas de texto largo, escala de espaciado 4dp, shapes, elevación tonal, iconografía Material Icons, accesibilidad (targets 48 dp, content descriptions), 11 componentes reutilizables con spec visual, wireframes textuales de las 8 pantallas, matriz de estados por pantalla y reglas de calidad de audio honesta. `find-skills` consultado: ninguna skill de diseño Android confiable (≤2 instalaciones).

---

# REGLA

No marcar una tarea como completada simplemente porque se escribió el código.

Debe estar:

- implementada;
- compilando;
- probada;
- revisada.
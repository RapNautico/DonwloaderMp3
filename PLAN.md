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

Estado: 🟢 Compilación verificada. Instalación en emulador NO posible (falta aceleración por hipervisor).

Notas clave de esta fase:

- AGP 9.4 usa **Kotlin integrado** (built-in Kotlin): NO se aplica `org.jetbrains.kotlin.android`; se aplica `com.android.application` + `org.jetbrains.kotlin.plugin.compose` (versión alineada con KGP 2.2.10 que AGP usa internamente). Fuente: documentación "Migrate to built-in Kotlin" y compat de AGP 9.4/9.0 en developer.android.com.
- targetSdk fija 36 explícito (por defecto de AGP 9 hereda compileSdk; se decide conservador) y minSdk 26 propuesto.

Tareas verificadas:

- [x] Gradle Wrapper 9.6.0 generado y validado (`gradlew.bat --version`) con JBR 25.
- [x] `gradlew.bat assembleDebug` → BUILD SUCCESSFUL.
- [x] testDebugUnitTest → sin tests todavía (NO-SOURCE); no marcar como "probó cosas".
- [x] APK debug generado: `app/build/outputs/apk/debug/app-debug.apk` (~18.9 MB).
- [⚠] Instalación en emulador: NO completada — el hipervisor del emulador no está instalado en este equipo; se requiere instalar "Android Emulator hypervisor driver" (AEHD/WSL2 hypervisor) o un dispositivo físico. Pendiente de decisión del usuario (reinstalar herramientos está fuera del alcance de la fase).

Criterios de aceptación (FASE 3):

- Proyecto monomódulo Kotlin+Compose compila sin errores.
- Icono de app, `strings.xml` español, `local.properties` con SDK local.
- Sin secretos ni credenciales.
- Código base: Activity única + NavigationHost + 5 destinos; MiniPlayer VISUAL (botones deshabilitados, no simula reproducción).
- No se marcó como completada hasta verificar la compilación real (hecho).

Pendiente documentado:

- El hipervisor para emulador (o conectar dispositivo física).
- El package `com.example.mimusica` es provisional: cambiar si el usuario quiere un package propio.

Nota: el único warning persistente era el icono deprecado `QueueMusic`; ya corregido con la variante AutoMirrored.

---

# FASE 4 — BASE DE DATOS

Estado: 🟢 Implementada y verificada (compilación + 9/9 tests OK el 2026-10-27).

- [x] Configurar Room: **Room 3 (`androidx.room3`) 3.0.3** con KSP `2.2.10-2.0.2` (alineado al Kotlin integrado del AGP 9.4) y SQLite embebido `sqlite-bundled 2.7.1` con `BundledSQLiteDriver` (recomendación oficial Room KMP). Room Gradle plugin `androidx.room3` exporta el esquema a `app/schemas` (v1 generada).
- [x] SongEntity: PK = URI del contenido (mismo archivo importado N veces = 1 fila); índices en title/artist; metadatos opcionales nullable (mimeType, sizeBytes, bitrateBps) — null = desconocido, nunca inventado (AGENTS §8).
- [x] SongDao: insert (OnConflict.IGNORE), update, delete, getByUri, búsqueda LIKE case-insensitive título/artista, observación `Flow` ordenada por título.
- [x] AppDatabase: `@Database(version 1, exportSchema)`; builder con `Room.databaseBuilder(name) + setDriver(BundledSQLiteDriver())`.
- [x] Repository: `SongRepositoryImpl` (Data) implementa `domain/repository/SongRepository`; mapeo explícito Entity<->Song; Domain libre de Room.
- [x] DI manual: `AppContainer` + `MiMusicaApplication` (sin Hilt). Pendiente conectar ViewModels en FASE 5+.
- [x] Tests: `SongRepositoryTest` — JVM puro SIN Robolectric ni emulador, con la MISMA configuración de producción (Room + BundledSQLiteDriver + archivo temporal). Cubre: inserción y consulta por id, URI inexistente→null, duplicados (IGNORE), update, delete, búsqueda por título/artista (case-insensitive), emisión reactiva del Flow (ordenado), y persistencia reabriendo el archivo. **Resultado: 9 tests, 0 fallos, 0 errores.**

Decisiones y problemas reales resueltos en esta fase:

1. Doc oficial (2026-10-23/21) introduce `androidx.room3` (Room 3.0.3 como estable recomendado) — se usa esa línea, no `androidx.room` 2.8.5.
2. El accesor Kotlin DSL `room { schemaDirectory(...) }` del plugin Room no se genera con built-in Kotlin de AGP 9.4 → se configura la extensión explícitamente: `configure<androidx.room3.gradle.RoomExtension> { schemaDirectory(...) }`.
3. KSP añade fuentes vía `kotlin.sourceSets` y built-in Kotlin lo bloquea → propiedad oficial indicada por el propio AGP: `android.disallowKotlinSourceSets=false` en gradle.properties.
4. En tests JVM, el driver embebido de la variante Android intenta cargar una `.so` nativa de Android → `testRuntimeOnly("androidx.sqlite:sqlite-bundled-jvm")` (variante desktop/JVM del mismo driver) y el mismo código de test funciona.
5. Warning JVM (no bloqueante): `System::loadLibrary` restringido en JDK 25 al cargar SQLite nativa; también "Kotlin no soporta target 25, degrada a 24" en compilación de tests. Sin impacto funcional; revisar en próximas versiones de Kotlin.

Pendiente dentro de la fase: connectar el repositorio a UI cuando la biblioteca importe canciones (FASE 5/7). Migraciones: no aplica todavía (esquema v1 inicial).

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

FASE 3 — Compilación real verificada.

Último cambio:

FASE 3 — Verificación final completa (2026-10-27):
- `gradlew.bat --version`: Gradle 9.6.0, Launcher/Daemon JVM = JBR OpenJDK 25.0.3 (`C:\Program Files\Android\Android Studio\jbr`).
- `gradlew.bat assembleDebug` → **BUILD SUCCESSFUL** (segunda pasada, 36 tareas up-to-date tras la primera compilación de 3m48s).
-`:app:testDebugUnitTest` → NO-SOURCE (no hay tests unitarios todavía; correcto en esta fase).
- Versiones realmente en uso (fuentes oficiales consultadas el 2026-10-27 en developer.android.com):
  - AGP: **9.4.0** (requiere Gradle ≥ 9.6.0, JDK ≥ 17, soporta hasta API 37 — coincide con el SDK instalado).
  - Gradle: **9.6.0** (wrapper, `distributionUrl=gradle-9.6.0-bin.zip`).
  - Kotlin: **integrado en AGP 9.x** (built-in Kotlin; KGP interno ≥ 2.2.10; NO se aplica `kotlin-android`). `gradle.bat --version` reporta Kotlin 2.3.21 para el propio Gradle (Groovy DSL del wrapper, distinto del KGP del proyecto).
  - Compose Compiler plugin: `org.jetbrains.kotlin.plugin.compose` versión **2.2.10** (alineado con el KGP mínimo requerido por AGP 9.0; fuente: "Migrate to built-in Kotlin" + "Setup Compose dependencies and compiler" en developer.android.com).
  - Compose BOM: **2026.09.00** (recomendación vigente; exige compileSdk 37 + AGP 9 — verificado en la misma fuente).
  - `compileSdk = 37`, `targetSdk = 36` (explícito, conservador), `minSdk = 26` (propuesto, sujeto a validación).
- APK debug EXISTE: `H:\Proyectos code\Donwloader\app\build\outputs\apk\debug\app-debug.apk` (18.86 MB).
- `.gitignore` verificado: excluye `build/`, `local.properties`, APKs, keystores y secretos; conserva `gradle/wrapper/gradle-wrapper.jar` (negación explícita) y el código fuente.
- Emulador: SIGUE no disponible — el hipervisor de aceleración no está instalado; queda PENDIENTE (no se instalaron drivers ni se modificó Windows/BIOS). Alternativa: conectar un dispositivo físico para instalar/apertura del APK.

Errores encontrados y corregidos durante la fase (compilaciones fallidas previas):
1. `stringResource` usado dentro de bloque `semantics {}` no-composable (EmptyState/MiniPlayer) → se resolvió el string fuera del bloque.
2. `Column` sin importar e import de `Color.Transparent` inexistente en HomeScreen → imports corregidos a `Column`/`Color.Transparent`.
3. Icono `QueueMusic` deprecado → reemplazado por `Icons.AutoMirrored.Filled.QueueMusic`.

Estado: la base COMPILA y el APK existe. FASE 3 marcada como completada con la salvedad documentada: la instalación/ejecución en emulador queda pendiente por falta de aceleración (requiere decisión del usuario).

---

# REGLA

No marcar una tarea como completada simplemente porque se escribió el código.

Debe estar:

- implementada;
- compilando;
- probada;
- revisada.
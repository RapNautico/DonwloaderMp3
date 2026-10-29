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

# FASE 5 — IMPORTACIÓN Y DESCUBRIMIENTO DE AUDIO LOCAL

Estado: 🟢 Implementada y verificada (2026-10-27): `assembleDebug` → BUILD SUCCESSFUL; `testDebugUnitTest` → 19/19 tests OK (0 fallos). Verificación en dispositivo/emulador PENDIENTE (sin hipervisor).

Implementado (requisitos §1-§11):

- [x] Selección SAF: `ActivityResultContracts.OpenMultipleDocuments` con `audio/*` (sin permisos de almacenamiento amplios; mínimo privilegio). Cancelar no produce errores (URIs vacías se ignoran).
- [x] Permisos persistibles: `takePersistableUriPermission` se pide en el callback del picker (actividad viva); `SecurityException` (provedor sin persistencia) se ignora y se documenta: Room conserva metadatos; la portada ya está copiada a filesDir.
- [x] Formatos: MP3, AAC, M4A, FLAC, WAV, OGG, OPUS (`domain/model/AudioFormat.kt`). Validación por el MIME REAL de la pista (MediaExtractor), nunca solo por extensión. Filetypes no soportados → reject + mensaje + el resto del lote continúa.
- [x] Metadatos REALES: MediaMetadataRetriever (título/artista/álbum/duración/bitrate/portada) + MediaExtractor (mime, sampleRate, canales). null = desconocido. Fallback del título: retriever → displayName → "Audio sin título" (documentado).
- [x] Duplicados: PK Room = URI (FASE 4); el informe distingue "añadidas" vs "ya estaban".
- [x] Persistencia de portada: copia única atómica a `filesDir/artwork/` (hash MD5 uri+vsize); sin depender del permiso; sin decodificar Bitmap en importación; placeholder mientras no exista.
- [x] Arquitectura: Presentation → Domain → Data: `ImportAudioUseCase` (Domain, puro) + `LocalAudioImporter` (interfaz Domain con `PreparedAudio`/`ImportFailureReason`/`ImportReport`) + impl `SafAudioImporter` y `AudioMetadataReader`/`ArtworkStore` (Data). La UI no ve el DAO.
- [x] UI Library real: `LibraryScreen` (lista con portada placeholder/artwork, duración, artista·álbum), EmptyState con CTA "Añadir música", banner de progreso "Importando 3 de 8", banner resultado "N añadidas / ya estaban / no se pudieron importar M".
- [x] Concurrencia: lecturas en `Dispatchers.IO`; estado de importación vía StateFlow (`ImportState.Idle|Running|Done|Failed`); `Mutex` impide importaciones concurrentes (las segundas se ignoran).
- [x] Seguridad §10: URI inválida/sin scheme, archivo inexistente, permiso revocado, sin pista de audio, IOException, guardia de tamaño (512 MB, `TOO_LARGE`): jamás cierra la app, siempre continúa el lote.
- [x] Tests JVM ejecutados (19):
  - `SongRepositoryTest` (Room, JVM con BundledSQLiteDriver, sin Robolectric): 9 — insert/get/null/ duplicado/ update/ delete/ búsqueda título+artista/ Flow/ persistencia (más verificación del conteo de nuevas).
  - `AudioFormatTest`: 3 — formatos mínimos, mimes con parámetros, no-audio/unknown → null.
  - `ImportAudioUseCaseTest`: 7 — válido, no compatible, metadata incompleta (nulls), duplicado reimport, lote múltiple con progreso, fallo sin detener el lote, URIs pasadas tal cual.
- [x] Room v1→v2: migración suspend con `SQLiteConnection.prepare/step` (3 columnas nuevas); esquema v2 exportado a `app/schemas`. ADVERTENCIA: aún no hay test de migración 1→2 (requiere room3-testing con driver, pendiente validación con dispositivo). NO existe migración 2→3 aún — el esquema v1 se exporta como archivo JSON para referencia.

Límites conocidos:

- NO probado en dispositivo real (sin emulador): comportamiento del picker, permisos persistibles con proveedores reales, archivos reales MP3/FLAC/etc.
- El artwork NO se muestra aún en la lista (placeholder del design system); la copia está lista para el jugador/miniaturas de FASE 10.
- Bitrate real vía MediaMetadataRetriever puede no informarlo todo proveedor: queda null y la UI no muestra nada (AGENTS §8).

---

# FASE 6 — ANALIZADOR DE FUENTES

Estado: 🟡 Análisis completado (2026-10-27), sin implementación — esperando aprobación del usuario para FASE 7.

Entregado (documentación, sin código):

- [x] Matriz de capacidades por fuente en ARCHITECTURE.md §22.9: proveedor/tipo, API oficial, operaciones autorizadas, restricciones, autenticación, formatos/metadata, enlaces oficiales, viabilidad e incertidumbres.
- [x] Fuentes verificadas con documentación oficial leída el 2026-10-27: Jamendo (API v3.0 docs + endpoint `/v3.0/tracks/file` con `audiodownload_allowed`), Internet Archive (portal developers), Wikimedia Commons (Commons:API), SoundCloud (API Terms of Use §05/§10: descarga PROHIBIDA).
- [x] Fuentes clasificadas sin vía oficial de descarga: YouTube/YouTube Music y Spotify (solo metadata/reproducción; por re-verificar ToS exactos antes de FASE 7).
- [x] Fuentes sin verificar marcadas como "Desconocida": Musopen, FMA, etc.
- [x] Fuente local: SAF ya implementado (FASE 5); MediaStore identificado como complemento viable de alta viabilidad.
- [x] Arquitectura futura PROPUESTA (no implementada): contrato `MusicSource` en Domain (search/metadata/capabilities/download) + `SourcePipeline` + implementaciones en Data + registro DI en AppContainer, sin acoplarse a la UI ni a Room.
- [x] Cero cambios de código, dependencias o versiones en esta fase.

Las fuentes utilizadas deben permitir la descarga del contenido: solo se integrarán fuentes con operación autorizada oficialmente (regla §22.2).

---

# FASE 7 — DESCARGAS

Estado: 🟡 PAUSADA (2026-10-28)

Implementado y CONSERVADO (archivos de 7.0 aún sin trackear en git; listos para commit cuando el usuario lo ordene):

- **7.0 — Contratos de dominio**, intactos y con sus 18 tests (37/37 JVM el 2026-10-27):
  - `domain/source/`: `SourceCapabilities` (canSearch/canDownload/attributionText), `TrackReference` (con `downloadAllowed: Boolean?` por recurso: null = desconocido), `SourceMetadata` + `DownloadFormatSpec` (formatos declarados por la fuente), `evaluateDownloadAuthorization()` → `GRANTED|DENIED_BY_SOURCE|DENIED_BY_TRACK|UNKNOWN`.
  - `domain/download/`: `DownloadState` (terminología de DESIGN §3.4; transiciones válidas explícitas; Completed/Failed/Cancelled terminales) y `DownloadQueue` (FIFO, máx. 2 concurrentes, cancelación pendiente/activo con confirmación de motor).
- Decisión formalizada: la autorización de descarga se evalúa POR RECURSO; la capacidad general de una fuente NO habilita pistas concretas.

Razones de la pausa: el motor de descargas exige decidir antes, con evaluación seria: procesamiento local en Android, motor HTTP (HttpURLConnection vs OkHttp), ejecución en segundo plano (WorkManager / User-Initiated Data Transfer / foreground service), permisos y restricciones de Android, y aspectos legales/condiciones de cada proveedor (incluida NewPipe Extractor: licencia y condiciones requieren análisis propio antes de considerar su uso).

Alternativas futuras a evaluar al RETOMAR la fase (nada decidido, sin dependencias introducidas):

- Fuentes de vía oficial verificada (§22.9): Jamendo / Internet Archive / Wikimedia Commons — plan por entregas 7.0–7.6 ya redactado en la planificación de FASE 7.
- Motor HTTP: HttpURLConnection (0 dependencias) vs OkHttp (dependencia: justificar APK/compatibilidad/mantenimiento).
- Segundo plano: WorkManager vs UIDT vs foreground service — decisión pendiente; cualquier dependencia nueva requiere aprobación EXPLÍCITA del usuario.
- Extractores tipo NewPipe Extractor: solo tras evaluar su licencia, sus condiciones de uso y la legalidad de cada proveedor objetivo. Sin bypass de DRM/autenticación (AGENTS §12).

Al retomar: re-verificar la documentación oficial vigente de cada proveedor, reutilizar los contratos 7.0 tal cual, y continuar por la entrega 7.1 del plan por entregas aprobado.

---

# FASE 8 — BIBLIOTECA COMPLETA (fase activa)

Estado: 🔵 En curso — 8.0 documentado y AUTORIZADO (2026-10-28); implementación por entregas desde 8.1.

## Decisiones autorizadas (8.0, 2026-10-28)

1. **Género**: campo nullable `genre` en `Song`/`SongEntity`; se extrae ÚNICAMENTE cuando `MediaMetadataRetriever.METADATA_KEY_GENRE` (API oficial 20+) devuelve un valor real; no se infiere ni inventa; `null` = desconocido → etiqueta "Desconocido" en UI. Entra en la migración Room **v2→v3**.
2. **Migración v3**: `ADD COLUMN genre TEXT` + índices de **album** y **dateAddedMs** — justificados EXCLUSIVAMENTE por las consultas previstas (navegación por álbumes, orden/recientes). Esquema v3 exportado a `app/schemas`.
3. **Eliminación**: "Eliminar de biblioteca" borra SOLO el registro de Room. El archivo físico NO se elimina jamás automáticamente; la acción "Eliminar también el archivo" queda SEPARADA y NO se implementa en FASE 8 (requerirá ConfirmDialog y aprobación explícita del usuario).
4. **Canciones inválidas**: nunca se eliminan registros automáticamente; identificadas bajo demanda (no en cada recomposición) con badge/etiqueta comprensible; los errores de acceso nunca crashean la app.
5. **Datos reales** regla mantenida (AGENTS §8): null → "Desconocido" en UI; el MIME real (MediaExtractor) sigue siendo fuente de verdad del formato.
6. **Arquitectura**: Presentation → Domain → Data → Room/Android; la UI no toca DAO; Domain sin Android. Play/Playlist quedan como callbacks/contratos preparados (sin Media3 ni motor real).
7. **FASE 7 sigue PAUSADA**: `domain/source/*` y `domain/download/*` intocables y sin contaminar la biblioteca.
8. **Dependencias**: cero añadidas automáticamente. Si 8.9 necesita `kotlinx-coroutines-test` u otra, se solicita aprobación explícita con justificación (por qué/problema/APK/alternativa/arquitectura) ANTES de agregarla.

## Entregas

- [x] **8.0 — Auditoría y diseño**: auditoría de Song/Room/Repository/UI/navegación completa (2026-10-28): verificado que Song tiene uri/título/artista/álbum/duración/dateAddedMs/mime/tamaño/bitrate/sampleRate/canales/artworkPath; faltaban genre (decidido incluir) y lastPlayedMs (posponer a Reproductor); Room v2 con índices title/artist; Repository sin búsquedas por álbum/género, recientes ni agrupados; UI solo esqueleto (sin búsqueda/orden/filtros/detalle/gestión); navegación plana de 5 destinos sin rutas anidadas.
- [x] **8.1 — Modelo + Room v3**: `genre` nullable en Song/SongEntity (D8.1); migración v2→v3 suspend prepare/step (ADD genre TEXT + índices album/dateAddedMs); extracción real vía METADATA_KEY_GENRE (blank → null, `normalizeOptionalText`); mappers completos (fix latente: sampleRateHz/channels/artworkPath no se persistían); schema v3 exportado; 45/45 tests OK (8 nuevos).
- [x] **8.2 — Consultas Room + Repository**: búsqueda por título/artista/álbum (parcial, ASCII case-insensitive, comodines literal con ESCAPE, trim, vacío=biblioteca completa); recientes (`dateAddedMs DESC` + desempate uri, LIMIT); por artista/álbum (null = bucket desconocido); `observeArtistSummaries`/`observeAlbumSummaries` (GROUP BY en SQL, sin N+1); `ArtistSummary`/`AlbumSummary` en Domain; orden secundario estable por uri. 8.2 = 69/69 tests OK.
- [x] **8.3 — Búsqueda + orden**: reactiva (Flow→StateFlow, debounce 500 ms — decisión usuario 2026-10-29, sustituye los 300 ms del wireframe); cancelación de resultados obsoletos (flatMapLatest); toma en cuenta vacía/espacios/sin resultados; banner de ERROR de búsqueda (errorContainer, conserva la lista previa, se limpia con el siguiente éxito); ordens título A-Z/Z-A, artista, álbum, recientes/antiguas, duración; orden en estado de UI (SIN DataStore: persistencia de preferencias espera Fase 11 con aprobación). Lógica en `LibraryListModel` testeable (7 pruebas de debounce/resultados/errores en tiempo real).
- [ ] **8.4 — Artistas + álbumes**: rutas anidadas sin tocar la bottom bar; buckets "Artista desconocido"/"Álbum desconocido" para nulls; reutilización de componentes.
- [ ] **8.5 — Detalle de canción**: bottom sheet M3 con datos REALES; null → "Desconocido"; sin inventar bitrate/calidad/codec.
- [ ] **8.6 — Gestión**: menú contextual por canción: reproducir (callback preparado para 9), añadir a playlist (para 10), información, eliminar de biblioteca (solo registro, con ConfirmDialog).
- [ ] **8.7 — Inválidos**: URI inaccesible/borrado/corrupto identificados bajo demanda con estado visible; sin crash y sin auto-eliminación.
- [ ] **8.8 — UI Library completa**: SearchBar, Tabs (Canciones/Artistas/Álbumes), FAB importar, SongListItem spec DESIGN_SYSTEM §5, ErrorState + esqueleto (no spinner global), badges 48dp/a11y.
- [ ] **8.9 — Tests**: repository/DAO (listar/buscar/ordenar/recientes/artistas/álbumes/eliminación lógica), lógica pura (nulos/inválidos/duplicados/vacía), ViewModel si coroutines-test es aprobada.
- [ ] **8.10 — Validación final**: assembleDebug + testDebugUnitTest + diff --check + revisión de schema v3 + informe.

Criterios de aceptación de la fase: biblioteca completa funcional sin red; 0 dependencias nuevas (salvo aprobación explícita en 8.9); sin datos inventados; contratos de 9/10 preparados sin implementar; migración v3 probada en JVM; pruebas de dispositivo siguen pendientes sin emulador.

---

# FASE 9 — REPRODUCTOR

Estado: ⬜ Pendiente

Objetivo: reproducción local completa. EVALUAR Media3/ExoPlayer ANTES de implementar: `androidx.media3` NO está en el classpath; agregarlo exige justificación (impacto APK, compatibilidad, mantenimiento) y tu aprobación explícita.

Arquitectura obligatoria (la UI nunca controla el reproductor subyacente):

```text
UI → PlayerViewModel → PlayerController/PlaybackManager → Media3 → archivo local
```

## 9.1 Reproducción

- [ ] Play/pausa · anterior · siguiente · seek · ±10 s · duración · progreso · volumen.
- [ ] Artwork real (copia de FASE 5) con placeholder del design system si no hay.

## 9.2 Cola y modos

- [ ] Cola de reproducción (INDEPENDIENTE de la cola de descargas de 7.0; solo concepción no compartida), shuffle, repeat, repeat-one, comportamiento al finalizar la cola.

## 9.3 Fondo, sesión y energía

- [ ] MediaSession · reproducción en segundo plano · notificación multimedia · lock screen · auriculares/Bluetooth · pérdida de foco → pausa (ARCHITECTURE §3.1).
- [ ] Recuperación de estado tras muerte del proceso (cola + posición).
- Pendiente de decisión: MediaSessionService de Media3 vs servicio propio; documentado antes de implementar.

Criterios de aceptación: Domain sin conocimiento de Media3 (control abstraído); ViewModel solo transmite estado; compilación y tests OK; instrumentadas pendientes si no hay dispositivo.

---

# FASE 10 — PLAYLISTS

Estado: ⬜ Pendiente

Modelo (m:n con orden independiente por lista):

```text
Playlist ──< PlaylistSong >── Song
```

- Room: tablas `playlists` y `playlist_songs` (playlistId, songUri, position): una canción en varias listas; una lista con muchas canciones; el orden es propio de cada lista.
- Funciones: crear, renombrar, eliminar, añadir/quitar canciones, reordenar, reproducir la lista completa o desde una canción concreta, mostrar cantidad, artwork representativo (portada de la primera canción; nunca inventado).
- Migración Room a la SIGUIENTE versión libre (esquema actual v2; la v3 planeada para descargas quedó pausada con su fase — aquí se usará la numeración que toque al implementar).
- Diseñar antes de implementar; FK/ON DELETE y posicionamiento final se documentarán en ARCHITECTURE.md al implementar.

---

# FASE 11 — AJUSTES

Estado: ⬜ Pendiente

- Apariencia: Sistema · Claro · Oscuro · Dynamic Color (evaluar soporte real con el stack actual).
- Biblioteca: orden predeterminado · vista lista/cuadrícula · comportamiento al eliminar (decisión 8.3) · actualización/escaneo.
- Reproducción: reanudar última canción · reproducción automática · shuffle/repeat predeterminados · comportamiento al finalizar.
- Audio: SOLO opciones realmente implementables — configuraciones ficticias prohibidas.
- Almacenamiento: ubicación · espacio utilizado · archivos temporales · información.
- Acerca de: versión · información de la app · licencias de terceros.
- Preferencias sencillas: DataStore CUANDO CORRESPONDA (dependencia nueva → requiere justificación + aprobación explícita; sin añadir ahora).

---

# FASE 12 — INTEGRACIÓN FINAL

Estado: ⬜ Pendiente

Flujos extremo a extremo que deben funcionar:

```text
Importar (SAF) → Biblioteca → Reproducir → MiniPlayer → Player → Cola → Playlist
Biblioteca → Añadir a playlist → Playlist → Reproducir → Player
Settings → Preferencias → Aplicación
```

- Verificación de que los cambios de configuración afectan REALMENTE a la app (nada decorativo).
- Criterio de compatibilidad con descargas futuras: una canción descargada debe entrar a la biblioteca por el mismo mecanismo que una importada (misma entidad `Song`, mismo `SongRepository.addSongs`); los contratos 7.0 ya apuntan ahí (`DownloadRequest` → archivo local → registro). Durante FASE 8–11 debe perderse ninguna capacidad ni acoplarse nada que impida conectar el motor de descargas después.

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

FASE 8 — Biblioteca completa: 8.0 AUDITORÍA/DISEÑO AUTORIZADO y documentado (2026-10-28). Implementación por entregas 8.1→8.10 autorizada (ver sección FASE 8 para las decisiones aprobadas: género real + migración v3 + índices album/dateAddedMs + eliminación solo-registro + inválidos identificados bajo demanda). FASE 7 🟡 PAUSADA (contratos 7.0 committeados en `84c687c`, intocables). FASE 5 verificada (37/37 tests + assembleDebug OK).

Último cambio:

FASE 8.0 — Documentación autorizada: PLAN.md FASE 8 (decisiones + entregas) y ARCHITECTURE.md §22.11 (modelo de biblioteca, migración v3, eliminación lógica/física, contratos para 9/10). Sin código.

Roadmap replanificado (2026-10-28):

- FASE 7 — DESCARGAS: 🟡 PAUSADA (retomar tras evaluar motor HTTP, segundo plano WorkManager/UIDT, NewPipe Extractor y condiciones legales).
- FASE 8 — BIBLIOTECA COMPLETA: ⬜ próxima activa.
- FASE 9 — REPRODUCTOR (Media3 prevista, pendiente de evaluación y aprobación de dependencia).
- FASE 10 — PLAYLISTS (m:n con orden; migración Room a versión libre).
- FASE 11 — AJUSTES (DataStore cuando corresponda; aprobación previa).
- FASE 12 — INTEGRACIÓN FINAL (los flujos importar→reproducir→playlist y compatibilidad con descargas futuras).
- FASE 13 TESTING / 14 OPTIMIZACIÓN / 15 RELEASE: intactas tras la 12.

---

# REGLA

No marcar una tarea como completada simplemente porque se escribió el código.

Debe estar:

- implementada;
- compilando;
- probada;
- revisada.
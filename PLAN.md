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
- [x] **8.4 — Artistas + álbumes**: rutas ANIDADAS dentro del grafo `library` (bottom bar intacta y sigue resaltada por hierarchy; el listado pasa a `library/songs` y navegar a `library` abre el start del grafo); entradas en la top bar de Biblioteca (las Tabs definitivas llegan en 8.8); Artistas/Álbumes REACTIVOS con los resúmenes SQL de 8.2 (`observeArtistSummaries`/`observeAlbumSummaries`, GROUP BY, sin N+1) mostrando nombre + nº de canciones REALES (plurales); detalle de grupo navegable con las canciones (orden SQL definido en 8.2: getByArtist/getByAlbum, carga única al abrir); bucket desconocido (name null del DOMAIN) navegable de primera clase → etiqueta "Artista/Álbum desconocido" SOLO en presentación; nombres codeados como URI en la ruta (espacios, "/", "&" llegan intactos); componente SongRow/SongList extraídos a `components/SongListItems.kt` (reutilización); lógica en `GroupSummariesModel`/`GroupDetailModel` testeables (8 pruebas: reactividad, bucket null, nombres especiales, errores sin crashear, estado vacío). Cero dependencias nuevas, cero cambios de esquema.
- [x] **8.5 — Detalle de canción**: bottom sheet M3 (`ModalBottomSheet`) como DESTINO `library/song?uri={uri}` en el grafo `library`; abre desde biblioteca y detalles de artista/álbum (fila clicable opcional en `SongRow`, reutilización); datos REALES de `Song` (título, artista, álbum, género, duración, formato/MIME real, tamaño, bitrate medido, muestreo, canales, fecha añadido); `null` del Domain → "Desconocido" SOLO en la UI (sin inventar bitrate/calidad/codec); estados Loading/NotFound/Error sin crash (archivo borrado de la biblioteca → NotFound; detección de archivo inválido es la 8.7); contrato existente `getSong(uri)` (8.2), SIN esquema Room nuevo ni dependencias; lógica en `SongDetailModel` testeable (8 pruebas: carga intacta con nulls, NotFound, uri nula, error, helpers de formato con punto decimal ROOT).
- [x] **8.6 — Gestión**: menú contextual "⋮" en cada fila de la BIBLIOTECA (`SongMenuButton`, FASE 8.6 activo solo ahí según plan 8.8): Reproducir (callback POR REFUERZO para 9, ítem inoperante), Añadir a playlist (callback POR REFUERZO para 10, ítem inoperante), Información (abre el detalle 8.5) y Eliminar de biblioteca — `AlertDialog` OBLIGATORIO antes de eliminar ("El archivo no se borra del dispositivo"); eliminación SOLO del registro Room vía contrato existente `deleteSong(uri)` (D8.3), UI sincronizada por los Flows reactivos existentes (biblioteca, búsqueda, artistas/álbumes re-emiten solos); una eliminación a la vez (busy), error → banner errorContainer sin borrar la lista (limpio con próximo éxito); lógica en `LibraryManagerModel` (5 pruebas JVM: URI real registrada, error sin crash, error limpiado por éxito, busy observable, segunda concurrente ignorada). Cero dependencias nuevas, cero cambios de esquema.
- [x] **8.7 — Inválidos**: acción «Comprobar archivo» por canción en el menú de biblioteca; abre el descriptor SAF y comprueba pista/formato con `MediaExtractor` en `Dispatchers.IO`; estados visibles disponible/ausente/sin acceso/no válido, sin auto-eliminar ni borrar archivos. Estado por URI solo en presentación; Room v3, permisos y dependencias sin cambios. 6 pruebas JVM nuevas; validación final: 109/109 tests, `assembleDebug` OK, `git diff --check` limpio.
- [x] **8.8 — UI Library completa**: SearchBar M3 conectada a la búsqueda reactiva 8.3 (debounce/cancelación conservados); Tabs Canciones/Artistas/Álbumes sobre rutas existentes y resúmenes SQL reales; FAB reutiliza selector SAF/importación; filas de canción con metadatos disponibles, portada/alternativa accesible, objetivos de 48 dp y estado de archivo 8.7; esqueleto inicial, estados vacía/sin coincidencias/error con reintento, sin confundir carga con vacío. Validación: 113 tests JVM OK, `assembleDebug` OK, `git diff --check` limpio; no hay `androidTest` ni `adb` disponible para validar UI en dispositivo.
- [x] **8.9 — Tests**: conserva cobertura existente de Room/DAO y repositorio (listar, buscar, todos los órdenes, recientes, grupos, duplicados y eliminación lógica), modelos de biblioteca/estado e importación; amplía pruebas de consulta en curso cancelada, consulta de espacios, biblioteca real vacía, errores/cancelación al analizar archivos y retención del registro tras MISSING/INACCESSIBLE/INVALID. Suite JVM: 119 tests OK; `assembleDebug` y `git diff --check` OK. Sin pruebas de ViewModel/UI: `kotlinx-coroutines-test` no está aprobada y no existe infraestructura `androidTest`/Compose UI.
- [ ] **8.10 — Validación final**: assembleDebug + testDebugUnitTest + diff --check + revisión de schema v3 + informe.

Criterios de aceptación de la fase: biblioteca completa funcional sin red; 0 dependencias nuevas (salvo aprobación explícita en 8.9); sin datos inventados; contratos de 9/10 preparados sin implementar; migración v3 probada en JVM; pruebas de dispositivo siguen pendientes sin emulador.

---

# FASE 9 — REPRODUCTOR

Estado: ✅ COMPLETADA (implementación y validación física confirmadas por el usuario).

Media3 fue evaluado y aprobado expresamente para esta fase: `androidx.media3:media3-exoplayer:1.11.1` y `androidx.media3:media3-session:1.11.1`. Se usa la pantalla completa de Navigation Compose elegida por el usuario; la UI no controla el reproductor subyacente.

Arquitectura obligatoria (la UI nunca controla el reproductor subyacente):

```text
UI → PlayerViewModel → PlayerController/PlaybackManager → Media3 → archivo local
```

## 9.1 Reproducción

- [x] Play/pausa · anterior · siguiente · seek · ±10 s · duración · progreso · volumen.
- [x] Artwork real (copia de FASE 5) con placeholder del design system si no hay.

## 9.2 Cola y modos

- [x] Cola de reproducción independiente de la cola de descargas, selección desde biblioteca, navegación secuencial y comportamiento de cola vacía/fin.
- [x] Repetición desactivada (predeterminada), repetición de cola y repetición de canción con ciclo de control explícito.
- [x] Shuffle sin repetir canciones antes de completar el ciclo; activar/desactivar conserva la canción actual y funciona junto con ambos modos de repetición.
- [x] Anterior/siguiente y MiniPlayer consultan la disponibilidad real de Media3, respetando shuffle y repetición.
- [x] Controles accesibles de repetición y shuffle en la pantalla completa; el MiniPlayer conserva el espacio para reproducción, pausa y siguiente.

## 9.3 Fondo, sesión y energía

- [x] MediaSession · servicio de reproducción en segundo plano · notificación multimedia · lock screen · auriculares/Bluetooth · gestión de foco (ARCHITECTURE §3.1); validación física confirmada por el usuario.
- [x] Recuperación serializable de cola, índice, posición, `playWhenReady`, modos y orden shuffle tras recrear el servicio/proceso; snapshots v1 se restauran con modos predeterminados.
- [x] Elección de `MediaSessionService` de Media3; permisos `FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_MEDIA_PLAYBACK` declarados.

Criterios de aceptación: Domain sin conocimiento de Media3 (control abstraído); ViewModel transmite estado; 141 pruebas unitarias aprobadas, `assembleDebug` y `git diff --check` exitosos en la validación previa. El usuario confirma la validación física satisfactoria de reproducción local, cola, modo normal, Repeat One, Repeat All, Shuffle y sus combinaciones, controles de pantalla completa y MiniPlayer, notificación/sesión multimedia y restauración de canción, posición, estado y modos. FASE 9 cerrada con base en esa confirmación; no se repiten ni se atribuyen pruebas nuevas a esta revisión.

---

# FASE 10 — PLAYLISTS

Estado: ✅ Completada.

Modelo (m:n con orden independiente por lista):

```text
Playlist ──< PlaylistSong >── Song
```

- [x] Modelo m:n Room `playlists` y `playlist_songs` con orden propio por lista; migración v3→v4 sin pérdida de canciones.
- [x] Contratos Domain y repositorio de playlists reactivos; creación/renombrado/eliminación, añadir/quitar y reordenar miembros con validación de nombres/orden.
- [x] Navegación y UI Material 3: listado, estados vacío/error, crear/renombrar/eliminar con confirmación, detalle, añadir desde biblioteca, reordenar y quitar canciones.
- [x] Reproducción de toda la lista o desde una canción concreta mediante el Player existente; artwork de la primera canción existente.
- [x] Validación final: suite completa (145 pruebas, 0 fallos), `assembleDebug` exitoso, `git diff --check` limpio y revisión del diff.

Validación final: `LibraryLoadModelTest`, `PlaylistRepositoryTest` y `RoomMigrationTest` pasan; la suite completa registra 145 pruebas, 0 fallos, 0 errores y 0 omitidas. `assembleDebug` termina con `BUILD SUCCESSFUL`; `git diff --check` devuelve 0. El fallo inicial era una sincronización insuficiente del test: con un único elemento, cambiar el orden no cambiaba el resultado y la condición de espera podía aceptar el estado anterior antes de que `flatMapLatest` suscribiera la nueva consulta. El test ahora espera estados emitidos por Flow/StateFlow y una lista de varias canciones que demuestra el cambio de orden. No se modificó código de producción.

Decisiones de persistencia (ARCHITECTURE §8.1): borrar playlist elimina en cascada solo sus relaciones; borrar una canción de la biblioteca elimina en cascada sus membresías; nunca se borra un archivo físico. `(playlistId, songUri)` es clave primaria, `position` es orden por playlist con desempate determinista por URI; el repositorio valida el conjunto completo antes de reordenar transaccionalmente. Nombres se recortan y no pueden quedar vacíos; se permiten nombres repetidos.

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

FASE 10 — PLAYLISTS, completada. FASE 9 completada según confirmación física del usuario; FASE 7 continúa pausada.

Último cambio:

FASE 10 — Playlists: Room m:n, repositorio, navegación, gestión y reproducción desde playlist.

Roadmap replanificado (2026-10-28):

- FASE 7 — DESCARGAS: 🟡 PAUSADA (retomar tras evaluar motor HTTP, segundo plano WorkManager/UIDT, NewPipe Extractor y condiciones legales).
- FASE 8 — BIBLIOTECA COMPLETA: completada previamente.
- FASE 9 — REPRODUCTOR: ✅ completada; validación física confirmada por el usuario.
- FASE 10 — PLAYLISTS: ✅ completada.
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
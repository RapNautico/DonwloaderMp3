# AGENTS.md

# Mi Música — Instrucciones del proyecto

Aplicación Android para gestión, descarga permitida y reproducción local de audio.

El objetivo es construir una aplicación Android moderna, estable, segura y mantenible.

## Objetivo
Aplicación Android para gestionar y reproducir música local.

## Estado
Consultar PLAN.md para la fase actual.

## Documentación obligatoria
Antes de modificar código:
- PLAN.md
- ARCHITECTURE.md
- DESIGN.md
- DESIGN_SYSTEM.md

## Arquitectura
Presentation → Domain → Data

Monomódulo inicialmente.

DI manual.

## Stack
Android
Kotlin
Jetpack Compose
Material 3
Room
Media3 (cuando corresponda)

## Reglas
- No cambiar versiones sin justificarlo.
- No introducir dependencias innecesarias.
- No romper la arquitectura existente.
- No implementar funcionalidades de fases futuras.
- No afirmar que una prueba pasó si no fue ejecutada.
- No hacer commit/push automáticamente.
- No introducir secretos.
- No inventar metadatos de audio.
- No implementar bypass de DRM.
- Respetar las APIs oficiales de Android.

## Flujo de trabajo
1. Leer documentación.
2. Inspeccionar código existente.
3. Analizar.
4. Proponer cambios.
5. Implementar.
6. Ejecutar pruebas.
7. Documentar resultados.
8. Esperar instrucciones para Git.

## Git
main = estable.

Las fases nuevas se desarrollan en ramas feature/fase-X-nombre.

---

# 1. ROL DEL AGENTE

Actúa como un equipo senior compuesto por:

- Android Developer
- Kotlin Developer
- Jetpack Compose Developer
- Software Architect
- UI/UX Designer
- QA Engineer
- Security Engineer
- Build/DevOps Engineer

Debes priorizar calidad, mantenibilidad y estabilidad sobre velocidad.

---

# 2. SKILLS

Las skills disponibles en el proyecto deben utilizarse cuando sean relevantes.

Skills principales:

- UI/UX Pro Max
- Graphity

Antes de realizar una tarea determina si alguna skill es relevante.

## UI/UX Pro Max

Utilízala para:

- diseño de interfaces;
- componentes;
- navegación;
- colores;
- tipografía;
- espaciado;
- responsive design;
- accesibilidad;
- dark mode;
- estados de carga;
- empty states;
- interacción;
- experiencia del reproductor.

## Graphity

Utilízala cuando sea útil para:

- diagramas;
- arquitectura visual;
- flujos;
- planificación visual;
- representación de componentes;
- documentación visual.

No utilices una skill simplemente por utilizarla.

---

# 3. TECNOLOGÍA PRINCIPAL

Preferir:

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX
- Coroutines
- Flow
- Room
- Media3
- WorkManager
- MediaStore

Antes de añadir una dependencia:

1. Determina si realmente es necesaria.
2. Comprueba si AndroidX/Kotlin/Jetpack ya proporciona una solución.
3. Evita dependencias innecesarias.
4. Evita librerías abandonadas.
5. Mantén las versiones compatibles.

No cambies versiones de dependencias sin una razón técnica.

---

# 4. ARQUITECTURA

Seguir la arquitectura definida en:

ARCHITECTURE.md

Preferir separación:

Presentation
↓
Domain
↓
Data

Utilizar cuando corresponda:

- ViewModels
- Use Cases
- Repository Pattern
- Dependency Injection
- Room
- Flow

Evitar:

- God classes.
- ViewModels gigantes.
- Activities gigantes.
- lógica de negocio dentro de Composables.
- acceso directo a Room desde UI.
- duplicación de lógica.
- dependencias circulares.

---

# 5. UI

La interfaz debe utilizar:

- Jetpack Compose
- Material 3
- componentes reutilizables
- estados explícitos
- accesibilidad
- dark mode
- light mode

No crear interfaces genéricas o inconsistentes.

Mantener un sistema visual coherente.

Antes de crear una pantalla nueva:

1. Revisar el sistema visual existente.
2. Revisar componentes existentes.
3. Reutilizar componentes cuando sea posible.
4. Utilizar UI/UX Pro Max si la tarea implica diseño o UX.

---

# 6. ESTADO DE UI

Las pantallas deben manejar claramente estados como:

- Loading
- Success
- Empty
- Error
- Offline
- Processing

No ocultar errores importantes.

Los mensajes para el usuario deben ser comprensibles.

---

# 7. DESCARGAS

Las descargas deben ser asíncronas.

Nunca bloquear el hilo principal.

Utilizar la solución adecuada de Android según el caso:

- Coroutines
- WorkManager
- Foreground Service

La elección debe justificarse según las restricciones de Android.

Mantener estados claros:

- Pending
- Analyzing
- Downloading
- Processing
- Completed
- Failed
- Cancelled

---

# 8. AUDIO

La aplicación debe priorizar la calidad real del audio disponible.

No asumir:

320 kbps = máxima calidad.

Diferenciar:

- calidad de fuente;
- formato fuente;
- bitrate fuente;
- formato final;
- bitrate final.

Evitar conversiones innecesarias.

No realizar upscaling engañoso.

---

# 9. REPRODUCTOR

Utilizar Android Media3.

Debe contemplar:

- reproducción;
- pausa;
- seek;
- cola;
- siguiente;
- anterior;
- MediaSession;
- background playback;
- notificación multimedia;
- controles del sistema;
- Bluetooth/auriculares.

La lógica del reproductor no debe estar acoplada directamente a las pantallas Compose.

---

# 10. ALMACENAMIENTO

Utilizar las APIs modernas de Android.

Preferir:

MediaStore cuando corresponda.

Evitar APIs de almacenamiento obsoletas.

La aplicación debe:

- sanitizar nombres;
- evitar path traversal;
- evitar sobrescritura accidental;
- manejar poco espacio;
- detectar archivos eliminados externamente;
- mantener correctamente las URI.

---

# 11. BASE DE DATOS

Room será utilizada para metadatos.

No almacenar archivos de audio dentro de SQLite.

La base de datos debe almacenar información como:

- título;
- artista;
- álbum;
- duración;
- URI;
- tamaño;
- MIME type;
- bitrate;
- portada;
- URL de origen;
- fecha de descarga;
- última reproducción.

---

# 12. SEGURIDAD

La aplicación debe utilizar únicamente fuentes y contenido cuya descarga esté permitida.

NO implementar:

- bypass de DRM;
- evasión de autenticación;
- bypass de paywalls;
- robo de credenciales;
- evasión de protecciones técnicas.

Validar y sanitizar todas las entradas externas.

Nunca ejecutar comandos construidos directamente a partir de entradas del usuario.

---

# 13. PRIVACIDAD

Minimizar la información que abandona el dispositivo.

No almacenar innecesariamente:

- credenciales;
- tokens;
- cookies;
- información personal.

Preferir procesamiento local siempre que sea posible.

---

# 14. TESTING

Las funcionalidades importantes deben tener pruebas.

Preferir:

- Unit Tests
- Repository Tests
- ViewModel Tests
- Integration Tests
- UI Tests cuando aporten valor

No considerar una funcionalidad terminada solamente porque compila.

---

# 15. COMPILACIÓN

Después de cambios relevantes:

1. Compilar.
2. Ejecutar tests relevantes.
3. Revisar errores.
4. Corregir errores.
5. Volver a compilar.

No ignorar errores de compilación.

No ocultar warnings importantes.

---

# 16. CAMBIOS

Antes de realizar cambios importantes:

Explicar:

- qué se modificará;
- por qué;
- archivos afectados;
- dependencias afectadas;
- posibles riesgos.

No realizar refactorizaciones masivas sin necesidad.

No borrar código funcional sin justificarlo.

---

# 17. NUEVAS FUNCIONALIDADES

Antes de implementar una nueva funcionalidad:

1. Revisar PLAN.md.
2. Revisar ARCHITECTURE.md.
3. Revisar el código existente.
4. Determinar dónde debe vivir la funcionalidad.
5. Implementar.
6. Probar.
7. Documentar.

---

# 18. DOCUMENTACIÓN

Mantener actualizados:

- README.md
- PLAN.md
- ARCHITECTURE.md

Cuando una decisión arquitectónica cambie:

Actualizar ARCHITECTURE.md.

Cuando una fase se complete:

Actualizar PLAN.md.

---

# 19. REGLA DE NO INVENTAR

Nunca inventar:

- APIs;
- métodos;
- clases;
- configuraciones;
- propiedades;
- dependencias.

Si no estás seguro de una API o comportamiento:

Investigar primero.

---

# 20. REGLA DE IMPLEMENTACIÓN

No implementar toda la aplicación de una sola vez.

Trabajar por fases.

Cada fase debe:

1. Analizar.
2. Planificar.
3. Implementar.
4. Compilar.
5. Testear.
6. Corregir.
7. Documentar.

No avanzar automáticamente a la siguiente fase.

---

# 21. ESTADO DEL PROYECTO

Cuando el usuario solicite:

"estado"

mostrar:

- fase actual;
- funcionalidades terminadas;
- funcionalidades pendientes;
- errores;
- warnings importantes;
- próximos pasos.

---

# 22. COMANDOS DEL USUARIO

Si el usuario dice:

## "analiza"

No implementar.

Analizar y explicar.

## "planifica"

Modificar PLAN.md si corresponde.

## "implementa"

Implementar la funcionalidad solicitada.

## "compila"

Compilar y analizar errores.

## "tests"

Ejecutar tests.

## "corrige"

Analizar primero el problema y después modificar lo necesario.

## "revisa"

Realizar una revisión del código sin agregar funcionalidades nuevas.

## "estado"

Mostrar estado actual del proyecto.

---

# 23. PRIORIDADES

Orden de prioridad:

1. Seguridad.
2. Funcionamiento correcto.
3. Arquitectura.
4. Mantenibilidad.
5. UX/UI.
6. Rendimiento.
7. Testing.
8. Optimización.

No sacrificar arquitectura por velocidad.

---

# 24. REGLA FINAL

Antes de crear código importante:

Lee:

- AGENTS.md
- PLAN.md
- ARCHITECTURE.md

Y revisa el código existente.

El proyecto debe evolucionar de forma incremental y controlada.
# Bitácora de Desarrollo: GymTrackerViews

*(Fecha de Inicio del Proyecto: 24 de abril de 2025)*

## Introducción y Objetivos del Proyecto
*(Semana del 24 al 26 de abril de 2025)*

El presente Trabajo de Fin de Grado (TFG) aborda el desarrollo de una aplicación móvil nativa para la plataforma Android, denominada provisionalmente 'GymTrackerViews'. El objetivo principal es ofrecer a los usuarios una herramienta sencilla e intuitiva para registrar y monitorizar sus entrenamientos en el gimnasio.
Actualmente, muchos usuarios encuentran dificultades para llevar un seguimiento efectivo de su progreso, ya sea por la incomodidad del registro manual en papel o por la complejidad de algunas aplicaciones existentes en el mercado. Esta aplicación busca solventar este problema permitiendo al usuario registrar fácilmente los ejercicios realizados en cada sesión, junto con detalles clave como el número de series, repeticiones y el peso utilizado. Se busca primar la usabilidad y una experiencia de usuario directa. A futuro, se contempla la posibilidad de añadir funcionalidades para visualizar la progresión a lo largo del tiempo.
El desarrollo se está realizando utilizando tecnologías actuales del ecosistema Android, con Kotlin como lenguaje de programación principal, el sistema de Vistas (XML) para la interfaz de usuario, y la librería Room para la gestión eficiente de la base de datos local en el dispositivo.

## Estado Actual del Desarrollo (Configuración Inicial)
*(24 - 27 de abril de 2025)*

A continuación, se resume brevemente el estado actual del desarrollo y los avances realizados hasta la fecha:
* **Proyecto Base:** Creado proyecto Android (GymTrackerViews, Kotlin, .kts) con plantilla "Empty Views Activity". Configurado build.gradle.kts con ViewBinding, Material Design, Room, Kapt, Coroutines y Lifecycle.
* **Interfaz Principal:** Diseñado activity_main.xml con RecyclerView para lista y FloatingActionButton para añadir.
* **Base de Datos (Room):** Implementada persistencia local:
    * Entidad Workout (id, startTime con DateConverter).
    * DAO WorkoutDao (insert suspend, getAll Flow).
    * Clase AppDatabase (Singleton).
* **Funcionalidad Básica:**
    * MainActivity obtiene instancia de AppDatabase.
    * Click en FAB (+) crea Workout con fecha actual y lo inserta en BD usando lifecycleScope y el DAO.
* **Visualización de Datos:**
    * Creado layout item_workout.xml para filas de la lista.
    * Implementado WorkoutAdapter (con ListAdapter, ViewHolder, DiffUtil) para mostrar ID y fecha formateada.
    * MainActivity observa el Flow de workouts desde el DAO y actualiza el RecyclerView mediante el adapter.
* **Control de Versiones:** Inicializado Git, realizado primer commit del estado funcional, conectado y subido (push) a repositorio GitHub (manuelduenascortes/TFG) usando SSH.
* **Estado General:** La aplicación cuenta con una estructura funcional básica que permite registrar sesiones de entrenamiento (identificadas por su fecha de inicio) en una base de datos local y visualizarlas en una lista en la pantalla principal, la cual se actualiza dinámicamente. El código está versionado y respaldado.

## Avances Recientes: Navegación a Detalles del Entrenamiento
*(28 - 29 de abril de 2025)*

Continuando con el desarrollo, se ha implementado la funcionalidad de navegación desde la lista principal de entrenamientos a una pantalla dedicada para visualizar los detalles de una sesión específica. Los pasos clave realizados en esta fase fueron:
* **Configuración de Navegación Segura:** Se integró el plugin Navigation Safe Args de Android Jetpack para permitir el paso de argumentos entre destinos de navegación de forma segura y tipada. Esto requirió la modificación de los archivos build.gradle.kts a nivel de proyecto y de módulo (app).
* **Ampliación del Gráfico de Navegación (nav\_graph.xml):** Se añadió un nuevo destino (fragment) correspondiente a WorkoutDetailFragment. Se definió un argumento (workoutId de tipo Long) para este destino, necesario para identificar qué entrenamiento mostrar. Se creó una nueva acción (action) para navegar desde WorkoutListFragment hacia WorkoutDetailFragment.
* **Creación de Pantalla de Detalle:** Se generó el archivo de layout fragment\_workout\_detail.xml con una estructura básica (TextViews para título/fecha, un RecyclerView para futuras series, un botón "Añadir Serie"). Se creó la clase WorkoutDetailFragment.kt, implementando la recepción del argumento workoutId mediante navArgs() y mostrando dicho ID en la interfaz.
* **Manejo de Clicks en la Lista:** Se modificó WorkoutAdapter para aceptar una función lambda (onClick) en su constructor. Dentro del ViewHolder, se añadió un OnClickListener a la vista raíz de cada item, que invoca la lambda onClick pasándole el objeto Workout correspondiente a la fila pulsada.
* **Implementación de la Navegación:** En WorkoutListFragment, al instanciar WorkoutAdapter, se le proporcionó la implementación de la lambda onClick. Esta implementación utiliza findNavController() junto con la clase Directions generada por Safe Args para navegar a WorkoutDetailFragment, pasando el workout.id del item seleccionado como argumento.
* **Refactorización:** Se completó el traslado de la lógica relacionada con la obtención de datos de la BD (AppDatabase, WorkoutDao), la configuración del RecyclerView, la observación de datos (getAllWorkouts) y la gestión del FloatingActionButton (insertNewWorkout) desde MainActivity hacia WorkoutListFragment. Como resultado, MainActivity se simplificó considerablemente, actuando únicamente como contenedor del NavHostFragment.
* **Estado Tras esta Fase:** La aplicación ahora permite al usuario seleccionar un entrenamiento de la lista principal y navegar a una pantalla de detalle específica para ese entrenamiento, pasando correctamente el identificador único (workoutId) del mismo. La pantalla de detalle muestra este ID, sentando las bases para cargar y mostrar información más detallada (fecha, lista de series) en los próximos pasos. El código sigue versionado localmente con Git y respaldado en GitHub.

## Avances Recientes: Pantalla de Detalle y Registro de Series
*(30 de abril - 2 de mayo de 2025)*

* **Carga Datos Workout:** WorkoutDetailFragment ahora usa el workoutId recibido para consultar la BD (vía WorkoutDao) y mostrar la fecha/hora de inicio real del entrenamiento seleccionado, observando los datos mediante Flow y lifecycleScope.
* **Preparación Lista Series:** Se creó el layout item\_workout\_set.xml y el adaptador WorkoutSetAdapter (con ListAdapter/DiffUtil) para mostrar los detalles de cada serie (ejercicio, reps, peso).
* **Configuración RecyclerView Series:** En WorkoutDetailFragment, se configuró el RecyclerView (recyclerViewSets) con LinearLayoutManager y el WorkoutSetAdapter. Se inició la observación del Flow de WorkoutSets desde WorkoutSetDao para el workout actual (mostrando una lista inicialmente vacía).
* **Funcionalidad "Añadir Serie":** Se implementó el botón "Añadir Serie". Al pulsarlo, muestra un AlertDialog con un layout personalizado (dialog\_add\_set.xml) para introducir nombre de ejercicio, repeticiones y peso. Tras validar y pulsar "Guardar", crea un WorkoutSet, lo inserta en la BD usando WorkoutSetDao en una corutina, y cierra el diálogo.
* **Actualización Automática:** Las nuevas series añadidas a través del diálogo aparecen inmediatamente en la lista de la pantalla de detalle gracias a la observación del Flow de datos.

## Avances Recientes: Borrado de Series
*(3 - 4 de mayo de 2025)*

* **Añadida Función de Borrado al DAO:** Se añadió la función `deleteSet(set: WorkoutSet)` al `WorkoutSetDao` para permitir la eliminación de series de la base de datos (Room).
* **Añadido Botón de Borrar al Layout:** Se modificó `item_workout_set.xml` (cambiando el layout principal a ConstraintLayout) para incluir un ImageButton con un icono de papelera al final de cada fila de serie.
* **Adaptador Maneja Click de Borrar:** `WorkoutSetAdapter` ahora recibe una lambda `onDeleteClick: (WorkoutSet) -> Unit` en su constructor. Al pulsar el botón de borrado de una fila, llama a esta lambda, pasando la serie correspondiente.
* **Lógica de Borrado en Fragment:** `WorkoutDetailFragment` recibe la señal de borrado del adaptador, muestra un diálogo de confirmación y, si el usuario confirma, llama a `database.workoutSetDao().deleteSet(setToDelete)` en una corutina. La lista de series se actualiza automáticamente gracias al Flow observado.

## Avances Recientes: Borrado de Entrenamientos Completos
*(5 de mayo de 2025)*

Se implementó la funcionalidad para eliminar sesiones de entrenamiento completas desde la lista principal (WorkoutListFragment).
* Se añadieron los métodos necesarios para el borrado en `WorkoutDao` y `WorkoutListViewModel`.
* Se incorporó un botón de borrado (icono de papelera) en el diseño de cada fila de la lista de entrenamientos (`item_workout.xml`).
* Se actualizó `WorkoutAdapter` para manejar el clic en el nuevo botón de borrado y comunicarlo al Fragment mediante una función lambda específica.
* `WorkoutListFragment` ahora recibe la acción de borrado, muestra un diálogo de confirmación al usuario, y si se confirma, ordena al `WorkoutListViewModel` que ejecute la eliminación.
* La interfaz de usuario refleja el borrado automáticamente (el elemento desaparece de la lista) gracias a la observación del `StateFlow` y a la configuración `ON DELETE CASCADE` de la base de datos que elimina también las series asociadas.

## Avances Recientes: Edición de Series y Mensajes de Estado Vacío
*(6 - 7 de mayo de 2025)*

* **Edición de Series:** Se ha implementado la capacidad de modificar series ya existentes. Ahora, al pulsar sobre una fila de la lista de series en la pantalla de detalle, se abre el diálogo de edición (reutilizando el de añadir) con los datos de esa serie precargados. Al guardar los cambios, se actualiza la información en la base de datos (mediante `@update` en `WorkoutSetDao` y una función en `WorkoutDetailViewModel`) y la lista se refresca automáticamente en la pantalla.
* **Mensajes de Lista Vacía:** Para mejorar la experiencia de usuario, se han añadido textos informativos en la pantalla principal (WorkoutListFragment) y en la de detalle (WorkoutDetailFragment). Estos textos (TextViews) aparecen automáticamente cuando la lista correspondiente (de workouts o de series) está vacía, indicando al usuario la ausencia de datos y/o la siguiente acción a realizar (ej. "Pulsa + para añadir"). La visibilidad de estos mensajes y de las listas se gestiona observando el tamaño de las listas de datos desde los ViewModels.
* **Estado Tras esta Fase:** La aplicación ahora permite el ciclo CRUD completo (Crear, Leer, Editar, Borrar) para las series de ejercicios y proporciona una respuesta visual más clara al usuario cuando no hay datos para mostrar en las listas.

## Avances Recientes: Mejora en Entrada de Ejercicios (Autocompletado)
*(8 de mayo de 2025)*

Se ha mejorado la usabilidad del diálogo de añadir/editar series para facilitar la introducción del nombre del ejercicio.
* Se definió una lista predefinida de ejercicios comunes como un recurso `string-array` (`default_exercise_list`) en el archivo `strings.xml`.
* En el layout del diálogo (`dialog_add_set.xml`), el campo `EditText` para el nombre del ejercicio fue reemplazado por un `AutoCompleteTextView`, manteniendo el mismo ID (`editTextExerciseName`).
* Se modificó la función `showAddOrEditSetDialog` en `WorkoutDetailFragment` para:
    * Cargar la lista de ejercicios desde el recurso `string-array`.
    * Crear un `ArrayAdapter` simple con esa lista.
    * Asignar dicho `ArrayAdapter` al `AutoCompleteTextView` mediante `setAdapter()`.
* **Resultado:** Ahora, al escribir en el campo de nombre de ejercicio dentro del diálogo, se muestran sugerencias desplegables basadas en la lista predefinida, permitiendo al usuario seleccionar una rápidamente o continuar escribiendo un nombre personalizado si lo desea. Esto agiliza la entrada de datos y ayuda a mantener la consistencia en los nombres de ejercicios comunes.

## Avances Recientes: Finalización de Entrenamientos
*(9 - 10 de mayo de 2025)*

* **Objetivo:** Permitir al usuario marcar una sesión de entrenamiento como completada, registrando la hora de finalización.
* **Modificación de Datos:** Se añadió un campo `endTime` (de tipo `Date?`, nulable) a la entidad `Workout` para almacenar la hora de fin.
* **Migración de Base de Datos:** Se incrementó la versión de `AppDatabase` a 3. Se implementó una `Migration` (MIGRATION\_2\_3) usando SQL (`ALTER TABLE`) para añadir la nueva columna `end_time` a la tabla `workouts` existente sin perder datos. Se eliminó `fallbackToDestructiveMigration`.
* **Actualización en DAO y ViewModel:** Se añadieron las funciones necesarias (`updateWorkout` en `WorkoutDao`, `finishWorkout` en `WorkoutDetailViewModel`) para actualizar el `Workout` con la `endTime`. Se creó un `StateFlow` (`isWorkoutFinished`) en el ViewModel para saber si el workout actual ya tiene una hora de fin.
* **Interfaz de Usuario:** Se añadió un botón "Finalizar Workout" al layout `fragment_workout_detail.xml`.
* **Lógica en Fragment:** `WorkoutDetailFragment` ahora:
    * Llama a `viewModel.finishWorkout()` al pulsar el nuevo botón.
    * Observa el `StateFlow isWorkoutFinished` del ViewModel.
    * Oculta el botón "Finalizar Workout" y deshabilita el botón "Añadir Serie" si el entrenamiento ya está finalizado.
* **Estado Tras esta Fase:** La aplicación permite marcar un entrenamiento como finalizado desde su pantalla de detalle. El estado finalizado se persiste en la base de datos y la interfaz se adapta para reflejarlo, ocultando el botón de finalizar y deshabilitando la adición de nuevas series a un workout ya completado.

## Avances Recientes: Mostrar Estado y Duración en Lista Principal
*(11 de mayo de 2025)*

* **Objetivo:** Mejorar la información presentada en la lista principal de entrenamientos (`WorkoutListFragment`) para que el usuario pueda ver rápidamente si un entrenamiento está en curso o, si ha finalizado, cuánto duró.
* **Modificación de Layout (`item_workout.xml`):** Se añadió un `TextView` adicional (`textViewWorkoutStatusDuration`) al diseño de cada fila de la lista de workouts.
* **Lógica en Adaptador (`WorkoutAdapter.kt`):** Se actualizó la función `bind` del `WorkoutViewHolder`:
    * Comprueba si el campo `endTime` del objeto `Workout` es nulo.
    * Si no es nulo, calcula la duración del entrenamiento restando `startTime` de `endTime`. La duración se formatea en horas y minutos (o segundos si es muy corta) y se muestra en `textViewWorkoutStatusDuration`.
    * Si `endTime` es nulo, se muestra el texto "En curso" en `textViewWorkoutStatusDuration`.
    * Se asegura que el `textViewWorkoutStatusDuration` sea visible.
* **Estado Tras esta Fase:** La lista principal de entrenamientos ahora muestra, para cada sesión, no solo su ID y hora de inicio, sino también si está "En curso" o su duración total si ya ha sido finalizada, proporcionando una visión más completa del historial de entrenamientos.

## Avances Recientes: Añadir Notas al Entrenamiento
*(12 de mayo de 2025)*

* **Objetivo:** Permitir al usuario añadir notas o comentarios descriptivos a cada sesión de entrenamiento.
* **Modificación de Datos:** Se añadió un campo `notes` (de tipo `String?`, nulable) a la entidad `Workout`.
* **Migración de Base de Datos:** Se incrementó la versión de `AppDatabase` a 4 y se implementó MIGRATION\_3\_4 para añadir la nueva columna `notes` (TEXT) a la tabla `workouts` sin pérdida de datos.
* **Interfaz de Usuario:** Se añadió un `EditText` multilínea (`editTextWorkoutNotes`) al layout `fragment_workout_detail.xml`, ubicado debajo de la lista de series.
* **ViewModel:** Se añadió la función `saveNotes(notes: String?)` a `WorkoutDetailViewModel` para actualizar el campo `notes` del workout actual en la base de datos.
* **Lógica en Fragment:** `WorkoutDetailFragment` ahora:
    * Observa las notas del `Workout` actual desde el ViewModel y las muestra en el `editTextWorkoutNotes`. Se usa `distinctUntilChanged` y una comprobación para evitar refrescar el `EditText` innecesariamente mientras el usuario escribe.
    * Guarda automáticamente el contenido del `editTextWorkoutNotes` llamando a `viewModel.saveNotes()` cuando el fragment se pausa (`onPause`).
    * Deshabilita el `editTextWorkoutNotes` si el workout está marcado como finalizado.
* **Estado Tras esta Fase:** La aplicación permite ahora visualizar, añadir y editar notas de texto libre para cada sesión de entrenamiento en la pantalla de detalle. Las notas se guardan automáticamente al salir de la pantalla y el campo se bloquea si el entrenamiento ha finalizado.

## Avances Recientes: Mostrar Preview de Notas en Lista Principal
*(13 de mayo de 2025)*

* **Objetivo:** Mejorar la vista de la lista principal (`WorkoutListFragment`) mostrando un extracto de las notas asociadas a cada entrenamiento, si existen.
* **Modificación de Layout (`item_workout.xml`):** Se añadió un `TextView` adicional (`textViewWorkoutNotesPreview`) al diseño de cada fila de la lista de workouts, configurado para mostrar un máximo de dos líneas y usar puntos suspensivos (`ellipsize`) si el texto es más largo. Se ajustó el orden de los elementos para dar más visibilidad a la fecha y las notas.
* **Lógica en Adaptador (`WorkoutAdapter.kt`):** Se actualizó la función `bind` del `WorkoutViewHolder`:
    * Comprueba si el campo `notes` del objeto `Workout` no es nulo ni está vacío/blanco.
    * Si hay notas, se asignan al `textViewWorkoutNotesPreview` y este se hace visible.
    * Si no hay notas, el `textViewWorkoutNotesPreview` se oculta (`View.GONE`).
* **Estado Tras esta Fase:** La lista principal de entrenamientos ahora muestra un preview de las notas (hasta dos líneas) para cada sesión que las tenga registradas, ofreciendo más contexto al usuario sin necesidad de entrar en la pantalla de detalle.

## Avances Recientes: Lista de Series Agrupada por Ejercicio y Correcciones
*(14 - 15 de mayo de 2025)*

* **Objetivo:** Mejorar la visualización en la pantalla de detalle (`WorkoutDetailFragment`) agrupando las series por ejercicio para mayor claridad y solucionar errores persistentes.
* **Agrupación de Datos:** Se modificó `WorkoutDetailViewModel` para procesar la lista de `WorkoutSets` y exponerla como un `Map<String, List<WorkoutSet>>` (`groupedWorkoutSets`), donde la clave es el nombre del ejercicio.
* **Nuevo Modelo y Layouts:** Se creó una `sealed class WorkoutDetailListItem` para representar los dos tipos de elementos de la lista (cabecera de ejercicio y detalle de serie). Se crearon layouts XML específicos: `item_exercise_header.xml` para las cabeceras y `item_workout_set_detail.xml` para las filas de series individuales.
* **Adaptador Multitipo:** Se implementó un nuevo `WorkoutDetailAdapter` que hereda de `ListAdapter<WorkoutDetailListItem, RecyclerView.ViewHolder>`. Este adaptador es capaz de:
    * Identificar el tipo de item (cabecera o serie) usando `getItemViewType`.
    * Crear el `ViewHolder` apropiado (`HeaderViewHolder` o `SetViewHolder`) en `onCreateViewHolder`.
    * Vincular los datos correctos al `ViewHolder` correspondiente en `onBindViewHolder`.
    * Manejar los clics para editar y borrar series dentro del `SetViewHolder`.
* **Actualización del Fragment:** `WorkoutDetailFragment` fue modificado para:
    * Utilizar el nuevo `WorkoutDetailAdapter`.
    * Observar el `Map` agrupado (`groupedWorkoutSets`) desde el ViewModel.
    * Transformar el `Map` agrupado en una lista plana (`List<WorkoutDetailListItem>`) que intercala cabeceras y series, y enviarla al adaptador.
* **Corrección de Errores:** Se solucionaron errores recurrentes de compilación y ejecución.
* **Estado Tras esta Fase:** La aplicación compila correctamente. La pantalla de detalle ahora muestra las series agrupadas por nombre de ejercicio, mejorando la legibilidad. Se han solucionado crashes y problemas que impedían añadir series. La funcionalidad de añadir, editar, borrar series y finalizar el workout, junto con las notas y el autocompletado, está operativa.

## Resumen Breve de Avances (Hasta el 16 de mayo de 2025)
*(16 de mayo de 2025)*

* **Listas Mejoradas:** La lista principal ahora muestra estado/duración, conteo de series y preview de notas. La lista de detalle agrupa series por ejercicio. Se añadieron mensajes para listas vacías.
* **Notas del Workout:** Implementada funcionalidad para añadir, ver, editar y guardar notas (con migración de BD v4).
* **Entrada de Series Optimizada:** Se añadió validación de datos (reps > 0, peso >= 0) y autocompletado para nombres de ejercicio en el diálogo de añadir/editar serie.
* **CRUD Completo:** Funcionalidad completa para crear, ver, editar y borrar tanto workouts como series. Implementada opción para finalizar workouts.
* **Arquitectura y Estabilidad:** Refactorizado a MVVM (ViewModels para lista y detalle). Solucionados crashes y errores relacionados con el ciclo de vida de fragments y la navegación.
* **Estado Actual:** App funcionalmente robusta con las características principales implementadas y una estructura de código mejorada.

## Resumen de Avances Recientes (Integración Firebase y Correcciones)
*(17 - 19 de mayo de 2025)*

* **Temporizador de Descanso Mejorado:** Ajustes en la UI y lógica.
* **Diálogo de Series con Material Design:** Actualización visual.
* **Mejoras Visuales y Correcciones:** Tema oscuro, diseño de tarjetas.
* **Resolución de Problemas con Kapt y Firebase:**
    * Diagnóstico de incompatibilidad de metadatos de Kotlin.
    * Reversión a commit estable (AGP 8.2.2, Kotlin 1.9.22, Kapt 1.9.22, Room 2.6.1).
    * Solución de errores de configuración de Firebase y ajuste de Firebase BOM a `32.8.1`.
* **Sistema de Cuentas de Usuario (Login, Logout y Registro):** Implementación completa con Firebase Authentication.
* **Edición del Nombre del Entrenamiento:** Funcionalidad añadida con diálogo y migración de BD.
* **Pantalla para Iniciar un Nuevo Entrenamiento con Nombre:** Implementación de `NewWorkoutFragment`.
* **Estado Actual:** El proyecto compila, la autenticación funciona, y las características previas están integradas. Se resolvieron problemas críticos de compilación.

## Avances de la Sesión Actual - Implementación de Estadísticas
*(20 - 21 de mayo de 2025)*

* **Decisión de la Funcionalidad:** Desarrollo de sección de "Estadísticas y Seguimiento del Progreso" con gráficos.
* **Diseño Inicial de la Pantalla de Estadísticas:** Creación de `fragment_statistics.xml` con `LineChart` y `TextView` para "no hay datos".
* **Creación del `StatisticsFragment` y `ViewModel` (inicial):** Reutilización inicial de `WorkoutListViewModel`. Lógica para configurar `LineChart` y transformar `WorkoutSummary` en `Entry` para el gráfico. Implementación de `DateAxisValueFormatter`.
* **Navegación a Estadísticas:** Añadido destino y acción en `nav_graph.xml`, y opción de menú en `workout_list_menu.xml`.
* **Mejora de la Pantalla de Estadísticas (Dos Gráficos y CardViews):**
    * Añadido segundo gráfico para "Progreso de Volumen Total (kg) por Workout".
    * Modificación de DAOs (`WorkoutDao`, `WorkoutSetDao`) para nuevas consultas de estadísticas.
    * Creación de `StatisticsViewModelFactory` y `StatisticsViewModel` dedicado para calcular series y volumen por workout.
    * Actualización de `fragment_statistics.xml` con dos `MaterialCardView` y `NestedScrollView`.
    * Modificación de `StatisticsFragment` para usar el nuevo ViewModel y poblar ambos gráficos.
* **Solución de Errores de Compilación y Advertencias:** Correcciones de referencias a recursos, nombres de ViewModel, y parámetros no usados.
* **Estado al Finalizar la Sesión:** Pantalla de estadísticas funcional con dos gráficos ("Series por Workout" y "Volumen Total por Workout"), mensajes de "no hay datos", y navegación correcta.

## Nuevas Funcionalidades Implementadas (Organización y Visualización)
*(22 de mayo de 2025)*

* **Asignación de Grupos Musculares a los Entrenamientos:**
    * Modificación de entidad `Workout` (campo `mainMuscleGroup`).
    * Migración de BD (v7, MIGRATION\_6\_7).
    * Rediseño de `NewWorkoutFragment` con `CheckBoxes` para selección múltiple de grupos musculares.
    * Actualización de `NewWorkoutFragment.kt` y `WorkoutListViewModel` para manejar y guardar los grupos seleccionados.
* **Filtrado de la Lista de Entrenamientos por Grupo Muscular Principal:**
    * Nuevo método en `WorkoutDao` (`getWorkoutSummariesByMuscleGroup`) con consulta `LIKE`.
    * Actualización de `WorkoutListViewModel` con `StateFlow` para filtro y `flatMapLatest`.
    * Modificación de `WorkoutListFragment` con `AutoCompleteTextView` para selección de filtro y botón para limpiar.
* **Problemas y Soluciones:** Corrección de errores de compilación relacionados con la nueva funcionalidad y referencias.

## Avance en la Gestión de Ejercicios y Sugerencias Dinámicas al Registrar Series
*(23 de mayo de 2025)*

* **Biblioteca de Ejercicios Personalizada:**
    * Consolidación de funcionalidad CRUD para ejercicios (`Exercise`, `ExerciseDao`, `ExerciseLibraryFragment`, `ExerciseLibraryViewModel`).
* **Integración de Ejercicios al Añadir Series (`WorkoutDetailFragment`):**
    * **Sugerencias Dinámicas:** `AutoCompleteTextView` en `dialog_add_set.xml` ahora ofrece sugerencias combinando ejercicios de la BD (via `WorkoutDetailViewModel` y `ExerciseDao`) y la lista predefinida de `arrays.xml`.
    * La lista combinada se maneja en `WorkoutDetailFragment` para poblar el `ArrayAdapter`.
* **Impacto:** Mayor flexibilidad y personalización, mejorando la experiencia de registro de series.

## Mejoras Generales de Interfaz de Usuario y Tema
*(24 de mayo de 2025)*

* **Paleta de Colores:** Definida nueva paleta para modo oscuro (primario azul, secundario naranja, fondos grises).
* **Tipografía:** Adoptada familia "Inter" (Regular y Medium).
* **Estilos de Componentes:** Definidos estilos base para `Toolbar`, `MaterialCardView`, `Button`, `TextInputLayout` en `themes.xml`.
* **Icono de la Aplicación:** Recomendación de uso de Image Asset Studio.
* **Pantallas de Autenticación:** Eliminación de Toolbar, adición de icono de app.
* **Pantalla de Lista de Entrenamientos:** Actualización de iconos de menú, añadido logo a Toolbar (en `MainActivity`), fondo sutil con icono, elementos translúcidos, actualización icono de borrar workout.
* **Pantalla de Detalle del Entrenamiento:** Título dinámico en Toolbar, ajuste tamaño nombre entrenamiento, actualización icono editar nombre, fondo sutil con icono, elementos translúcidos, reorganización botones, funcionalidad duplicar series, actualización iconos temporizador, corrección botón reiniciar, ajuste tamaño campo de notas.
* **Gestión de Ejercicios y Sugerencias:** Conexión de `AutoCompleteTextView` a múltiples fuentes (BD y `arrays.xml`).
* **Navegación:** Implementación de transiciones de deslizamiento entre pantallas.
* **Corrección de Errores:** Diversos errores de compilación y runtime solucionados.

## Finalización y Entrega
*(25 de mayo de 2025)*
* Revisión final de todas las funcionalidades.
* Pruebas exhaustivas de la aplicación.
* Preparación de la memoria del TFG.
* Generación de los entregables finales.
* Subida final del proyecto al repositorio de GitHub.

# GymTrackerViews: Aplicación Android para Seguimiento de Entrenamientos

**Autor:** Manuel Dueñas Cortés

## Descripción del Proyecto

GymTrackerViews es una aplicación Android nativa desarrollada en Kotlin como Proyecto Integrado del Ciclo Formativo de Grado Superior en DAM. Su objetivo principal es ofrecer a los usuarios una herramienta intuitiva y completa para registrar, visualizar y analizar sus entrenamientos de gimnasio, fomentando la constancia y el seguimiento del progreso.

La aplicación permite a los usuarios gestionar sus sesiones de entrenamiento, registrar series detalladas (ejercicio, repeticiones, peso), mantener una biblioteca de ejercicios personalizable, y visualizar estadísticas de su progreso, incluyendo récords personales.

## Características Principales

* **Autenticación de Usuarios:** Sistema de registro e inicio de sesión seguro mediante Firebase Authentication.
* **Registro Detallado de Entrenamientos:**
    * Creación de nuevas sesiones de entrenamiento con nombre y grupos musculares.
    * Registro de series con nombre de ejercicio, repeticiones y peso.
    * Edición, duplicado y eliminación de series.
    * Temporizador de descanso configurable.
    * Notas personalizadas por entrenamiento.
* **Biblioteca de Ejercicios:**
    * Gestión de un catálogo personal de ejercicios.
    * Sugerencias de ejercicios al registrar series.
* **Estadísticas y Progreso:**
    * Gráficos de frecuencia de entrenamientos semanales.
    * Gráficos de progresión de peso por ejercicio.
    * Listado de récords personales.
* **Interfaz de Usuario:** Diseño intuitivo basado en Material 3, con tema oscuro.
* **Persistencia de Datos:** Uso de Room para el almacenamiento local de toda la información.

## Tecnologías Utilizadas

* **Lenguaje:** Kotlin (v1.9.22)
* **Arquitectura:** MVVM (Model-View-ViewModel)
* **Componentes Jetpack:**
    * Room Persistence Library (v2.6.1)
    * ViewModel & Lifecycle (v2.7.0)
    * Navigation Component (v2.7.7) (con Safe Args)
    * ViewBinding
* **Programación Asíncrona:** Kotlin Coroutines (v1.7.3) y Flow
* **Interfaz de Usuario:** Material Components 3 (v1.11.0), RecyclerView, ConstraintLayout
* **Backend (BaaS):**
    * Firebase Authentication (BOM v32.8.1)
    * Firebase Analytics (BOM v32.8.1)
* **Librerías Externas:** MPAndroidChart (v3.1.0)
* **Build:** Gradle (v8.11.1) con Android Gradle Plugin (v8.2.2) y scripts en Kotlin (kts)

## Estructura del Proyecto

El proyecto está organizado principalmente en los siguientes paquetes dentro del módulo `:app` (la ruta completa es `app/src/main/java/com/example/gymtrackerviews/` y luego los subpaquetes):

* `data` (o el paquete raíz para Entidades Room, DAOs, AppDatabase como `Workout.kt`, `WorkoutDao.kt`, `AppDatabase.kt`)
* `ui/auth` (Fragments para Login, Register, Splash como `LoginFragment.kt`)
* `ui/library` (Fragment y ViewModel para la Biblioteca de Ejercicios como `ExerciseLibraryFragment.kt`)
* `ui/statistics` (Fragments y ViewModel para Estadísticas y Récords como `StatisticsFragment.kt`)
* *Paquete raíz* (`com.example.gymtrackerviews`) para Fragments principales como `WorkoutListFragment.kt`, `NewWorkoutFragment.kt`, `WorkoutDetailFragment.kt`, y sus ViewModels.
* `adapter` (Adapters para los RecyclerViews como `WorkoutAdapter.kt`)

## Documentación Adicional

* **Memoria Completa del TFG:** Para una descripción exhaustiva del proyecto, objetivos, diseño, desarrollo, resultados y conclusiones, consulta la [Memoria Completa del TFG (Memoria_TFG_Manuel_Duenas_GymTrackerViews.pdf)](Memoria_TFG_Manuel_Duenas_GymTrackerViews.pdf) (Este archivo PDF deberá ser subido al repositorio).
* **Bitácora de Desarrollo:** El seguimiento detallado del proceso de desarrollo, incluyendo decisiones técnicas, problemas encontrados y soluciones aplicadas, se encuentra documentado en la [Bitácora de Desarrollo (BITACORA.md)](BITACORA.md).

## Cómo Empezar (Configuración del Entorno)

1.  Clonar el repositorio: `git clone https://github.com/manuelduenascortes/TFG.git`
2.  Abrir el proyecto en Android Studio (compatible con Android Gradle Plugin 8.2.2 y Gradle 8.11.1).
3.  El archivo `google-services.json` necesario para la conexión con Firebase se encuentra en el directorio `app/`.
4.  Construir (`Build > Make Project`) y ejecutar la aplicación en un emulador o dispositivo Android (API 24+).

## Autor

* **Manuel Dueñas Cortés**
    * GitHub: [manuelduenascortes](https://github.com/manuelduenascortes)

---

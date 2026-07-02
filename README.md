# GeoAvisos

GeoAvisos es una aplicación Android desarrollada en Kotlin que permite crear recordatorios asociados a ubicaciones geográficas concretas.

La aplicación utiliza geovallas (Geofencing) para detectar cuándo el usuario entra en una zona determinada y mostrar automáticamente una notificación con la tarea asociada.

Este proyecto ha sido desarrollado como Trabajo Fin de Grado del Curso de Adaptación al Grado en Ingeniería Informática.

## Funcionalidades

- Creación de tareas asociadas a una ubicación.
- Edición y eliminación de tareas.
- Selección de ubicaciones mediante Google Maps.
- Configuración del radio de activación de las geovallas.
- Activación y desactivación de recordatorios.
- Notificaciones automáticas basadas en ubicación.
- Almacenamiento local mediante Room Database.

## Tecnologías utilizadas

- Kotlin
- Android SDK
- Google Maps SDK
- Google Play Services Location
- Geofencing API
- Room Database
- SQLite
- Arquitectura MVVM

## Requisitos

- Android 8.0 (API 26) o superior.
- Permisos de localización.
- Conexión a Internet para la carga de mapas.

## Instalación

1. Clonar el repositorio.
2. Abrir el proyecto en Android Studio.
3. Por motivos de seguridad, el archivo local.properties no se incluye en este repositorio, ya que contiene información específica del entorno local y credenciales que no deben publicarse.
   Se ha añadido un archivo "local.properties.example" como ejemplo para poder configurar este archivo correctamente.
   Para compilar y ejecutar la aplicación es necesario crear dicho archivo siguiendo estos pasos:
   - Copiar "local.properties.example" como "local.properties".
   - Configurar la ruta del SDK de Android (sdk.dir).
   - Crear una clave de API de Google Maps Platform.
   - Sustituir API_KEY_DE_GOOGLE_MAPS por la clave correspondiente.
   - Sustituir la ruta de Android SDK por la que corresponda.
5. Compilar y ejecutar la aplicación.

## APK de prueba

En la carpeta `releases/` se incluye un APK de prueba:

`GeoAvisos-v1-debug.apk`

Esta versión permite instalar la aplicación en un dispositivo Android para comprobar su funcionamiento.

## Autor

Antonio Llorente Gordo

## Licencia

Este proyecto se distribuye bajo licencia MIT.

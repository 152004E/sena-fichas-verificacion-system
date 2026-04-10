# 🚀 SENA Fichas Verificación System

## 🧠 📊 Resumen del Sistema

**Sistema de Validación y Gestión de Fichas SENA**

### 💡 Problema que soluciona

Actualmente:
- ✗ La información está en un Excel en SharePoint
- ✗ Una persona lo actualiza manualmente
- ✗ Se descarga y se envía manualmente
- ✗ No hay validaciones automáticas
- ✗ Es lento y propenso a errores

### 🚀 Solución propuesta

Una aplicación de escritorio (ejecutable) que:
- ✅ Se conecta al Excel en la nube
- ✅ Descarga y procesa la información automáticamente
- ✅ Convierte los datos en un sistema estructurado
- ✅ Permite analizar, validar y visualizar fichas automáticamente
- ✅ Genera reportes inteligentes

### 🔥 En una sola frase:
> Sistema que transforma un Excel en un sistema inteligente de validación automática de fichas

---

## 🏗️ 🧱 Arquitectura del Sistema

### 📥 Fuente de datos
- Excel en SharePoint

### ⚙️ Procesamiento
- **Java 11+** (lógica de negocio)
- **Apache POI** (lectura de Excel)

### 🗄️ Almacenamiento
- **SQLite** (base de datos local)

### 🖥️ Interfaz
- **JavaFX** (aplicación de escritorio)

### 🔄 Flujo general
```
1. Usuario abre la app
2. Clic en "Actualizar información"
3. Se descarga el Excel desde SharePoint
4. Se procesan los datos y se validan
5. Se guardan en SQLite local
6. Se muestran resultados en la interfaz
```

### ⚠️ Nota importante sobre SharePoint
> **SharePoint NO deja descargar archivos fácilmente con URL directa**

En una primera versión:
- ✅ Descargar manualmente el archivo y seleccionarlo en la app
- ✅ O usar una URL pública si el archivo está compartido
- ⚠️ Autenticación con Microsoft Graph API para futuras versiones

---

## 🔥 Funcionalidades Clave

| Funcionalidad | Descripción |
|--------------|------------|
| 🔍 **Búsqueda de fichas** | Buscar por número instantáneamente |
| 📊 **Estado automático** | Completa, Incompleta, Con errores |
| 📚 **Validación de competencias** | Detecta faltantes, calcula estado automáticamente |
| 🚨 **Alertas** | Fichas incompletas, inconsistencias |
| 📄 **Reportes** | Exportar resultados, generar Excel limpio |
| 🔄 **Actualización automática** | Desde SharePoint o archivo local |

### 🚨 Ejemplos de Validaciones

- **Ficha sin programa asignado** → ❌ Error crítico
- **Competencias incompletas** → ⚠️ Advertencia
- **Instructor no asignado** → ⚠️ Alerta
- **Fechas inconsistentes** → ⚠️ Validar
- **Campos obligatorios vacíos** → ❌ Error de validación

---

## 📁 Estructura del Proyecto

```
sena-fichas-verificacion-system/
│
├── .git/                                        # Control de versiones del repositorio
│
├── src/
│   └── main/
│       ├── java/
│       │   ├── app/
│       │   │   └── Main.java                    # Clase principal y arranque de la aplicación
│       │   │
│       │   ├── database/
│       │   │   └── DatabaseManager.java        # Acceso y gestión de la base de datos SQLite
│       │   │
│       │   ├── Model/
│       │   │   ├── Ficha.java                   # Modelo de ficha
│       │   │   ├── Instructor.java              # Modelo de instructor
│       │   │   ├── Programa.java                # Modelo de programa
│       │   │   └── EstadoFicha.java             # Enumeración de estados de ficha
│       │   │
│       │   ├── excel/
│       │   │   └── (ExcelReader.java próximamente) # Lector de archivos Excel
│       │   │
│       │   ├── service/
│       │   │   └── (SyncService.java próximamente) # Lógica de sincronización de datos
│       │   │
│       │   └── ui/
│       │       └── MainWindow.java              # Interfaz gráfica con JavaFX
│       │
│       └── resources/
│           └── config.properties               # Configuración de la aplicación
│
├── data/
│   └── .gitkeep                                # Carpeta de datos persistentes
│
├── target/                                      # Salida de compilación Maven
│   ├── classes/
│   ├── generated-sources/
│   └── maven-status/
│
├── pom.xml                                     # Archivo de configuración y dependencias Maven
└── README.md                                   # Documentación del proyecto
```

---

## 🧰 Tecnologías Utilizadas

| Tecnología | Propósito | Versión |
|-----------|----------|---------|
| **Java** | Lenguaje principal | 11+ |
| **Maven** | Gestor de dependencias | 3.6+ |
| **JavaFX** | Interfaz gráfica | 21.0.2 |
| **Apache POI** | Lectura de Excel | 5.0.0 |
| **SQLite** | Base de datos local | 3.44.0.0 |
| **JDBC** | Conexión a BD | latest |

---

## � Modelo de Datos (Propuesto)

### Entidades principales

```
FICHA
├── idFicha
├── numero
├── estado (Completa, Incompleta, Con errores)
├── programa_id (FK)
└── instructor_id (FK)

PROGRAMA
├── idPrograma
├── nombre
└── descripcion

INSTRUCTOR
├── idInstructor
├── nombre
├── email
└── especialidad

COMPETENCIA
├── idCompetencia
├── nombre
├── descripcion
└── codigo

FICHA_COMPETENCIA (Relación muchos a muchos)
├── idFichaCompetencia
├── ficha_id (FK)
├── competencia_id (FK)
├── estado (Completada, Pendiente, En progreso)
└── observacion
```

### Relaciones clave
- **Ficha → Programa** (1:N) Una ficha pertenece a UN programa
- **Ficha → Instructor** (1:N) Una ficha está a cargo de UN instructor
- **Ficha → Competencia** (N:M) Una ficha tiene VARIAS competencias

---

## �🧠 🎯 Filosofía del Sistema

```
Excel = Fuente de datos (actualización desde SharePoint)
App   = Inteligencia y control (validaciones y análisis)
```

El sistema **NO reemplaza el Excel**, sino que lo **potencia**:
- Los datos vienen del Excel
- La inteligencia está en la app
- Los reportes se generan de forma automática

---

## 🔄 Estrategia de Sincronización

### Proceso de actualización

```
1. Usuario carga el archivo Excel
   ↓
2. App valida estructura del Excel
   ↓
3. Se elimina la BD actual (/data/datos.db)
   ↓
4. Se crea BD nueva y limpia
   ↓
5. Se leen todas las filas del Excel
   ↓
6. Se convierten a objetos Java
   ↓
7. Se valida cada ficha
   ↓
8. Se insertan en SQLite
   ↓
9. Se cargan resultados en la interfaz
```

### Ventajas
- ✅ Datos siempre sincronizados
- ✅ No hay inconsistencias
- ✅ Fácil de auditar controles
- ✅ Reportes siempre actualizados

---

## 🚀 🪜 Fases de Desarrollo

### 🥇 FASE 0 — Configurar el entorno
- [x] Instalar Java 11+
- [x] Instalar Maven 3.6+
- [x] Clonar el repositorio
- [x] Ejecutar `mvn clean compile`

### 🥇 FASE 1 — Entender el Excel
- [ ] Identificar hojas y columnas
- [ ] Mapear estructura de datos
- [ ] Documentar relaciones
- [ ] Crear modelo de datos definitivo

### 🥈 FASE 2 — Diseñar el modelo de datos
- [x] Crear clases Java (Ficha, Instructor, Programa, EstadoFicha)
- [ ] Diseñar tablas SQLite
- [ ] Definir relaciones y crear Competencia.java y FichaCompetencia.java
- [ ] Validaciones en las entidades

### 🥉 FASE 3 — Descargar desde SharePoint
- [ ] Obtener URL directa del Excel
- [ ] Implementar descarga con Java

### 🏅 FASE 4 — Leer el Excel (ExcelReader.java)
- [ ] Usar Apache POI
- [ ] Convertir filas en objetos Java

### 🏅 FASE 5 — Procesar lógica (SyncService.java)
- [ ] Validar competencias
- [ ] Detectar faltantes
- [ ] Calcular estado automático

### 🏅 FASE 6 — Guardar en SQLite (DatabaseManager.java)
- [ ] Crear tablas
- [ ] Persistir datos
- [ ] Optimizar consultas

### 🏅 FASE 7 — Crear interfaz (MainWindow.java - JavaFX)
- [ ] Pantalla principal
- [ ] Vista de fichas (tabla)
- [ ] Detalle de ficha
- [ ] Panel de alertas

### 🏅 FASE 8 — Exportar resultados
- [ ] Generar Excel nuevo
- [ ] Crear reportes en PDF o Excel

---

## � Casos de Uso

### 1. **Usuario actualiza información**
```
1. Usuario abre la aplicación
2. Hace clic en "Cargar Excel"
3. Selecciona el archivo desde su computadora
4. Hace clic en "Actualizar"
5. App procesa los datos
6. Se muestran fichas en la tabla principal
```

### 2. **Usuario busca una ficha**
```
1. Usuario está en la vista principal
2. Escribe número de ficha en buscador
3. App filtra instantáneamente
4. Usuario puede ver resultado
```

### 3. **Usuario revisa estado de una ficha**
```
1. Usuario hace doble clic en una ficha
2. Se abre ventana de detalle
3. Ve:
   - Datos básicos
   - Competencias asignadas
   - Estado de cada competencia
   - Alertas y errores
```

### 4. **Usuario revisa alertas/errores**
```
1. Usuario hace clic en pestaña "Alertas"
2. Ve lista de fichas con problemas
3. Puede filtrar por tipo de error
4. Puede hacer correcciones en el Excel
```

### 5. **Usuario exporta reporte**
```
1. Usuario hace clic en "Exportar"
2. Selecciona formato (Excel, PDF)
3. Elige qué información incluir
4. Define carpeta de destino
5. Se genera el archivo
```

---

## �📚 Ruta de Aprendizaje

### 🥇 1. Java Básico
- [ ] Variables y tipos de datos
- [ ] Clases y objetos
- [ ] Listas y colecciones
- [ ] Métodos y encapsulación

### 🥈 2. SQLite + JDBC
- [ ] Conexión a base de datos
- [ ] Crear tablas (CREATE)
- [ ] Insertar datos (INSERT)
- [ ] Consultar datos (SELECT)
- [ ] Eliminar datos (DELETE)

### 🥉 3. Leer Excel (Apache POI)
- [ ] Leer filas
- [ ] Leer celdas
- [ ] Convertir datos a objetos Java

### 🏆 4. JavaFX
- [ ] Crear ventanas
- [ ] Botones y eventos
- [ ] Tablas (TableView)
- [ ] Layouts y estilos

---

## 🔧 Instalación y Configuración

### Requisitos previos
```bash
- Java 11 o superior
- Maven 3.6 o superior
- Git
```

### Clonar el repositorio
```bash
git clone <url-repositorio>
cd sena-fichas-manager
mkdir -p data
```

### Compilar el proyecto
```bash
mvn clean compile
```

### Ejecutar la aplicación
```bash
mvn javafx:run
```

### Empaquetar en JAR ejecutable
```bash
mvn clean package
```

---

## 🔥 Valor Diferencial

| Antes | Después |
|-------|---------|
| ✗ Excel manual | ✅ Sistema automatizado |
| ✗ Sin validaciones | ✅ Validación inteligente |
| ✗ Dependencia de envío | ✅ Descarga automática |
| ✗ Fácil de equivocarse | ✅ Consistencia garantizada |
| ✗ Sin reportes | ✅ Reportes automáticos |

---

## � Estado Actual del Proyecto

### ✅ Implementado
- Estructura Maven configurada
- Clases modelo base (Ficha.java, Instructor.java, Programa.java, EstadoFicha.java)
- Punto de entrada (Main.java)
- Estructura base de interfaz (MainWindow.java)
- DatabaseManager.java creado

### 🔄 En Desarrollo
- Completar modelo de datos (Competencia, FichaCompetencia)
- Implementar DatabaseManager con SQLite
- Crear ExcelReader para lectura de archivos
- Desarrollar SyncService para la lógica de validación

### 📋 Próximos Pasos

1. **Examinar el Excel** original para documentar su estructura exacta
2. **Completar el modelo de datos** (agregar Competencia.java y FichaCompetencia.java)
3. **Implementar DatabaseManager** (crear tablas y conexión a SQLite)
4. **Desarrollar ExcelReader** (usar Apache POI para leer datos)
5. **Codificar SyncService** (validación y procesamiento de datos)
6. **Diseñar MainWindow** (interfaz gráfica con JavaFX)
7. **Crear módulo de reportes** (exportar resultados)

---

## 💬 Conclusión

No estamos haciendo una app cualquiera. Estamos creando:

🔥 **Un sistema que convierte procesos manuales en automatizados**
🚀 **Una herramienta que elimina errores humanos**
💼 **Una solución integral para la gestión de fichas SENA**

---

## 📄 Licencia

Proyecto desarrollado para SENA

---

**Última actualización:** Abril 2026

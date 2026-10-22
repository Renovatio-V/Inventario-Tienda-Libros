# Tienda de Libros 📚

Sistema de gestión de inventario para una tienda de libros, desarrollado en Java con Spring Boot y una interfaz gráfica moderna en Swing.

---

## 📖 Descripción

**Tienda de Libros** es una aplicación enterprise de gestión de inventario desarrollada en Java. Combina la potencia del backend de **Spring Boot (JPA) + MySQL** con una interfaz gráfica moderna de **Swing** rediseñada bajo el concepto minimalista *Obsidian Library Studio* (Negro Obsidiana), orientada a ofrecer una experiencia visual limpia, rápida y profesional.

---

## ✨ Funcionalidades Actuales

### 🖥️ Interfaz Gráfica
- Paleta cromática minimalista de alto contraste (`#08080A` y `#111115`) con acentos en *Electric Indigo* y *Cyan*.
- **Vista Dual Interactiva:** permite alternar instantáneamente entre una *Galería de Portadas* (Cover Grid) estilo gestor moderno y una *Vista de Tabla Minimalista*.
- **Barra de Telemetría Ejecutiva (KPIs en tiempo real):** muestra el catálogo activo (SKUs), total de unidades en bodega, valorización financiera total del inventario y alertas dinámicas de stock crítico (≤ 5 unidades).
- **Navegación Alfabética (A-Z):** filtro rápido por iniciales del título y buscador instantáneo reactivo.

### 🖼️ Sincronización y Descarga Automática de Portadas
- Integración asíncrona con **Google Books API** y **Open Library API** para buscar y descargar la carátula oficial de cada obra a partir de su título y autor.
- **Caché Local Persistente:** las portadas descargadas se almacenan automáticamente en la carpeta oculta `C:\Users\tu_usuario/.tienda_libros_covers/` de la máquina local para cargar de forma instantánea en futuros arranques.
- **Generador de Portadas Editoriales (Fallback):** si un libro no se encuentra en la web o se registra de manera personalizada, el motor gráfico renderiza una portada editorial minimalista única en alta definición.

### ⚡ Autocompletado Inteligente Bidireccional
- Sugerencias en tiempo real mientras escribes el título del libro, autocompletando automáticamente el autor oficial verificado y previsualizando la portada en el panel inspector.
- Botón de búsqueda dedicada (**⚡ Buscar Autor**) para asegurar la integridad de los metadatos y evitar registros erróneos.

### 🛡️ Protección Anti-Duplicados y Control de Edición
- Validación estricta que impide registrar libros repetidos por título: si intentas agregar una obra existente, el sistema selecciona automáticamente el registro original y avisa al usuario para actualizar su stock en lugar de duplicarlo.
- Bloqueo dinámico de botones según el estado (*Modo Registro* vs *Modo Edición*).

---

## 🛠️ Tecnologías Utilizadas

| Tecnología | Uso |
|---|---|
| Java 22 | Lenguaje principal |
| Spring Boot 3.3.x (JPA / Hibernate) | Backend y persistencia |
| MySQL | Base de datos relacional |
| Swing & Java 2D | Renderizado gráfico personalizado (sin librerías externas de diseño) |
| Jackson & HttpClient | Consumo de APIs asíncronas (metadatos y portadas) |
| Lombok | Reducción de código boilerplate |
| Maven | Gestión de dependencias y empaquetado |

---

## 📂 Estructura del Proyecto

```
src/main/java/Renovatio/Tienda_Libros/
├── TiendaLibrosApplication.java   # Punto de entrada (Spring Boot + Swing)
├── modelo/
│   └── Libro.java                 # Entidad JPA (id, nombre, autor, precio, existencias)
├── repositorio/
│   └── LibroRepositorio.java      # Repositorio JPA (acceso a datos)
├── servicio/
│   ├── ILibroServicio.java        # Contrato de la capa de negocio
│   └── LibroServicio.java         # Lógica de negocio (CRUD)ls
└── vista/
    └── LibroForm.java             # Interfaz gráfica Swing
```

---

## 🚀 Instalación y Configuración

### Prerrequisitos
- Java 22 o superior
- Maven 3.x (o usar el wrapper `mvnw` incluido)
- MySQL en ejecución

### Pasos

1. **Clona tu fork** del repositorio en tu máquina local:

   ```bash
   git clone https://github.com/tu_usuario/tu_repositorio.git
   ```

2. **Navega al directorio del proyecto:**

   ```bash
   cd Inventario-Tienda-Libros
   ```

3. **Configura la base de datos MySQL.** Crea una base de datos llamada `libros_db` y actualiza el archivo `src/main/resources/application.properties` con tus credenciales:

   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/libros_db?createDatabaseIfNotExist=true
   spring.datasource.username=tu_usuario
   spring.datasource.password=tu_contraseña
   spring.jpa.hibernate.ddl-auto=update
   ```

4. **Compila y empaqueta la aplicación** usando Maven:

   ```bash
   mvn clean package
   ```

5. **Ejecuta la aplicación:**

   ```bash
   java -jar target/Tienda_Libros-0.0.1-SNAPSHOT.jar
   ```

---

## 🔮 Funcionalidades Futuras (Roadmap)

Las siguientes mejoras están planteadas para futuras versiones, aplicando lógica de negocio más avanzada sobre la base actual:

### 📦 Gestión de Inventario
- **Movimientos de entrada/salida con historial:** registrar cada venta, devolución o reposición como una transacción auditable, en lugar de solo editar el stock.
- **Alertas automáticas de reposición:** sugerir órdenes de compra a proveedores cuando el stock baje del punto de reorden calculado según la rotación del libro.
- **Gestión de proveedores:** catálogo de editoriales/distribuidores con tiempos de entrega y precios de costo.

### 💰 Lógica de Ventas y Finanzas
- **Módulo de punto de venta (POS):** registrar ventas con tickets/facturas, descuento automático de stock y cálculo de ganancia por transacción.
- **Precios dinámicos y descuentos:** reglas de promociones (porcentaje por categoría, 2x1, descuento por volumen).
- **Reportes financieros:** cierre de caja diario, márgenes de ganancia por libro/autor y proyecciones de valorización del inventario.

### 📊 Analítica e Inteligencia
- **Dashboard de estadísticas:** libros más vendidos, rotación de inventario (ABC), autores y géneros con mayor demanda.
- **Pronóstico de demanda:** estimar ventas futuras a partir del historial para optimizar las compras.
- **Exportación de reportes:** generación de informes en PDF y Excel (inventario, ventas, stock crítico).

### 👥 Usuarios y Seguridad
- **Sistema de roles y autenticación:** administrador, vendedor y bodeguero con permisos diferenciados sobre el catálogo y las ventas.
- **Bitácora de auditoría:** registro de quién modificó qué y cuándo, para trazabilidad completa.

### 🌐 Plataforma e Integraciones
- **API REST:** exponer el catálogo y las operaciones como servicios para integrar con una tienda en línea.
- **Migración a interfaz web/móvil:** frontend en React o app móvil consumiendo la API.
- **Códigos de barras / ISBN:** alta de libros escaneando el ISBN con enriquecimiento automático de metadatos.
- **Modo offline y sincronización multi-sucursal:** operar sin conexión y sincronizar inventarios entre tiendas.

### 🧪 Calidad y DevOps
- **Pruebas unitarias y de integración:** cobertura sobre la capa de servicio y el repositorio.
- **Contenerización con Docker:** despliegue de la app junto con MySQL vía Docker Compose.
- **CI/CD:** pipeline automatizado de compilación, pruebas y empaquetado.

---

## 🖼️ Galería Visual

### Vista General de la Aplicación

![Vista General](https://github.com/user-attachments/assets/2dae852f-b424-4d23-b110-aea9bb4f3328)

*Panel Inspector con autocompletado y portada sincronizada.*

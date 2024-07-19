# Tienda de Libros

## Descripción
Tienda de Libros es una aplicación de gestión de inventario de libros desarrollada en Java. Utiliza Swing para la capa de presentación, Spring Boot para el servicio, repositorio y entidad, y MySQL como base de datos. La aplicación permite realizar operaciones CRUD (Crear, Leer, Actualizar, Eliminar) sobre una base de datos de libros.

## Características
- **Listar Libros**: Muestra una lista de todos los libros disponibles en la base de datos.
- **Agregar Libro**: Permite añadir un nuevo libro a la base de datos.
- **Modificar Libro**: Permite actualizar la información de un libro existente.
- **Eliminar Libro**: Permite eliminar un libro de la base de datos.

## Tecnologías Utilizadas
- **Java 22**
- **Spring Boot 3.3.1**
- **MySQL**
- **Swing**
- **Maven**
- **SpringBoot-Dependencies:** JPA, Lombok, MySQL

## Instalación
1. Clona el repositorio en tu máquina local:
   ```bash
   git clone https://github.com/tu_usuario/tu_repositorio.git

2. Navega al directorio del proyecto.
3. Configura la base de datos MySQL. Crea una base de datos llamada libros_db y actualiza el archivo application.properties con tus credenciales de MySQL:
  ```bash
  spring.datasource.url=jdbc:mysql://localhost:3306/libros_db
  spring.datasource.username=tu_usuario
  spring.datasource.password=tu_contraseña
  spring.jpa.hibernate.ddl-auto=update
  ```
4. Compila y empaqueta la aplicación usando Maven:
  ```bash
  mvn clean package
  ```
5. Ejecuta la aplicación:
  ```bash
  java -jar target/tu_proyecto-1.0.0.jar
  ```
## Visuals
![image](https://github.com/user-attachments/assets/2dae852f-b424-4d23-b110-aea9bb4f3328)








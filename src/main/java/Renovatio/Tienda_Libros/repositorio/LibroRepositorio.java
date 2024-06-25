package Renovatio.Tienda_Libros.repositorio;

import Renovatio.Tienda_Libros.modelo.Libro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibroRepositorio extends JpaRepository<Libro, Integer> {

}

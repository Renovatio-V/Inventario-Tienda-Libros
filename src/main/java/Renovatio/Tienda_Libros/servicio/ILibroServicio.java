package Renovatio.Tienda_Libros.servicio;

import Renovatio.Tienda_Libros.modelo.Libro;

import java.util.List;

public interface ILibroServicio {
    public List<Libro> ListarLibros();
    public Libro buscarLibroPorId(Integer idLibro);
    public void guardarLibro(Libro libro);
    public void eliminarLibro(Libro libro);
}

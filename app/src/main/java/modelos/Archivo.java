package modelos;

import java.io.Serializable;

public class Archivo implements Serializable {
    private String uriArchivo;
    private String nameMetadata;
    int tamano;
    private String extension;
    private String imagen;
    private String descripcion;
    private String fecha_subida,ultimaMod;
    private boolean isFavorito;
    private boolean isCompartido;
    private boolean isCarpeta;
    private String Autor;

    public boolean isCarpeta() {
        return isCarpeta;
    }
    public Archivo(String uriArchivo, String nameMetadata, String extension,boolean isFavorito,boolean isCompartido,boolean isCarpeta) {
        this.uriArchivo = uriArchivo;
        this.nameMetadata = nameMetadata;
        this.extension = extension;
        this.isFavorito = isFavorito;
        this.isCompartido = isCompartido;
        this.isCarpeta = isCarpeta;
    }

    public int getTamano() {
        return tamano;
    }

    public void setTamano(int tamano) {
        this.tamano = tamano;
    }

    public Archivo(String uriArchivo, String nameMetadata) {
        this.uriArchivo = uriArchivo;
        this.nameMetadata = nameMetadata;
        this.isFavorito = false;
    }


    public Archivo(String uriArchivo, String nameMetadata, String extension, String imagen) {
        this.uriArchivo = uriArchivo;
        this.nameMetadata = nameMetadata;
        this.extension = extension;
        this.imagen = imagen;
        this.isFavorito = false;
    }

    public String getAutor() {
        return Autor;
    }

    public void setAutor(String autor) {
        Autor = autor;
    }

    public String getFecha_subida() {
        return fecha_subida;
    }

    public void setFecha_subida(String fecha_subida) {
        this.fecha_subida = fecha_subida;
    }

    public String getUltimaMod() {
        return ultimaMod;
    }

    public void setUltimaMod(String ultimaMod) {
        this.ultimaMod = ultimaMod;
    }

    public void setCarpeta(boolean carpeta) {
        isCarpeta = carpeta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isCompartido() {
        return isCompartido;
    }

    public void setCompartido(boolean compartido) {
        isCompartido = compartido;
    }

    public String getUriArchivo() {
        return uriArchivo;
    }

    public void setUriArchivo(String uriArchivo) {
        this.uriArchivo = uriArchivo;
    }

    public String getNameMetadata() {
        return nameMetadata;
    }

    public void setNameMetadata(String nameMetadata) {
        this.nameMetadata = nameMetadata;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public void setFavorito(boolean favorito) {
        isFavorito = favorito;
    }

    public boolean isFavorito() {
        return isFavorito;
    }
}

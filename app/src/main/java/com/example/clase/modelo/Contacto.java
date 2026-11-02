package com.example.clase.modelo;

public class Contacto {
    private final long id;
    private final String nombre;
    private final String telefono;
    private final String email;

    public Contacto(long id, String nombre, String telefono, String email) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }

    @Override
    public String toString() { return nombre; }
}

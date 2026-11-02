package com.example.clase.modelo;

public class PuntoMapa {
    private final long id;
    private final String nombre;
    private final double latitud;
    private final double longitud;

    public PuntoMapa(long id, String nombre, double latitud, double longitud) {
        this.id = id;
        this.nombre = nombre;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
}

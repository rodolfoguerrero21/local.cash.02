package com.example.clase.modelo;

public class ProductoCompra {
    private final String nombre;
    private final int cantidad;
    private final double precioUnitario;

    public ProductoCompra(String nombre, int cantidad, double precioUnitario) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public String getNombre() { return nombre; }
    public int getCantidad() { return cantidad; }
    public double getPrecioUnitario() { return precioUnitario; }
    public double getSubtotal() { return cantidad * precioUnitario; }
}

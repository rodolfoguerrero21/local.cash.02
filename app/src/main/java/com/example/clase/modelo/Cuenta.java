package com.example.clase.modelo;

public class Cuenta {
    private final long id;
    private final String nombre;
    private final String tipo;
    private final double saldo;

    public Cuenta(long id, String nombre, String tipo, double saldo) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.saldo = saldo;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public double getSaldo() { return saldo; }

    @Override
    public String toString() {
        return nombre + " (" + tipo + ")";
    }
}

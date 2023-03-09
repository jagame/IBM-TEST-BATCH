package com.jagame.ibm.test.supplier.domain;

import java.time.LocalDate;
import java.util.Objects;

public class Supplier {

    private Long idProveedor;
    private String nombre;
    private String fechaAlta;
    private Long idCliente;

    public Long getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(Long idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(String fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Supplier supplier = (Supplier) o;
        return Objects.equals(idProveedor, supplier.idProveedor) && Objects.equals(nombre, supplier.nombre) && Objects.equals(fechaAlta, supplier.fechaAlta) && Objects.equals(idCliente, supplier.idCliente);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProveedor, nombre, fechaAlta, idCliente);
    }
}

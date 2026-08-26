package com.example.topologyinventory.domain.entity;

import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.domain.vo.Location;
import com.example.topologyinventory.domain.vo.Model;
import com.example.topologyinventory.domain.vo.Vendor;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Predicate;

/**
 * Entidad raíz de la jerarquía de equipos de red. Reúne los atributos comunes a
 * todo dispositivo físico del inventario (identidad, fabricante, modelo, IP y
 * ubicación). Es abstracta porque un "equipo" nunca existe por sí solo: siempre
 * es un {@link Router} o un {@link Switch}.
 */
@Getter
@AllArgsConstructor
public abstract class Equipment {

    /** Identidad única del equipo dentro del inventario. */
    protected Id id;
    /** Fabricante del equipo. */
    protected Vendor vendor;
    /** Modelo del equipo. */
    protected Model model;
    /** Dirección IP asignada al equipo. */
    protected IP ip;
    /** Ubicación física del equipo. */
    protected Location location;

    /**
     * Predicado para filtrar colecciones de equipos por fabricante.
     *
     * @param vendor fabricante buscado
     * @return predicado que acepta los equipos de ese fabricante
     */
    public static Predicate<Equipment> getVendorPredicate(Vendor vendor) {
        return equipment -> equipment.getVendor().equals(vendor);
    }
}
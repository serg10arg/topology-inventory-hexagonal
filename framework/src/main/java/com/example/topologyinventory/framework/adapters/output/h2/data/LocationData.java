package com.example.topologyinventory.framework.adapters.output.h2.data;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Espejo de persistencia del value object {@code Location} del dominio.
 *
 * Se modela como {@code @Embeddable}: al no tener identidad propia en el dominio,
 * sus columnas se embeben en las tablas de las entidades que lo contienen
 * (routers y switches) mediante {@code @AttributeOverrides}, en lugar de vivir en
 * una tabla {@code location} con clave primaria. Así se elimina el desajuste
 * value-object/entidad que impedía persistir un router con una ubicación nueva
 * (el {@code @ManyToOne} sin cascade a un LocationData transitorio).
 */
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class LocationData {

    private String address;

    private String city;

    private String state;

    private int zipcode;

    private String country;

    private float latitude;

    private float longitude;
}

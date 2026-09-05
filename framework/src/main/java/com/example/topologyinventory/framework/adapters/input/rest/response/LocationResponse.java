package com.example.topologyinventory.framework.adapters.input.rest.response;

import com.example.topologyinventory.domain.vo.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO de salida para la ubicación de un equipo. Espeja su gemelo de entrada
 * ({@code LocationRequest}), pero se mantiene separado a propósito: entrada y salida son
 * contratos independientes que pueden evolucionar por su cuenta. Inmutable; se construye
 * desde el dominio con {@link #from(Location)}.
 */
@Getter
@AllArgsConstructor
public class LocationResponse {

    private final String address;
    private final String city;
    private final String state;
    private final int zipCode;
    private final String country;
    private final float latitude;
    private final float longitude;

    /** Proyecta el value object {@link Location} del dominio a su DTO de salida. */
    public static LocationResponse from(Location location) {
        return new LocationResponse(
                location.getAddress(),
                location.getCity(),
                location.getState(),
                location.getZipCode(),
                location.getCountry(),
                location.getLatitude(),
                location.getLongitude());
    }
}

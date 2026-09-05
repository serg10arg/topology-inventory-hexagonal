package com.example.topologyinventory.framework.adapters.input.rest.request;

import com.example.topologyinventory.domain.vo.Location;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para la ubicación física de un equipo. Es un objeto plano de la
 * frontera REST: Jackson lo instancia con el constructor sin argumentos y lo puebla
 * por los setters, evitando el {@code LocationDeserializer} a medida que el libro
 * necesita porque {@link Location} del dominio no tiene constructor sin-args.
 *
 * <p>Su única responsabilidad es traducirse al value object del dominio con
 * {@link #toDomain()}; ninguna regla vive aquí.
 */
@Getter
@Setter
@NoArgsConstructor
public class LocationRequest {

    private String address;
    private String city;
    private String state;
    private int zipCode;
    private String country;
    private float latitude;
    private float longitude;

    /** Traduce este DTO al value object {@link Location} del dominio. */
    public Location toDomain() {
        return Location.builder()
                .address(address)
                .city(city)
                .state(state)
                .zipCode(zipCode)
                .country(country)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}

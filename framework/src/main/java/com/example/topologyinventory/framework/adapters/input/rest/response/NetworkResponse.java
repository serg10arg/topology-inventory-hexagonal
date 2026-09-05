package com.example.topologyinventory.framework.adapters.input.rest.response;

import com.example.topologyinventory.domain.vo.Network;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO de salida de una red. Una {@link Network} es un objeto de valor hoja (sin colecciones
 * perezosas), así que se proyecta completa sin riesgo. Inmutable; se construye desde el
 * dominio con {@link #from(Network)}.
 */
@Getter
@AllArgsConstructor
public class NetworkResponse {

    private final String networkAddress;
    private final String networkName;
    private final int networkCidr;

    /** Proyecta el value object {@link Network} del dominio a su DTO de salida. */
    public static NetworkResponse from(Network network) {
        return new NetworkResponse(
                network.getNetworkAddress().getIpAddress(),
                network.getNetworkName(),
                network.getNetworkCidr());
    }
}

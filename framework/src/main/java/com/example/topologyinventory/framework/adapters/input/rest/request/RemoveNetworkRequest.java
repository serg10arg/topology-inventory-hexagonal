package com.example.topologyinventory.framework.adapters.input.rest.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para quitar una red de un switch. La red se identifica por su nombre
 * dentro del switch (que a su vez se alcanza por la clave compuesta edge router + switch):
 * localizar por nombre evita depender de la igualdad por valor de la IP al reconstruir la red.
 */
@Getter
@Setter
@NoArgsConstructor
public class RemoveNetworkRequest {

    private String edgeRouterId;
    private String switchId;
    private String networkName;
}

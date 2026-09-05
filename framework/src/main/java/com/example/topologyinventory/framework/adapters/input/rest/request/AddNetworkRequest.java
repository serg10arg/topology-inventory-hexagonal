package com.example.topologyinventory.framework.adapters.input.rest.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para crear una red y añadirla a un switch en un solo paso. Una red vive
 * dentro de un switch, y un switch dentro de un edge router; como ninguno de los dos se
 * recupera de forma independiente, para alcanzar el switch se necesita la clave compuesta
 * (edge router + switch). El adapter recupera el edge, localiza el switch, crea la red y la añade.
 */
@Getter
@Setter
@NoArgsConstructor
public class AddNetworkRequest {

    private String networkAddress;
    private String networkName;
    private int networkCidr;
    private String edgeRouterId;
    private String switchId;
}

package com.example.topologyinventory.framework.adapters.input.rest.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para crear una red (en memoria, sin asociarla a un switch). La dirección
 * de red viaja como {@code String}; el CIDR debe respetar el mínimo del dominio (>= 8).
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateNetworkRequest {

    private String networkAddress;
    private String networkName;
    private int networkCidr;
}

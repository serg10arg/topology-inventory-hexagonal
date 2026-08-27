package com.example.topologyinventory.domain.service;

import com.example.topologyinventory.domain.vo.Network;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Servicio de dominio para operaciones de consulta sobre redes. Solo ofrece
 * filtrado: a diferencia de routers y switches, una {@link Network} es un value
 * object sin identidad, por lo que no existe búsqueda por Id.
 */
public class NetworkService {

    /**
     * Filtra una lista de redes aplicando el predicado indicado (por ejemplo, por
     * protocolo IPv4/IPv6).
     *
     * @param networks         redes a filtrar
     * @param networkPredicate criterio de selección
     * @return las redes que satisfacen el predicado
     */
    public static List<Network> filterAndRetrieveNetworks(List<Network> networks, Predicate<Network> networkPredicate) {
        return networks.stream()
                .filter(networkPredicate)
                .collect(Collectors.toList());
    }
}
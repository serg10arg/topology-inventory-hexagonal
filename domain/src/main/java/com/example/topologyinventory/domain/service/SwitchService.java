package com.example.topologyinventory.domain.service;

import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.vo.Id;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Servicio de dominio para operaciones de consulta sobre switches. Igual que
 * {@link RouterService}, agrupa lógica de filtrado y búsqueda por identidad que
 * es transversal a la colección y no propia de un único {@link Switch}.
 */
public class SwitchService {

    /**
     * Filtra una lista de switches aplicando el predicado indicado (por tipo de
     * switch, protocolo de red, etc.).
     *
     * @param switches        switches a filtrar
     * @param switchPredicate criterio de selección
     * @return los switches que satisfacen el predicado
     */
    public static List<Switch> filterAndRetrieveSwitch(List<Switch> switches, Predicate<Switch> switchPredicate) {
        return switches.stream()
                .filter(switchPredicate)
                .collect(Collectors.toList());
    }

    /**
     * Busca un switch por su identidad dentro de un mapa indexado por {@link Id}.
     *
     * @param switches mapa de switches indexado por Id
     * @param id       identidad buscada
     * @return el switch asociado a ese Id, o {@code null} si no existe
     */
    public static Switch findById(Map<Id, Switch> switches, Id id) {
        return switches.get(id);
    }
}
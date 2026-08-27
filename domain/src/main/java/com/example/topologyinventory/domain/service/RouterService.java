package com.example.topologyinventory.domain.service;

import com.example.topologyinventory.domain.entity.Equipment;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.Id;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Servicio de dominio para operaciones de consulta sobre routers que no
 * pertenecen a ninguna entity concreta. Reúne lógica transversal —filtrar una
 * colección o buscar por identidad— que no tendría sentido colocar dentro de un
 * único {@link Router}. Es stateless: todos sus métodos son estáticos.
 */
public class RouterService {

    /**
     * Filtra una lista de routers aplicando el predicado indicado. El predicado
     * suele provenir de las fábricas de {@link Router} / {@link Equipment}
     * (por tipo, modelo, país o fabricante).
     *
     * @param routers         routers a filtrar
     * @param routerPredicate criterio de selección
     * @return los routers que satisfacen el predicado
     */
    public static List<Router> filterAndRetrieveRouter(List<Router> routers, Predicate<Equipment> routerPredicate) {
        return routers.stream()
                .filter(routerPredicate)
                .collect(Collectors.toList());
    }

    /**
     * Busca un router por su identidad dentro de un mapa indexado por {@link Id}.
     *
     * @param routers mapa de routers indexado por Id
     * @param id      identidad buscada
     * @return el router asociado a ese Id, o {@code null} si no existe
     */
    public static Router findById(Map<Id, Router> routers, Id id) {
        return routers.get(id);
    }
}
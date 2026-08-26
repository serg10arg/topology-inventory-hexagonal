package com.example.topologyinventory.domain.entity;

import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.domain.vo.Location;
import com.example.topologyinventory.domain.vo.Model;
import com.example.topologyinventory.domain.vo.RouterType;
import com.example.topologyinventory.domain.vo.Vendor;
import lombok.Getter;

import java.util.function.Predicate;

/**
 * Especialización abstracta de {@link Equipment} para los routers. Añade el tipo
 * de router (core o edge) y los predicados de filtrado comunes a ambos. Las
 * subclases {@link CoreRouter} y {@link EdgeRouter} definen las capacidades
 * propias de cada tipo.
 */
@Getter
public abstract class Router extends Equipment {

    /** Tipo de router (CORE o EDGE); inmutable una vez creado. */
    protected final RouterType routerType;

    public Router(Id id, Vendor vendor, Model model, IP ip, Location location, RouterType routerType) {
        super(id, vendor, model, ip, location);
        this.routerType = routerType;
    }

    /** Predicado para filtrar routers por su tipo (core/edge). */
    public static Predicate<Equipment> getRouterTypePredicate(RouterType routerType) {
        return equipment -> ((Router) equipment).getRouterType().equals(routerType);
    }

    /** Predicado para filtrar equipos por modelo. */
    public static Predicate<Equipment> getModelPredicate(Model model) {
        return equipment -> equipment.getModel().equals(model);
    }

    /** Predicado para filtrar equipos por país (a partir de su ubicación). */
    public static Predicate<Equipment> getCountryPredicate(Location location) {
        return equipment -> equipment.getLocation().getCountry().equals(location.getCountry());
    }
}
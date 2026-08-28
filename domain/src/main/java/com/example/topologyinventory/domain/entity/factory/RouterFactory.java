package com.example.topologyinventory.domain.entity.factory;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.*;

/**
 * Fábrica de routers. Centraliza la creación de la subclase correcta de
 * {@link Router} según el {@link RouterType}, de modo que el hexágono de
 * aplicación no necesite conocer las clases concretas {@link CoreRouter} y
 * {@link EdgeRouter}. Si el Id es nulo, genera uno nuevo.
 */
public class RouterFactory {

    /**
     * Crea el router del tipo indicado.
     *
     * @param id         identidad a asignar; si es {@code null} se genera una nueva
     * @param vendor     fabricante
     * @param model      modelo
     * @param ip         dirección IP
     * @param location   ubicación física
     * @param routerType tipo de router, que selecciona la subclase a instanciar
     * @return un {@link CoreRouter} o un {@link EdgeRouter} según {@code routerType}
     */
    public static Router getRouter(Id id, Vendor vendor, Model model,
                                   IP ip, Location location, RouterType routerType) {
        // switch expression (Java 21): exhaustivo sobre el enum, sin rama default muerta.
        return switch (routerType) {
            case CORE -> CoreRouter.builder()
                    .id(id == null ? Id.withoutId() : id)
                    .vendor(vendor).model(model).ip(ip)
                    .location(location).routerType(routerType).build();
            case EDGE -> EdgeRouter.builder()
                    .id(id == null ? Id.withoutId() : id)
                    .vendor(vendor).model(model).ip(ip)
                    .location(location).routerType(routerType).build();
        };
    }
}
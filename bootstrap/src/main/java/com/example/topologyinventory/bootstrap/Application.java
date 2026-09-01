package com.example.topologyinventory.bootstrap;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.vo.*;
import com.example.topologyinventory.framework.adapters.input.generic.RouterManagementGenericAdapter;

/**
 * Punto de arranque y raíz de composición de la aplicación.
 *
 * Ensambla el sistema construyendo los generic adapters (que se auto-cablean con
 * su constructor sin argumentos) y ejecuta un flujo demostrativo que recorre el
 * sistema de punta a punta. Es la primera vez que el cableado por
 * {@code ServiceLoader} se resuelve en una aplicación en ejecución (no en un
 * test): al estar el módulo framework en el grafo, el output port de H2 queda
 * disponible.
 *
 * <p>El flujo persiste un core router <em>sin hijos</em> (camino de salida
 * completo: crear → persistir → recuperar contra H2) y luego crea y conecta un
 * edge <em>en memoria</em> (camino de dominio). No se persiste el agregado
 * conectado: eso ejercitaría la relación {@code @OneToMany} sin cascade de
 * RouterData, un asunto de mapeo aparte del arranque.
 *
 * <p>La base H2 es en memoria y efímera: lo que aquí se persiste vive solo
 * mientras corre el proceso. Es una demostración del flujo, no un almacén.
 */
public class Application {

    public static void main(String[] args) {
        var routerAdapter = new RouterManagementGenericAdapter();

        var location = Location.builder()
                .address("Amos Ln").city("Tully").state("NY").zipCode(13159)
                .country("United States").latitude(42.79731f).longitude(-76.13075f)
                .build();

        System.out.println("== topology-inventory :: arranque ==");

        // 1) Camino de salida completo: crear → persistir → recuperar (round-trip contra H2).
        var core = (CoreRouter) routerAdapter.createRouter(
                Vendor.CISCO, Model.XYZ0001, IP.fromAddress("1.0.0.1"),
                location, RouterType.CORE);
        System.out.println("Core creado: " + core.getId());

        routerAdapter.persistRouter(core);
        System.out.println("Core persistido en H2.");

        var retrieved = routerAdapter.retrieveRouter(core.getId());
        System.out.println("Core recuperado desde H2: " + retrieved.getId()
                + " [" + retrieved.getRouterType() + ", "
                + retrieved.getLocation().getCity() + "]");

        // 2) Camino de dominio: crear un edge y conectarlo al core (en memoria).
        var edge = (EdgeRouter) routerAdapter.createRouter(
                Vendor.JUNIPER, Model.XYZ0001, IP.fromAddress("5.0.0.5"),
                location, RouterType.EDGE);
        var connectedCore = routerAdapter.addRouterToCoreRouter(edge, core);
        System.out.println("Edge " + edge.getId() + " conectado al core; "
                + "routers en el core: " + connectedCore.getRouters().size());

        System.out.println("== fin ==");
    }
}

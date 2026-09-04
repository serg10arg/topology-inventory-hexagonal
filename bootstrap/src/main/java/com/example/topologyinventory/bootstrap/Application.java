package com.example.topologyinventory.bootstrap;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.vo.*;
import com.example.topologyinventory.framework.adapters.input.generic.RouterManagementGenericAdapter;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

/**
 * Punto de arranque de la aplicación bajo Quarkus (raíz de composición).
 *
 * Anotada con {@link QuarkusMain}, es el punto de entrada real: Quarkus arranca primero
 * el contenedor —Arc, el datasource Agroal, Hibernate ORM y el seed— y solo entonces
 * invoca {@link #run(String...)}. Como corre bajo el contenedor vivo, esta clase es un
 * bean gestionado y recibe el adapter de entrada por {@code @Inject}: la raíz de
 * composición también deja de construir sus colaboradores con {@code new}. Es la primera
 * vez que la resolución por CDI se ejercita en una aplicación Quarkus en ejecución (no en
 * un test), con toda la cadena router cableada por Arc.
 *
 * <p>Se ejecuta en <em>command mode</em>: {@code run} realiza el flujo y devuelve un
 * código de salida, con lo que la aplicación termina (no queda como servicio; el adapter
 * HTTP que la mantendría viva es trabajo de una fase posterior).
 *
 * <p>El flujo persiste un core router <em>sin hijos</em> (camino de salida completo:
 * crear → persistir → recuperar contra H2) y luego crea y conecta un edge <em>en
 * memoria</em> (camino de dominio). No se persiste el agregado conectado: eso ejercitaría
 * la relación {@code @OneToMany} sin cascade de RouterData, un asunto de mapeo aparte.
 *
 * <p>La base H2 es en memoria y efímera: lo que aquí se persiste vive solo mientras corre
 * el proceso. Es una demostración del flujo, no un almacén.
 */
@QuarkusMain
public class Application implements QuarkusApplication {

    /**
     * Adapter de entrada, inyectado por Quarkus. En command mode el contenedor gestiona
     * esta clase como bean y satisface sus dependencias antes de invocar
     * {@link #run(String...)}.
     */
    @Inject
    RouterManagementGenericAdapter routerAdapter;

    @Override
    public int run(String... args) {
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
        return 0;
    }
}

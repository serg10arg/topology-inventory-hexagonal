package com.example.topologyinventory.framework.adapters.input.generic;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.vo.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test end-to-end del lado <em>driving</em>: entra por los generic adapters (la "puerta de
 * entrada" del sistema, base del futuro adapter REST) y recorre dos caminos completos —la
 * jerarquía del agregado en memoria y la persistencia, que atraviesa los tres hexágonos:
 * adapter de entrada → use case → puerto de salida → output adapter de H2—. Que la malla
 * driving completa (router, switch, red) se resuelva por {@code @Inject} prueba que el
 * cableado CDI funciona sin que el hexágono de application conozca las clases concretas.
 *
 * <p>Los tres adapters se obtienen por {@code @Inject} (beans CDI): además de la lógica de
 * dominio, este test verifica que Arc arma el grafo driving completo sin ambigüedad ni
 * ciclos. Un fallo de cableado (bean ausente, resolución dudosa) caería en el arranque del
 * contenedor, no en un assert.
 *
 * <p>Los fixtures respetan las reglas de dominio (specifications): misma ubicación (mismo
 * país) en toda la cadena, IPs distintas entre core, edge y switch, y un CIDR válido
 * (&gt;= 8) para la red.
 */
@QuarkusTest
class GenericAdaptersEndToEndTest {

    /** EDGE router semilla insertado por import.sql (no lo escribe ningún test). */
    private static final String SEEDED_EDGE_ROUTER_ID = "b832ef4f-f894-4194-8feb-a99c2cd4be0a";

    private static final Location LOCATION = Location.builder()
            .address("Amos Ln").city("Tully").state("NY").zipCode(13159)
            .country("United States").latitude(42.79731f).longitude(-76.13075f)
            .build();

    @Inject
    RouterManagementGenericAdapter routerAdapter;

    @Inject
    SwitchManagementGenericAdapter switchAdapter;

    @Inject
    NetworkManagementGenericAdapter networkAdapter;

    @Test
    @DisplayName("e2e: crear y conectar la jerarquía core → edge → switch → red")
    void createAndConnectHierarchy() {
        // core ← edge
        var core = (CoreRouter) routerAdapter.createRouter(
                Vendor.CISCO, Model.XYZ0001, IP.fromAddress("1.0.0.1"),
                LOCATION, RouterType.CORE);
        var edge = (EdgeRouter) routerAdapter.createRouter(
                Vendor.JUNIPER, Model.XYZ0001, IP.fromAddress("5.0.0.5"),
                LOCATION, RouterType.EDGE);
        var coreWithEdge = routerAdapter.addRouterToCoreRouter(edge, core);
        assertEquals(1, coreWithEdge.getRouters().size(),
                "el core debería tener 1 router conectado");

        // edge ← switch
        var networkSwitch = switchAdapter.createSwitch(
                Vendor.CISCO, Model.XYZ0002, IP.fromAddress("9.0.0.9"),
                LOCATION, SwitchType.LAYER3);
        var edgeWithSwitch = switchAdapter.addSwitchToEdgeRouter(networkSwitch, edge);
        assertEquals(1, edgeWithSwitch.getSwitches().size(),
                "el edge debería tener 1 switch conectado");

        // switch ← red
        var network = networkAdapter.createNetwork(IP.fromAddress("10.0.0.0"), "HR", 8);
        var switchWithNetwork = networkAdapter.addNetworkToSwitch(network, networkSwitch);
        assertEquals(1, switchWithNetwork.getSwitchNetworks().size(),
                "el switch debería tener 1 red conectada");
    }

    /**
     * Round-trip por el adapter de entrada: persistir un router nuevo y volver a recuperarlo.
     * Con el {@code EntityManager} transaction-scoped, persistir y recuperar son
     * transacciones separadas, así que la lectura no se sirve de la caché de primer nivel:
     * lee de H2 tras el commit del persist.
     */
    @Test
    @DisplayName("e2e: persistir y recuperar un router por el adapter de entrada")
    void persistAndRetrieveRouter() {
        var core = routerAdapter.createRouter(
                Vendor.CISCO, Model.XYZ0001, IP.fromAddress("3.0.0.3"),
                LOCATION, RouterType.CORE);

        assertDoesNotThrow(() -> routerAdapter.persistRouter(core),
                "persistir por el adapter debería resolver el puerto de salida y confirmar");

        var retrieved = routerAdapter.retrieveRouter(core.getId());
        assertNotNull(retrieved, "el router persistido debería recuperarse");
        assertEquals(core.getId(), retrieved.getId());
        assertEquals(RouterType.CORE, retrieved.getRouterType());
        assertEquals("Tully", retrieved.getLocation().getCity());
    }

    /**
     * Lectura real desde H2 por la puerta de entrada: el router semilla no lo ha escrito
     * ningún test, así que no puede venir de la caché del {@code EntityManager}.
     */
    @Test
    @DisplayName("e2e: recuperar un router semilla por el adapter de entrada")
    void retrieveSeededRouter() {
        var retrieved = routerAdapter.retrieveRouter(Id.withId(SEEDED_EDGE_ROUTER_ID));

        assertNotNull(retrieved, "el router semilla debería recuperarse por el adapter");
        assertEquals(RouterType.EDGE, retrieved.getRouterType());
        assertEquals("Tully", retrieved.getLocation().getCity());
    }
}

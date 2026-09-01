package com.example.topologyinventory.framework.adapters.input.generic;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.vo.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test end-to-end del lado <em>driving</em>: entra por los generic adapters (la
 * "puerta de entrada" del sistema, base del futuro adapter REST) y recorre dos
 * caminos completos —la jerarquía del agregado en memoria y la persistencia, que
 * atraviesa los tres hexágonos: adapter de entrada → use case → puerto de salida
 * resuelto por {@code ServiceLoader} → output adapter de H2—. Que el
 * {@code ServiceLoader} resuelva aquí prueba que el binding JPMS
 * {@code provides}/{@code uses} funciona sin que el hexágono de application
 * conozca la clase concreta.
 *
 * <p>Los fixtures respetan las reglas de dominio (specifications): misma
 * ubicación (mismo país) en toda la cadena, IPs distintas entre core, edge y
 * switch, y un CIDR válido (&gt;= 8) para la red. Un dato que violara una regla
 * haría fallar el test por el motivo equivocado.
 */
class GenericAdaptersEndToEndTest {

    /** EDGE router semilla insertado por data.sql (no lo escribe ningún test). */
    private static final String SEEDED_EDGE_ROUTER_ID = "b832ef4f-f894-4194-8feb-a99c2cd4be0a";

    private static final Location LOCATION = Location.builder()
            .address("Amos Ln").city("Tully").state("NY").zipCode(13159)
            .country("United States").latitude(42.79731f).longitude(-76.13075f)
            .build();

    @Test
    @DisplayName("e2e: crear y conectar la jerarquía core → edge → switch → red")
    void createAndConnectHierarchy() {
        var routerAdapter = new RouterManagementGenericAdapter();
        var switchAdapter = new SwitchManagementGenericAdapter();
        var networkAdapter = new NetworkManagementGenericAdapter();

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
     * Round-trip por el adapter de entrada: persistir un router nuevo y volver a
     * recuperarlo. Posible desde que {@code LocationData} pasó a ser
     * {@code @Embeddable}: mientras la ubicación fue una entidad con PK propia,
     * un router recién persistido quedaba sin {@code location_id} válido y
     * releerlo fallaba.
     *
     * <p>El output adapter es un singleton con un único {@code EntityManager}, así
     * que esta lectura puede servirse de su caché de primer nivel. Eso está bien
     * para lo que este test afirma —el ida y vuelta por la puerta de entrada— y no
     * lo invalida: la lectura contra disco la cubre {@link #retrieveSeededRouter()},
     * que pide un router que ningún test ha escrito.
     */
    @Test
    @DisplayName("e2e: persistir y recuperar un router por el adapter de entrada")
    void persistAndRetrieveRouter() {
        var routerAdapter = new RouterManagementGenericAdapter();

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
     * Lectura real desde H2 por la puerta de entrada: el router semilla no lo ha
     * escrito ningún test, así que no puede venir de la caché del
     * {@code EntityManager}.
     */
    @Test
    @DisplayName("e2e: recuperar un router semilla por el adapter de entrada")
    void retrieveSeededRouter() {
        var routerAdapter = new RouterManagementGenericAdapter();

        var retrieved = routerAdapter.retrieveRouter(Id.withId(SEEDED_EDGE_ROUTER_ID));

        assertNotNull(retrieved, "el router semilla debería recuperarse por el adapter");
        assertEquals(RouterType.EDGE, retrieved.getRouterType());
        assertEquals("Tully", retrieved.getLocation().getCity());
    }
}

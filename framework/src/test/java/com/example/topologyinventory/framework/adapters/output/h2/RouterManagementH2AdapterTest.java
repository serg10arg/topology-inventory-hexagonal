package com.example.topologyinventory.framework.adapters.output.h2;

import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.entity.factory.RouterFactory;
import com.example.topologyinventory.domain.vo.*;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración del output adapter de persistencia. Es la primera vez que
 * el sistema arranca EclipseLink de verdad y habla con la base de datos H2
 * (unidad de persistencia "inventory", cargada con el esquema de
 * {@code schema.sql} y los datos semilla de {@code data.sql}).
 *
 * <p>El adapter es un singleton con un único {@code EntityManager}, así que su
 * caché de primer nivel podría servir una lectura sin tocar disco. Para evitar
 * ese falso positivo, los dos caminos se prueban por separado:
 * <ul>
 *   <li><b>Lectura</b>: se recupera un router <em>semilla</em> (no escrito por el
 *       test), lo que fuerza una lectura real desde H2 y ejercita el mapper de
 *       vuelta, el {@code UUIDTypeConverter} y la resolución del {@code Location}.</li>
 *   <li><b>Escritura</b>: se persiste un router nuevo y se comprueba que la
 *       transacción {@code RESOURCE_LOCAL} confirma sin lanzar.</li>
 * </ul>
 *
 * <p>Nota: aquí no se hace round-trip de un mismo router porque estos dos tests
 * existen justamente para separar los caminos. El ida y vuelta completo —posible
 * desde que {@code LocationData} es {@code @Embeddable}— lo cubre el test
 * end-to-end de los generic adapters.
 */
@QuarkusTest
class RouterManagementH2AdapterTest {

    /** EDGE router semilla insertado por import.sql (JUNIPER/XYZ0001, ubicación en Tully). */
    private static final String SEEDED_EDGE_ROUTER_ID = "b832ef4f-f894-4194-8feb-a99c2cd4be0a";

    @Test
    @DisplayName("retrieveRouter recupera y mapea un router semilla desde H2")
    void retrieveSeededRouter_shouldMapFromDatabase() {
        var adapter = new RouterManagementH2Adapter();

        var router = adapter.retrieveRouter(Id.withId(SEEDED_EDGE_ROUTER_ID));

        assertNotNull(router, "El router semilla debería recuperarse desde la base de datos");
        assertEquals(Id.withId(SEEDED_EDGE_ROUTER_ID), router.getId());
        assertEquals(RouterType.EDGE, router.getRouterType());
        assertEquals(Vendor.JUNIPER, router.getVendor());
        assertEquals(Model.XYZ0001, router.getModel());
        // Prueba de que las columnas @Embedded de Location se leyeron de la fila semilla.
        assertEquals("Tully", router.getLocation().getCity());
    }

    @Test
    @DisplayName("persistRouter confirma la transacción sin lanzar")
    void persistNewCoreRouter_shouldCommitWithoutError() {
        var adapter = new RouterManagementH2Adapter();
        var newRouter = newCoreRouterFixture(
                Id.withId("11111111-1111-1111-1111-111111111111"));

        assertDoesNotThrow(() -> adapter.persistRouter(newRouter),
                "Persistir un router nuevo debería confirmar la transacción sin error");
    }

    /**
     * Construye un {@code CoreRouter} sin hijos con un id conocido. Se usa
     * {@link RouterFactory} (la misma vía que el caso de uso) para no acoplar el
     * test a las clases concretas del dominio.
     */
    private Router newCoreRouterFixture(Id id) {
        var location = Location.builder()
                .address("Amos Ln").city("Tully").state("NY").zipCode(13159)
                .country("United States").latitude(42.79731f).longitude(-76.13075f)
                .build();

        return RouterFactory.getRouter(
                id, Vendor.CISCO, Model.XYZ0001, IP.fromAddress("1.0.0.1"),
                location, RouterType.CORE);
    }
}
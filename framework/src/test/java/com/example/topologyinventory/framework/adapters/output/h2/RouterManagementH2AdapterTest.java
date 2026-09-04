package com.example.topologyinventory.framework.adapters.output.h2;

import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.entity.factory.RouterFactory;
import com.example.topologyinventory.domain.vo.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración del output adapter de persistencia. Arranca el contenedor de Quarkus
 * ({@code @QuarkusTest}) —Arc + Hibernate ORM sobre H2, con el esquema generado desde las
 * entidades y los datos semilla de {@code import.sql}— y ejercita el adapter como bean CDI.
 *
 * <p>El adapter se obtiene por {@code @Inject}: es un bean {@code @ApplicationScoped} cuyo
 * {@code EntityManager} inyectado es transaction-scoped, de modo que cada método corre en su
 * propia transacción ({@code @Transactional}). Los dos caminos se prueban por separado:
 * <ul>
 *   <li><b>Lectura</b>: se recupera un router <em>semilla</em> (no escrito por el test), lo
 *       que fuerza una lectura real desde H2 y ejercita el mapper de vuelta y la resolución
 *       del {@code Location}.</li>
 *   <li><b>Escritura</b>: se persiste un router nuevo y se comprueba que la transacción
 *       confirma sin lanzar.</li>
 * </ul>
 */
@QuarkusTest
class RouterManagementH2AdapterTest {

    /** EDGE router semilla insertado por import.sql (JUNIPER/XYZ0001, ubicación en Tully). */
    private static final String SEEDED_EDGE_ROUTER_ID = "b832ef4f-f894-4194-8feb-a99c2cd4be0a";

    @Inject
    RouterManagementH2Adapter adapter;

    @Test
    @DisplayName("retrieveRouter recupera y mapea un router semilla desde H2")
    void retrieveSeededRouter_shouldMapFromDatabase() {
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
        var newRouter = newCoreRouterFixture(
                Id.withId("11111111-1111-1111-1111-111111111111"));

        assertDoesNotThrow(() -> adapter.persistRouter(newRouter),
                "Persistir un router nuevo debería confirmar la transacción sin error");
    }

    /**
     * Construye un {@code CoreRouter} sin hijos con un id conocido. Se usa
     * {@link RouterFactory} (la misma vía que el caso de uso) para no acoplar el test a las
     * clases concretas del dominio.
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

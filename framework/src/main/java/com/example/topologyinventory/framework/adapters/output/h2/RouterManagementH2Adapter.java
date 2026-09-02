package com.example.topologyinventory.framework.adapters.output.h2;

import com.example.topologyinventory.application.ports.output.RouterManagementOutputPort;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.framework.adapters.output.h2.data.RouterData;
import com.example.topologyinventory.framework.adapters.output.h2.mappers.RouterH2Mapper;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

/**
 * Output adapter que implementa {@link RouterManagementOutputPort} usando JPA
 * (Hibernate ORM) sobre H2, con la persistencia <em>gestionada por Quarkus</em>.
 *
 * Es la implementación concreta del puerto de salida del router: aquí la
 * tecnología de persistencia queda encapsulada. Traduce a/desde el modelo de
 * dominio mediante {@link RouterH2Mapper}, de modo que el núcleo nunca ve tipos
 * de base de datos.
 *
 * <p><b>Cableado (Opción A, CDI-lite).</b> Este adapter NO es un bean CDI: lo
 * instancia {@link java.util.ServiceLoader} (por {@code module-info provides} en
 * el module path, por {@code META-INF/services} en el classpath de Quarkus). Al
 * no ser bean, no puede recibir el {@code EntityManager} por {@code @Inject} ni
 * usar {@code @Transactional}; por eso:
 * <ul>
 *   <li>obtiene el {@link EntityManager} gestionado seleccionándolo del contenedor
 *       con la SPI estándar {@link CDI#current()} (no la API propietaria de Quarkus,
 *       para no acoplar el hexágono de framework a módulos Quarkus en su descriptor);</li>
 *   <li>demarca la transacción de forma programática con {@link UserTransaction}.</li>
 * </ul>
 * El adapter es <em>stateless</em>: no guarda EntityManager ni es singleton. Cada
 * operación abre y cierra su propia transacción, dentro de la cual el contexto de
 * persistencia está activo (necesario para que el mapper navegue las colecciones
 * perezosas al recuperar). Convertirlo en bean CDI ({@code @ApplicationScoped},
 * {@code @Inject}, {@code @Transactional}) es trabajo de la fase de CDI.
 */
public class RouterManagementH2Adapter implements RouterManagementOutputPort {

    /**
     * Constructor público sin argumentos: es el que {@link java.util.ServiceLoader}
     * exige en el camino de classpath ({@code META-INF/services}), donde el método
     * {@code provider()} no se consulta. Sustituye al singleton de la fase anterior,
     * innecesario ahora que el adapter no guarda estado.
     */
    public RouterManagementH2Adapter() {
    }

    @Override
    public Router retrieveRouter(Id id) {
        var transaction = userTransaction();
        try {
            transaction.begin();
            // La lectura y el mapeo van dentro de la transacción: el mapper navega
            // las colecciones @OneToMany (perezosas), que fuera de una sesión activa
            // lanzarían LazyInitializationException. find (no getReference) carga la
            // entidad de inmediato.
            var routerData = entityManager().find(RouterData.class, id.getId());
            var router = RouterH2Mapper.routerDataToDomain(routerData);
            transaction.commit();
            return router;
        } catch (Exception e) {
            rollbackQuietly(transaction);
            throw new RuntimeException("Fallo al recuperar el router " + id.getId(), e);
        }
    }

    @Override
    public Router persistRouter(Router router) {
        // El mapeo dominio -> data es en memoria; puede hacerse fuera de la transacción.
        var routerData = RouterH2Mapper.routerDomainToData(router);
        var transaction = userTransaction();
        try {
            transaction.begin();
            entityManager().persist(routerData);
            transaction.commit();
            return router;
        } catch (Exception e) {
            rollbackQuietly(transaction);
            throw new RuntimeException(
                    "Fallo al persistir el router " + router.getId().getId(), e);
        }
    }

    /**
     * Selecciona el {@link EntityManager} gestionado por Quarkus del contenedor CDI.
     * Su contexto de persistencia queda ligado a la transacción JTA activa.
     */
    private EntityManager entityManager() {
        return CDI.current().select(EntityManager.class).get();
    }

    /** Selecciona la {@link UserTransaction} del contenedor CDI (proveída por Narayana/JTA). */
    private UserTransaction userTransaction() {
        return CDI.current().select(UserTransaction.class).get();
    }

    /** Rollback de limpieza que nunca enmascara la excepción original. */
    private static void rollbackQuietly(UserTransaction transaction) {
        try {
            transaction.rollback();
        } catch (Exception ignored) {
            // no-op: la excepción de negocio ya se propaga
        }
    }
}

package com.example.topologyinventory.framework.adapters.output.h2;

import com.example.topologyinventory.application.ports.output.RouterManagementOutputPort;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.framework.adapters.output.h2.data.RouterData;
import com.example.topologyinventory.framework.adapters.output.h2.mappers.RouterH2Mapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Output adapter que implementa {@link RouterManagementOutputPort} usando JPA
 * (Hibernate ORM) sobre H2, con la persistencia <em>gestionada por Quarkus</em>.
 *
 * Es la implementación concreta del puerto de salida del router: aquí la
 * tecnología de persistencia queda encapsulada. Traduce a/desde el modelo de
 * dominio mediante {@link RouterH2Mapper}, de modo que el núcleo nunca ve tipos
 * de base de datos.
 *
 * <p><b>Cableado (CDI).</b> Este adapter es un bean {@code @ApplicationScoped}: lo
 * descubre Arc por el índice Jandex y lo inyecta donde se declare
 * {@link RouterManagementOutputPort} (en {@code RouterManagementInputPort}). Al
 * ser bean:
 * <ul>
 *   <li>recibe el {@link EntityManager} gestionado por {@code @Inject}, en lugar de
 *       seleccionarlo del contenedor con {@code CDI.current()};</li>
 *   <li>delega la demarcación de la transacción en {@code @Transactional}, en lugar
 *       de abrir y confirmar una {@code UserTransaction} a mano.</li>
 * </ul>
 * El {@code EntityManager} inyectado es <em>transaction-scoped</em>: su contexto de
 * persistencia se liga a la transacción que abre {@code @Transactional}, de modo que
 * el mapper puede navegar las colecciones {@code @OneToMany} (perezosas) al recuperar
 * sin lanzar {@code LazyInitializationException}.
 *
 * <p><b>Desviación respecto al libro (cap. 11).</b> El libro deja el
 * {@code EntityManager} con {@code @PersistenceContext} y difiere su inyección al
 * cap. 13; aquí se adelanta a {@code @Inject} porque este núcleo ya no arrastra el
 * {@code Persistence.createEntityManagerFactory} que el libro conserva.
 */
@ApplicationScoped
public class RouterManagementH2Adapter implements RouterManagementOutputPort {

    /**
     * {@link EntityManager} gestionado por Quarkus, inyectado por el contenedor. Su
     * contexto de persistencia queda ligado a la transacción activa que demarca
     * {@link Transactional}.
     */
    @Inject
    EntityManager entityManager;

    @Override
    @Transactional
    public Router retrieveRouter(Id id) {
        // La lectura y el mapeo van dentro de la transacción que abre @Transactional:
        // el mapper navega las colecciones @OneToMany (perezosas), que fuera de una
        // sesión activa lanzarían LazyInitializationException. find (no getReference)
        // carga la entidad de inmediato.
        var routerData = entityManager.find(RouterData.class, id.getId());
        return RouterH2Mapper.routerDataToDomain(routerData);
    }

    @Override
    @Transactional
    public Router persistRouter(Router router) {
        // El mapeo dominio -> data es en memoria; queda dentro de la transacción, que
        // @Transactional confirma al salir del método (rollback si algo lanza).
        var routerData = RouterH2Mapper.routerDomainToData(router);
        entityManager.persist(routerData);
        return router;
    }
}

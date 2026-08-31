package com.example.topologyinventory.framework.adapters.output.h2;

import com.example.topologyinventory.application.ports.output.RouterManagementOutputPort;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.framework.adapters.output.h2.data.RouterData;
import com.example.topologyinventory.framework.adapters.output.h2.mappers.RouterH2Mapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Output adapter que implementa {@link RouterManagementOutputPort} usando JPA
 * (EclipseLink) sobre una base de datos H2 en memoria.
 *
 * Es la implementación concreta del puerto de salida del router: aquí la
 * tecnología de persistencia queda encapsulada. Traduce a/desde el modelo de
 * dominio mediante {@link RouterH2Mapper}, de modo que el núcleo nunca ve tipos
 * de base de datos. El puerto solo declara recuperar y persistir (el router es
 * la raíz del agregado); no hay borrado directo.
 *
 * Se expone como singleton ({@link #getInstance()}) porque en esta fase el
 * cableado de puertos es manual; más adelante lo gestionará la inyección de
 * dependencias.
 */
public class RouterManagementH2Adapter implements RouterManagementOutputPort {

    private static RouterManagementH2Adapter instance;

    private EntityManager em;

    private RouterManagementH2Adapter() {
        setUpH2Database();
    }

    @Override
    public Router retrieveRouter(Id id) {
        var routerData = em.getReference(RouterData.class, id.getId());
        return RouterH2Mapper.routerDataToDomain(routerData);
    }

    @Override
    public Router persistRouter(Router router) {
        var routerData = RouterH2Mapper.routerDomainToData(router);
        // Fuera de un contenedor Jakarta EE, con una unidad de persistencia
        // RESOURCE_LOCAL la transacción es manual: sin begin/commit, em.persist
        // no confirma nada en la base de datos.
        var transaction = em.getTransaction();
        transaction.begin();
        try {
            em.persist(routerData);
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
        return router;
    }

    /**
     * Arranca el {@link EntityManager} contra la unidad de persistencia
     * "inventory" definida en {@code META-INF/persistence.xml}. Fuera de un
     * contenedor Jakarta EE, el {@code EntityManager} se obtiene de forma
     * programática con {@link Persistence#createEntityManagerFactory(String)}.
     */
    private void setUpH2Database() {
        EntityManagerFactory entityManagerFactory =
                Persistence.createEntityManagerFactory("inventory");
        this.em = entityManagerFactory.createEntityManager();
    }

    public static RouterManagementH2Adapter getInstance() {
        if (instance == null) {
            instance = new RouterManagementH2Adapter();
        }
        return instance;
    }

    /**
     * Punto de entrada para {@link java.util.ServiceLoader}. El sistema de módulos
     * instancia el proveedor de un servicio mediante un constructor público sin
     * argumentos <em>o</em> un método estático público {@code provider()}. Como
     * esta clase es un singleton con constructor privado, exponemos {@code provider()}
     * para que ServiceLoader reutilice la instancia de {@link #getInstance()} sin
     * abrir el constructor.
     */
    public static RouterManagementH2Adapter provider() {
        return getInstance();
    }
}
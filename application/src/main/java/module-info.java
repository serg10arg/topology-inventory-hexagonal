/**
 * Hexágono de aplicación: orquesta el dominio a través de casos de uso (puertos de entrada) y
 * define los puertos de salida que el framework implementará.
 *
 * Exporta 'ports.output' (los puertos que los output adapters implementan), 'usecases' y
 * 'ports.input' (contratos de entrada y application services, gestionados como beans CDI).
 *
 * Inyección de dependencias (CDI): los application services se anotan '@ApplicationScoped' y
 * reciben colaboradores con '@Inject'; por eso 'requires jakarta.cdi'. La resolución del puerto
 * de salida la hace Arc por tipo (no ServiceLoader).
 *
 * Reactivo (Fase 8): 'requires io.smallrye.mutiny' porque el puerto de salida del router y las
 * firmas 'retrieveRouter'/'persistRouter' del caso de uso exponen ahora 'Uni'. Es la primera
 * dependencia reactiva de este hexágono: se acota a las dos operaciones que tocan persistencia
 * (decisión D3); el resto del caso de uso sigue con tipos de dominio directos.
 */
module application {
    exports com.example.topologyinventory.application.usecases;
    exports com.example.topologyinventory.application.ports.input;
    exports com.example.topologyinventory.application.ports.output;

    requires domain;
    requires static lombok;
    requires jakarta.cdi;
    requires io.smallrye.mutiny;
}

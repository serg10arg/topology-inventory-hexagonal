/**
 * Hexágono de aplicación: orquesta el dominio a través de casos de uso (puertos de
 * entrada) y define los puertos de salida que el framework implementará.
 *
 * Exporta tres paquetes para que el hexágono de framework pueda consumirlos:
 * - 'ports.output': los puertos de salida que los output adapters implementan
 *   (p. ej. RouterManagementOutputPort ← RouterManagementH2Adapter).
 * - 'usecases' y 'ports.input': los contratos de entrada y sus application services,
 *   ahora gestionados como beans CDI e inyectados por los input adapters.
 *
 * Inyección de dependencias (CDI): los application services se anotan
 * '@ApplicationScoped' y reciben sus colaboradores con '@Inject'; por eso este módulo
 * 'requires jakarta.cdi'. La resolución del puerto de salida la hace Arc por tipo, no
 * ServiceLoader: se retiró la cláusula 'uses' (su contraparte 'provides' salió de
 * framework). javac exige el 'requires' para ver las anotaciones; en runtime, Quarkus
 * descubre los beans por el índice Jandex sobre un classpath plano, así que no hace
 * falta abrir paquetes para los proxies de Arc.
 */
module application {
    exports com.example.topologyinventory.application.usecases;
    exports com.example.topologyinventory.application.ports.input;
    exports com.example.topologyinventory.application.ports.output;

    requires domain;
    requires static lombok;
    requires jakarta.cdi;
}

/**
 * Hexágono de aplicación: orquesta el dominio a través de casos de uso (puertos
 * de entrada) y define los puertos de salida que el framework implementará.
 *
 * Exporta tres paquetes para que el hexágono de framework pueda consumirlos:
 * - 'ports.output': los puertos de salida que los output adapters implementan
 *   (p. ej. RouterManagementOutputPort ← RouterManagementH2Adapter).
 * - 'usecases' y 'ports.input': los contratos de entrada y sus application
 *   services, que los input adapters cablearán en la siguiente fase.
 *
 * Consumo de servicio (inversión de dependencias vía JPMS):
 * - 'uses ...RouterManagementOutputPort': declara que este módulo pedirá una
 *   implementación de su propio puerto de salida mediante ServiceLoader (lo hace
 *   RouterManagementInputPort). La implementación la aporta el módulo framework
 *   con su cláusula 'provides'. Importante: application NO hace 'requires
 *   framework' (sería un ciclo); ServiceLoader resuelve por el grafo de módulos.
 */
module application {
    exports com.example.topologyinventory.application.usecases;
    exports com.example.topologyinventory.application.ports.input;
    exports com.example.topologyinventory.application.ports.output;

    requires domain;
    requires static lombok;

    uses com.example.topologyinventory.application.ports.output.RouterManagementOutputPort;
}
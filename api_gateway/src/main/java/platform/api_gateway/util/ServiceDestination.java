package platform.api_gateway.util;

import java.util.List;

public class ServiceDestination {
    private List<String> services;
    private List<String> destinations;

    public ServiceDestination(List<String> services, List<String> destinations) {
        this.services = services;
        this.destinations = destinations;
    }

    public List<String> getServices() {
        return services;
    }

    public List<String> getDestinations() {
        return destinations;
    }
}

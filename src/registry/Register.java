// src/registry/Register.java
package registry;

import client.ServiceReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Service registry for dynamic service discovery.
 * Caches service references for efficient lookup.
 */
public class Register {
    private Map<String, ServiceReference> serviceRegistry;

    public Register() {
        this.serviceRegistry = new HashMap<>();
    }

    /**
     * Looks up a service by name.
     * @param serviceName the name of the service
     * @return ServiceReference if found, null otherwise
     */
    public synchronized ServiceReference lookup(String serviceName) {
        return serviceRegistry.get(serviceName);
    }

    /**
     * Binds or rebinds a service reference in the registry.
     * @param serviceName the name of the service
     * @param ref the service reference
     */
    public synchronized void rebind(String serviceName, ServiceReference ref) {
        serviceRegistry.put(serviceName, ref);
        System.out.println("[Registry] Service '" + serviceName + "' registered.");
    }

    /**
     * Removes a service from the registry.
     * @param serviceName the name of the service
     */
    public synchronized void unbind(String serviceName) {
        serviceRegistry.remove(serviceName);
        System.out.println("[Registry] Service '" + serviceName + "' unregistered.");
    }

    /**
     * Displays all cached services in the console.
     */
    public synchronized void displayCache() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("REGISTRY CACHE STATUS");
        System.out.println("=".repeat(50));
        
        if (serviceRegistry.isEmpty()) {
            System.out.println("⚠ Registry cache is EMPTY - no services cached.");
        } else {
            System.out.println("Found " + serviceRegistry.size() + " service(s) in cache:");
            System.out.println("-".repeat(50));
            
            for (Map.Entry<String, ServiceReference> entry : serviceRegistry.entrySet()) {
                String serviceName = entry.getKey();
                ServiceReference ref = entry.getValue();
                System.out.println("  • Service: " + serviceName);
                System.out.println("    Location: " + ref.getServiceName());
                System.out.println("    Dispatcher: " + ref.getDispatcher().getClass().getSimpleName());
                System.out.println();
            }
        }
        System.out.println("=".repeat(50) + "\n");
    }

    public synchronized void clearCache() {
        serviceRegistry.clear();
        System.out.println("[Registry] Cache cleared - all services removed.");
    }
}
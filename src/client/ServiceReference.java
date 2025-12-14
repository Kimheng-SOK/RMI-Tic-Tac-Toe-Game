package client;

import server.ServerDispatcher;


 // Represents a reference to a remote service.
 // Acts as a handle for clients to access services through the dispatcher.
public class ServiceReference {
    private String serviceName;
    private ServerDispatcher dispatcher;

    public ServiceReference(String serviceName, ServerDispatcher dispatcher) {
        this.serviceName = serviceName;
        this.dispatcher = dispatcher;
    }

    public String getServiceName() {
        return serviceName;
    }

    public ServerDispatcher getDispatcher() {
        return dispatcher;
    }
}
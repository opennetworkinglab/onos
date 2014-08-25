package net.onrc.onos.of.ctl.registry;

public class RegistryException extends Exception {

    private static final long serialVersionUID = -8276300722010217913L;

    public RegistryException(String message) {
        super(message);
    }

    public RegistryException(String message, Throwable cause) {
        super(message, cause);
    }

}

package id.global.event.messaging.runtime;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class HostnameProvider {
    private static final Logger LOG = LoggerFactory.getLogger(HostnameProvider.class);

    private static final String COMPUTERNAME_ENV = "COMPUTERNAME";
    private static final String HOSTNAME_ENV = "HOSTNAME";
    private static final String OS_NAME_PROPERTY = "os.name";
    private static final String WINDOWS = "windows";
    private static final String UNKNOWN_HOSTNAME = "UNKNOWN_HOST";

    private final String hostname;

    public HostnameProvider() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.warn("Hostname resolution failed, falling back to system environment properties");
            String envHostname = getHostnameFromSystemEnv();
            hostname = Objects.requireNonNullElse(envHostname, UNKNOWN_HOSTNAME);
        }
        this.hostname = hostname;
    }

    /**
     * Returns hostname from java InetAddress API. If that fails, looks up system ENV for "COMPUTERNAME" or "HOSTNAME",
     * depending on the OS. If that fails returns "UNKNOWN_HOST".
     *
     * @return hostname
     */
    public String getHostName() {
        return hostname;
    }

    private String getHostnameFromSystemEnv() {
        if (System.getProperty(OS_NAME_PROPERTY).toLowerCase().contains(WINDOWS)) {
            return System.getenv(COMPUTERNAME_ENV);
        } else {
            return System.getenv(HOSTNAME_ENV);
        }
    }
}

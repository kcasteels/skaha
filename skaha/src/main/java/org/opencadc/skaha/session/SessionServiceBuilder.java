package org.opencadc.skaha.session;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceSpec;
import io.kubernetes.client.util.Yaml;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opencadc.skaha.Job;
import org.opencadc.skaha.SessionType;

/**
 * Programmatically build a Kubernetes service for a session, using the session type and job information to provide a
 * base YAML file. Example usage:
 *
 * <pre>
 *     final Job job = new Job("name", "uid", "my-sessionID", SessionType.CARTA);
 *     final SessionServiceBuilder sessionServiceBuilder = new SessionServiceBuilder(job);
 *
 *     // Can be used to execute in a Kubernetes cluster.
 *     final String outputYAML = sessionServiceBuilder.build();
 * </pre>
 */
public class SessionServiceBuilder {
    private static final String RUN_LABEL_KEY = "run";
    private static final String SELECTOR_KEY = "canfar-net-sessionID";

    private final Job job;

    public SessionServiceBuilder(final Job job) {
        this.job = Objects.requireNonNull(job);
    }

    public String build() throws IOException {
        final SessionType sessionType = this.job.getSessionType();
        final V1Service service = loadService();
        if (service == null) {
            throw new IOException("Service configuration not found for session type: " + sessionType);
        } else {
            final V1ObjectMeta metadata = service.getMetadata();
            if (metadata == null) {
                throw new IOException("Service metadata not found for session type: " + sessionType);
            } else {
                final String serviceName = sessionType.getServiceName(this.job.getSessionID());
                metadata.setName(serviceName);
                final Map<String, String> labels;
                if (metadata.getLabels() == null) {
                    labels = new HashMap<>();
                } else {
                    labels = metadata.getLabels();
                }

                labels.put(SessionServiceBuilder.RUN_LABEL_KEY, serviceName);

                final V1OwnerReference ownerReference;
                final List<V1OwnerReference> ownerReferences = metadata.getOwnerReferences();
                if (ownerReferences == null || ownerReferences.isEmpty()) {
                    ownerReference = new V1OwnerReference();
                } else {
                    ownerReference = ownerReferences.get(0);
                }
                ownerReference.name(this.job.getName());
                ownerReference.uid(this.job.getUID());

                final V1ServiceSpec spec;
                if (service.getSpec() == null) {
                    spec = new V1ServiceSpec();
                } else {
                    spec = service.getSpec();
                }

                final Map<String, String> selector;
                if (spec.getSelector() == null) {
                    selector = new HashMap<>();
                } else {
                    selector = spec.getSelector();
                }

                selector.put(SessionServiceBuilder.SELECTOR_KEY, this.job.getSessionID());
                spec.selector(selector);
            }
        }

        return Yaml.dump(service);
    }

    V1Service loadService() throws IOException {
        return (V1Service)
                Yaml.load(this.job.getSessionType().getServiceConfigPath().toFile());
    }
}

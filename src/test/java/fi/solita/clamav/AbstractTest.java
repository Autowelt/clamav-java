package fi.solita.clamav;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public abstract class AbstractTest {

    @Container
    protected final GenericContainer<?> clamAvContainer = new GenericContainer<>(DockerImageName.parse("clamav/clamav"))
            .withEnv("CLAMAV_NO_FRESHCLAMD", "false")
            .withExposedPorts(3310)
            .withAccessToHost(true);

    @Test
    void top_level_container_should_be_running() {
        assertTrue(clamAvContainer.isRunning());
    }
}

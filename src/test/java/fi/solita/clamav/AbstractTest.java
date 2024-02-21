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
    protected static final GenericContainer<?> clamAvContainer = new GenericContainer<>(
            DockerImageName.parse("clamav/clamav"))
            .withEnv("CLAMAV_NO_FRESHCLAMD", "true")
            .withExposedPorts(3310)
            .withAccessToHost(true)
            .withReuse(true);

    @Test
    void top_level_container_should_be_running() {
        assertTrue(clamAvContainer.isRunning());
    }
}

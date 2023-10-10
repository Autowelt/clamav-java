package fi.solita.clamav;

import org.junit.ClassRule;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class AbstractTest {

    @ClassRule
    @Container
    static GenericContainer<?> clamAvContainer = new GenericContainer<>(DockerImageName.parse("clamav/clamav"))
            .withEnv("CLAMAV_NO_FRESHCLAMD", "false")
            .withExposedPorts(3310)
            .withAccessToHost(true);
}

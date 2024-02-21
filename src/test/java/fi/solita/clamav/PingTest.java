package fi.solita.clamav;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These tests assume clamd is running and responding in the virtual machine.
 */
public class PingTest extends AbstractTest {

    private ClamAVClient clamAVClient;

    @org.junit.jupiter.api.BeforeEach
    public void setupTest() {
        this.clamAVClient = new ClamAVClient(clamAvContainer.getHost(), clamAvContainer.getMappedPort(3310));
    }

    @Test
    public void testPingPong() throws IOException {
        assertTrue(clamAVClient.ping());
    }
}

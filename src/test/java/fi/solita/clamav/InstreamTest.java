package fi.solita.clamav;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These tests assume clamd is running and responding in the virtual machine.
 */
public class InstreamTest extends AbstractTest {

    private ClamAVClient clamAVClient;

    private static final String EICAR =
            "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";

    private byte[] scan(byte[] input) throws UnknownHostException, IOException {
        return this.clamAVClient.scan(input);
    }

    private byte[] scan(InputStream input) throws UnknownHostException, IOException {
        return this.clamAVClient.scan(input);
    }

    @BeforeEach
    public void setupTest() {
        this.clamAVClient = new ClamAVClient(clamAvContainer.getHost(), clamAvContainer.getMappedPort(3310));
    }

    @Test
    public void testRandomBytes() throws UnknownHostException, IOException {
        byte[] r = scan("alsdklaksdla".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        assertTrue(ClamAVClient.isCleanReply(r));
    }

    @Test
    public void testPositive() throws UnknownHostException, IOException {
        // http://www.eicar.org/86-0-Intended-use.html
        byte[] bytes = EICAR.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] r = scan(bytes);
        assertFalse(ClamAVClient.isCleanReply(r));
    }

    @Test
    public void testStreamChunkingWorks() throws UnknownHostException, IOException {
        byte[] multipleChunks = new byte[50000];
        byte[] r = scan(multipleChunks);
        assertTrue(ClamAVClient.isCleanReply(r));
    }

    @Test
    public void testChunkLimit() throws UnknownHostException, IOException {
        byte[] maximumChunk = new byte[2048];
        byte[] r = scan(maximumChunk);
        assertTrue(ClamAVClient.isCleanReply(r));
    }

    @Test
    public void testZeroBytes() throws UnknownHostException, IOException {
        byte[] r = scan(new byte[]{});
        assertTrue(ClamAVClient.isCleanReply(r));
    }

//    @Test
//    public void testSizeLimit() throws UnknownHostException, IOException {
//        Assertions.assertThrows(ClamAVSizeLimitException.class, () -> {
//            scan(new SlowInputStream());
//        });
//    }

    // Only the first 10000 bytes will be scanned, so it will not reach size limit
    @Test
    public void testMaxStreamSize() throws UnknownHostException, IOException {
        this.clamAVClient.setMaxStreamSize(10000);
        ScanResult result = this.clamAVClient.scanWithResult(new SlowInputStream());
        assertEquals(ScanResult.Status.PASSED, result.getStatus());
    }

    //Only the first 10000 bytes will be scanned, so it will not reach size limit
    @Test
    public void testMaxStreamSizePositive() throws UnknownHostException, IOException {

        this.clamAVClient.setMaxStreamSize(10000);
        byte[] bytes = EICAR.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        ScanResult result = this.clamAVClient.scanWithResult(bytes);
        assertEquals(ScanResult.Status.FOUND, result.getStatus());
    }
}

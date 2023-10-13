package fi.solita.clamav;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * These tests assume clamd is running and responding in the virtual machine.
 */
public class InstreamTest {

    private static final String EICAR =
            "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
    private static final String CLAMAV_HOST = "localhost";

    private byte[] scan(byte[] input) throws IOException {
        ClamAVClient cl = new ClamAVClient(CLAMAV_HOST, 3310);
        return cl.scan(input);
    }

    private byte[] scan(InputStream input) throws IOException {
        ClamAVClient cl = new ClamAVClient(CLAMAV_HOST, 3310);
        return cl.scan(input);
    }

    @Test
    public void testRandomBytes() throws IOException {
        byte[] r = scan("alsdklaksdla".getBytes(StandardCharsets.US_ASCII));
        assertTrue(ClamAVClient.isCleanReply(r));
    }

    @Test
    public void testPositive() throws IOException {
        // http://www.eicar.org/86-0-Intended-use.html
        byte[] bytes = EICAR.getBytes(StandardCharsets.US_ASCII);
        byte[] r = scan(bytes);
        assertFalse(ClamAVClient.isCleanReply(r));
    }

    @Test
    public void testStreamChunkingWorks() throws IOException {
        byte[] multipleChunks = new byte[50000];
        byte[] r = scan(multipleChunks);
        assertTrue(ClamAVClient.isCleanReply(r));
    }

    @Test
    public void testChunkLimit() throws IOException {
        byte[] maximumChunk = new byte[2048];
        byte[] r = scan(maximumChunk);
        assertTrue(ClamAVClient.isCleanReply(r));
    }

    @Test
    public void testZeroBytes() throws IOException {
        byte[] r = scan(new byte[]{});
        assertTrue(ClamAVClient.isCleanReply(r));
    }

    @Test
    public void testSizeLimit() throws IOException {
        Assertions.assertThrows(ClamAVSizeLimitException.class, () -> {
            scan(new SlowInputStream());
        });
    }

    // Only the first 10000 bytes will be scanned, so it will not reach size limit
    @Test
    public void testMaxStreamSize() throws IOException {
        ClamAVClient cl = new ClamAVClient(CLAMAV_HOST, 3310);
        cl.setMaxStreamSize(10000);
        ScanResult result = cl.scanWithResult(new SlowInputStream());
        assertEquals(ScanResult.Status.PASSED, result.getStatus());
    }

    //Only the first 10000 bytes will be scanned, so it will not reach size limit
    @Test
    public void testMaxStreamSizePositive() throws IOException {
        ClamAVClient cl = new ClamAVClient(CLAMAV_HOST, 3310);
        cl.setMaxStreamSize(10000);
        byte[] bytes = EICAR.getBytes(StandardCharsets.US_ASCII);
        ScanResult result = cl.scanWithResult(bytes);
        assertEquals(ScanResult.Status.FOUND, result.getStatus());
    }
}

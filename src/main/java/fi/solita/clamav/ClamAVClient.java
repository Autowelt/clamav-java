package fi.solita.clamav;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Simple client for ClamAV's clamd scanner. Provides straightforward instream scanning.
 */
public class ClamAVClient {

    // "do not exceed StreamMaxLength as defined in clamd.conf, otherwise clamd will reply with INSTREAM size limit exceeded and close the connection."
    private static final int CHUNK_SIZE = 2048;
    private static final int DEFAULT_TIMEOUT = 500;
    private static final int PONG_REPLY_LEN = 4;
    private final String hostName;
    private final int port;
    private final int timeout;
    private long maxStreamSize = 0;

    /**
     * @param hostName The hostname of the server running clamav-daemon
     * @param port     The port that clamav-daemon listens to(By default it might not listen to a port. Check your clamav configuration).
     * @param timeout  zero means infinite timeout. Not a good idea, but will be accepted.
     */
    public ClamAVClient(String hostName, int port, int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout value does not make sense.");
        }
        this.hostName = hostName;
        this.port = port;
        this.timeout = timeout;
    }

    public ClamAVClient(String hostName, int port) {
        this(hostName, port, DEFAULT_TIMEOUT);
    }

    /**
     * Interpret the result from a  ClamAV scan, and determine if the result means the data is clean
     *
     * @param reply The reply from the server after scanning
     * @return true if no virus was found according to the clamd reply message
     */
    public static boolean isCleanReply(byte[] reply) {
        String r = new String(reply, StandardCharsets.US_ASCII);
        return (r.contains("OK") && !r.contains("FOUND"));
    }

    // byte conversion based on ASCII character set regardless of the current system locale
    private static byte[] asBytes(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    // reads all available bytes from the stream
    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();

        byte[] buf = new byte[2000];
        int read = 0;
        while ((read = is.read(buf)) != -1) {
            tmp.write(buf, 0, read);
        }
        return tmp.toByteArray();
    }

    /**
     * Run PING command to clamd to test it is responding.
     *
     * @return true if the server responded with proper ping reply.
     */
    public boolean ping() throws IOException {
        try (Socket s = new Socket(hostName, port); OutputStream outs = s.getOutputStream()) {
            s.setSoTimeout(timeout);
            outs.write(asBytes("zPING\0"));
            outs.flush();
            byte[] b = new byte[PONG_REPLY_LEN];
            InputStream inputStream = s.getInputStream();
            int copyIndex = 0;
            int readResult;
            do {
                readResult = inputStream.read(b, copyIndex, Math.max(b.length - copyIndex, 0));
                copyIndex += readResult;
            } while (readResult > 0);
            return Arrays.equals(b, asBytes("PONG"));
        }
    }

    /**
     * Streams the given data to the server in chunks and returns a ScanResult indicating the outcome.
     *
     * @param is data to scan. Not closed by this method!
     * @return server reply as ScanResult
     */
    public ScanResult scanWithResult(InputStream is) {
        try {
            return new ScanResult(scan(is));
        } catch (Exception e) {
            return new ScanResult(e);
        }
    }

    /**
     * Streams the given data to the server in chunks. The whole data is not kept in memory.
     * This method is preferred if you don't want to keep the data in memory, for instance by scanning a file on disk.
     * Since the parameter InputStream is not reset, you can not use the stream afterwards, as it will be left in a EOF-state.
     * If your goal is to scan some data, and then pass that data further, consider using {@link #scan(byte[]) scan(byte[] in)}.
     * <p>
     * Opens a socket and reads the reply. Parameter input stream is NOT closed.
     *
     * @param is data to scan. Not closed by this method!
     * @return server reply
     */
    public byte[] scan(InputStream is) throws IOException {
        try (Socket s = new Socket(hostName, port); OutputStream outs = new BufferedOutputStream(s.getOutputStream())) {
            s.setSoTimeout(timeout);

            // handshake
            outs.write(asBytes("zINSTREAM\0"));
            outs.flush();
            byte[] chunk = new byte[CHUNK_SIZE];
            long streamedSize = 0;

            try (InputStream clamIs = s.getInputStream()) {
                // send data
                int read = is.read(chunk);
                while (read >= 0) {
                    // If maxStreamSize is restricted and buffered data would exceed size, stop streaming.
                    streamedSize += read;
                    if (maxStreamSize != 0 && streamedSize > maxStreamSize) {
                        break;
                    }

                    // The format of the chunk is: '<length><data>' where <length> is the size of the following data in bytes expressed as a 4 byte unsigned
                    // integer in network byte order and <data> is the actual chunk. Streaming is terminated by sending a zero-length chunk.
                    byte[] chunkSize = ByteBuffer.allocate(4).putInt(read).array();

                    outs.write(chunkSize);
                    outs.write(chunk, 0, read);
                    if (clamIs.available() > 0) {
                        // reply from server before scan command has been terminated.
                        byte[] reply = assertSizeLimit(readAll(clamIs));
                        throw new IOException("Scan aborted. Reply from server: " + new String(reply, StandardCharsets.US_ASCII));
                    }
                    read = is.read(chunk);
                }

                // terminate scan
                outs.write(new byte[]{0, 0, 0, 0});
                outs.flush();
                // read reply
                return assertSizeLimit(readAll(clamIs));
            }
        }
    }

    /**
     * Scans bytes for virus by passing the bytes to clamav
     *
     * @param in data to scan
     * @return server reply as a ScanResult
     **/
    public ScanResult scanWithResult(byte[] in) {
        try {
            return new ScanResult(scan(in));
        } catch (Exception e) {
            return new ScanResult(e);
        }
    }

    /**
     * Scans bytes for virus by passing the bytes to clamav
     *
     * @param in data to scan
     * @return server reply
     **/
    public byte[] scan(byte[] in) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(in);
        return scan(bis);
    }

    /**
     * Perform a SCAN of the given file path.
     *
     * @param path Path of the file to scan
     * @return server reply as a ScanResult
     * @throws IOException
     */
    public ScanResult scanWithResult(Path path) {
        try {
            return new ScanResult(scan(path));
        } catch (Exception e) {
            return new ScanResult(e);
        }
    }

    /**
     * Perform a SCAN of the given file path.
     *
     * @param path Path of the file to scan
     * @return server reply
     * @throws IOException
     */
    public byte[] scan(Path path) throws IOException {
        try (Socket s = new Socket(hostName, port);
             OutputStream outs = new BufferedOutputStream(s.getOutputStream())) {
            s.setSoTimeout(timeout);

            // handshake
            outs.write(asBytes("SCAN " + path.toAbsolutePath() + "\n"));
            outs.flush();

            try (InputStream clamIs = s.getInputStream()) {
                return assertSizeLimit(readAll(clamIs));
            }
        }
    }

    private byte[] assertSizeLimit(byte[] reply) {
        String r = new String(reply, StandardCharsets.US_ASCII);
        if (r.startsWith("INSTREAM size limit exceeded.")) {
            throw new ClamAVSizeLimitException("Clamd size limit exceeded. Full reply from server: " + r);
        }
        return reply;
    }

    /**
     * Set the max stream size. If this is set to 0, then the stream
     * can be of unlimited length. If set to any other positive value, then
     * at most only the first maxStreamSize bytes will be streamed. This
     * parallels the behavior of MaxFileSize.
     * <p>
     * Generally should be the same as your StreamMaxLength.
     *
     * @param maxStreamSize
     */
    public void setMaxStreamSize(long maxStreamSize) {
        this.maxStreamSize = maxStreamSize;
    }
}

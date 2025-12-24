package services;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;

/**
 * Custom SocketFactory that uses a trust-all SSLContext.
 * Used by Jakarta Mail to bypass TLS certificate validation for development.
 * WARNING: Not recommended for production!
 */
public class TrustAllSocketFactory extends SocketFactory {
    
    private static final SSLContext sslContext;
    private static final SocketFactory instance;
    
    static {
        try {
            // Create trust-all TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            
            // Create and initialize SSLContext
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // Create socket factory instance
            instance = sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TrustAllSocketFactory", e);
        }
    }
    
    /**
     * Get singleton instance of the factory
     */
    public static SocketFactory getDefault() {
        return instance;
    }
    
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return sslContext.getSocketFactory().createSocket(host, port);
    }
    
    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return sslContext.getSocketFactory().createSocket(host, port, localHost, localPort);
    }
    
    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return sslContext.getSocketFactory().createSocket(host, port);
    }
    
    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return sslContext.getSocketFactory().createSocket(address, port, localAddress, localPort);
    }
}

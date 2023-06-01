package sd2223.trab1.clients;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class InsecureHostnameVerifier implements HostnameVerifier {
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }
}

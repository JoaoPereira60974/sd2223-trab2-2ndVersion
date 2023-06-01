package sd2223.trab1.servers.soap;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jakarta.xml.ws.Endpoint;
import sd2223.trab1.discovery.Discovery;
import sd2223.trab1.servers.java.AbstractServer;
import utils.IP;

import javax.net.ssl.SSLContext;

public class AbstractSoapServer<T> extends AbstractServer {
	private static final String SOAP_CTX = "/soap";

	private final int port;
	final T webservice;
	
	protected AbstractSoapServer( boolean enableSoapDebug, Logger log, String service, int port, T webservice) {
		super(log, service, String.format(SERVER_BASE_URI, IP.hostName(), port, SOAP_CTX));
		this.webservice = webservice;
		this.port = port;
		
		if(enableSoapDebug ) {
			System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
			System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
			System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
			System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
		}
	}
	
	protected void start() {
		String ip = IP.hostName();

		HttpsServer server = null;
		try {
			server = HttpsServer.create(new InetSocketAddress(ip, port), 0);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		server.setExecutor(Executors.newCachedThreadPool());
		try {
			server.setHttpsConfigurator(new HttpsConfigurator(SSLContext.getDefault()));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		var endpoint = Endpoint.create(webservice);
		endpoint.publish(server.createContext("/soap"));


		server.start();

		//Endpoint.publish(serverURI.replace(IP.hostName(), INETADDR_ANY), webservice );


		Discovery.getInstance().announce(service, serverURI);
		Log.info(String.format("%s Soap Server ready @ %s\n", service, serverURI));
	}
}

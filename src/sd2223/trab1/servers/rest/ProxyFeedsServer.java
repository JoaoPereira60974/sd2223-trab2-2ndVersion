package sd2223.trab1.servers.rest;

import org.glassfish.jersey.server.ResourceConfig;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.mastodon.MastodonResource;
import sd2223.trab1.servers.Domain;
import utils.Args;

import java.util.logging.Logger;

//Proxy Feeds REST
public class ProxyFeedsServer extends AbstractRestServer {
    private static final int PORT = 8080;

    private static Logger Log = Logger.getLogger(ProxyFeedsServer.class.getName());

    ProxyFeedsServer(){
        super(Log,Feeds.SERVICENAME, PORT);
    }
    @Override
    void registerResources(ResourceConfig config) {
        config.register( MastodonResource.class );
    }

    public static void main(String[] args) throws Exception {
        Args.use( args );
        Domain.set( args[0], Long.valueOf(args[1]));
        new ProxyFeedsServer().start();
    }


}

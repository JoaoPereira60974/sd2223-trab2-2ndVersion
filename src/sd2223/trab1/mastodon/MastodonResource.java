package sd2223.trab1.mastodon;

import sd2223.trab1.api.rest.FeedsService;
import sd2223.trab1.servers.rest.RestFeedsResource;

public class MastodonResource extends RestFeedsResource<Mastodon> implements FeedsService {
    public MastodonResource() {
        super(new Mastodon());
    }
}

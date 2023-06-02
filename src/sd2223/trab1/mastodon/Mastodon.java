package sd2223.trab1.mastodon;

import static sd2223.trab1.api.java.Result.ErrorCode.*;
import static sd2223.trab1.api.java.Result.error;
import static sd2223.trab1.api.java.Result.ok;

import java.util.List;

import com.google.gson.reflect.TypeToken;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.mastodon.msgs.PostStatusArgs;
import sd2223.trab1.mastodon.msgs.PostStatusResult;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import sd2223.trab1.servers.Domain;
import utils.JSON;

public class Mastodon implements Feeds {
	
	static String MASTODON_NOVA_SERVER_URI = "http://10.170.138.52:3000";
	static String MASTODON_SOCIAL_SERVER_URI = "https://mastodon.social";
	
	static String MASTODON_SERVER_URI = MASTODON_NOVA_SERVER_URI;
	
	private static final String clientKey = "r-8WG_3qoOdb5bQYCZvWq2wouq-9IhwmR0Zv4Ny6kjM";
	private static final String clientSecret = "YzZd-N9fiqBTrCrHIgxEVjfm9inIySZtfm8IurbrrcI";
	private static final String accessTokenStr = "XoKPmP_GvmG3VWwv_49COFC-XY6h4rkW4U30HRm2Y2M";

	static final String STATUSES_PATH= "/api/v1/statuses";
	static final String STATUS_PATH = "/api/v1/statuses/:id";
	static final String TIMELINES_PATH = "/api/v1/timelines/home";
	static final String ACCOUNT_FOLLOWING_PATH = "/api/v1/accounts/%s/following";
	static final String VERIFY_CREDENTIALS_PATH = "/api/v1/accounts/verify_credentials";
	static final String SEARCH_ACCOUNTS_PATH = "/api/v1/accounts/search";
	static final String ACCOUNT_FOLLOW_PATH = "/api/v1/accounts/%s/follow";
	static final String ACCOUNT_UNFOLLOW_PATH = "/api/v1/accounts/%s/unfollow";
	
	private static final int HTTP_OK = 200;

	protected OAuth20Service service;
	protected OAuth2AccessToken accessToken;

	private static Mastodon impl;
	
	protected Mastodon() {
		try {
			service = new ServiceBuilder(clientKey).apiSecret(clientSecret).build(MastodonApi.instance());
			accessToken = new OAuth2AccessToken(accessTokenStr);
		} catch (Exception x) {
			x.printStackTrace();
			System.exit(0);
		}
	}

	synchronized public static Mastodon getInstance() {
		if (impl == null)
			impl = new Mastodon();
		return impl;
	}

	private String getEndpoint(String path, Object ... args ) {
		var fmt = MASTODON_SERVER_URI + path;
		return String.format(fmt, args);
	}
	
	@Override
	public Result<Long> postMessage(String user, String pwd, Message msg) {
		try {
			final OAuthRequest request = new OAuthRequest(Verb.POST, getEndpoint(STATUSES_PATH));

			JSON.toMap(new PostStatusArgs(msg.getText())).forEach((k, v) -> {
				request.addBodyParameter(k, v.toString());
			});

			service.signRequest(accessToken, request);

			Response response = service.execute(request);
			if (response.getCode() == HTTP_OK) {
				var res = JSON.decode(response.getBody(), PostStatusResult.class);
				return ok(res.getId());
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
	}

	@Override
	public Result<List<Message>> getMessages(String user, long time) {
		System.out.println("Entrei no getMessages");
		try {
			final OAuthRequest request = new OAuthRequest(Verb.GET, getEndpoint(TIMELINES_PATH));

			request.addQuerystringParameter("max_id",Long.toString(time));

			service.signRequest(accessToken, request);

			Response response = service.execute(request);

			if (response.getCode() == HTTP_OK) {
				List<PostStatusResult> res = JSON.decode(response.getBody(), new TypeToken<List<PostStatusResult>>() {
				});
				return ok(res.stream().map(result -> result.toCleanMessage(Domain.get())).toList());
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
	}

	
	@Override
	public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
		return error(NOT_IMPLEMENTED);
	}

	@Override
	public Result<Message> getMessage(String user, long mid) {
		System.out.println("Cheguei ao getMessage individual");
		try{

			String url = getEndpoint(STATUS_PATH).replace(":id",Long.toString(mid));
			final OAuthRequest request = new OAuthRequest(Verb.GET, url);

			service.signRequest(accessToken, request);

			Response response = service.execute(request);

			if(response.getCode() == HTTP_OK) {
				var res = JSON.decode(response.getBody(), PostStatusResult.class);
				return ok(res.toCleanMessage(Domain.get()));
			}
		}catch(Exception x){
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
	}

	@Override
	public Result<Void> subUser(String user, String userSub, String pwd) {
		return error(NOT_IMPLEMENTED);
	}

	@Override
	public Result<Void> unsubscribeUser(String user, String userSub, String pwd) {
		return error(NOT_IMPLEMENTED);
	}

	@Override
	public Result<List<String>> listSubs(String user) {
		return error(NOT_IMPLEMENTED);
	}

	@Override
	public Result<Void> deleteUserFeed(String user) {
		return error(NOT_IMPLEMENTED);
	}

}

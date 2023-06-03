package sd2223.trab1.mastodon;

import static sd2223.trab1.api.java.Result.ErrorCode.*;
import static sd2223.trab1.api.java.Result.error;
import static sd2223.trab1.api.java.Result.ok;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.mastodon.msgs.MastodonAccount;
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
	static final String ACCOUNT_FOLLOW_PATH = "/api/v1/accounts/:id/follow";
	static final String ACCOUNT_UNFOLLOW_PATH = "/api/v1/accounts/:id/unfollow";
	static final String ACCOUNT_LOOKUP_PATH = "/api/v1/accounts/lookup";
	
	private static final int HTTP_OK = 200;
	private static final int HTTP_NOT_FOUND = 404;
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
		try {
			final OAuthRequest request = new OAuthRequest(Verb.GET, getEndpoint(TIMELINES_PATH));
			service.signRequest(accessToken, request);

			Response response = service.execute(request);

			if (response.getCode() == HTTP_OK) {
				List<PostStatusResult> res = JSON.decode(response.getBody(), new TypeToken<List<PostStatusResult>>() {
				});
				return ok(filterMessages(res.stream().map(result -> result.toCleanMessage(Domain.get())).toList(), time));
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
	}

	private List<Message> filterMessages(List<Message> toList, long time) {
		List<Message> filteredMessages = new ArrayList<>();
		for(Message m : toList){
			if(m.getCreationTime() > time){
				filteredMessages.add(m);
			}
		}
		return filteredMessages;

	}


	@Override
	public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
		try{
			String url = getEndpoint(STATUS_PATH).replace(":id",Long.toString(mid));
			final OAuthRequest request = new OAuthRequest(Verb.DELETE, url);

			service.signRequest(accessToken, request);

			Response response = service.execute(request);

			if(response.getCode() == HTTP_OK) {
				return ok();
			}else if(response.getCode() == HTTP_NOT_FOUND)
				return error(NOT_FOUND);
		}catch(Exception x){
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
	}


	@Override
	public Result<Message> getMessage(String user, long mid) {
		try{
			String url = getEndpoint(STATUS_PATH).replace(":id",Long.toString(mid));
			final OAuthRequest request = new OAuthRequest(Verb.GET, url);

			service.signRequest(accessToken, request);

			Response response = service.execute(request);

			if(response.getCode() == HTTP_OK) {
				var res = JSON.decode(response.getBody(), PostStatusResult.class);
				return ok(res.toCleanMessage(Domain.get()));
			}
			else if(response.getCode() == HTTP_NOT_FOUND){
				return error(NOT_FOUND);
			}
		}catch(Exception x){
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
	}

	@Override
	public Result<Void> subUser(String user, String userSub, String pwd) {
		try{
			String url = getEndpoint(ACCOUNT_LOOKUP_PATH);
			String [] parts = userSub.split("@");
			String displayName = parts[0];
			final OAuthRequest request = new OAuthRequest(Verb.GET, url);
			request.addQuerystringParameter("acct", displayName);

			service.signRequest(accessToken, request);

			Response response = service.execute(request);
			String userId;
			if(response.getCode() == HTTP_OK) {
				var res = JSON.decode(response.getBody(), MastodonAccount.class);
				userId = res.id();
				return actualSub(userId);
			}
		}catch(Exception x){
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
	}

	public Result<Void> actualSub(String id) {
		try{
			String url = getEndpoint(ACCOUNT_FOLLOW_PATH).replaceAll(":id", id);
			final OAuthRequest request = new OAuthRequest(Verb.POST, url);

			service.signRequest(accessToken, request);
			Response response = service.execute(request);
			if(response.getCode() == HTTP_OK) {
				return ok();
			}
		}catch(Exception x){
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
	}

	@Override
	public Result<Void> unsubscribeUser(String user, String userSub, String pwd) {
		try{
			String url = getEndpoint(ACCOUNT_LOOKUP_PATH);
			String [] parts = userSub.split("@");
			String displayName = parts[0];
			final OAuthRequest request = new OAuthRequest(Verb.GET, url);
			request.addQuerystringParameter("acct", displayName);

			service.signRequest(accessToken, request);

			Response response = service.execute(request);
			String userId;
			if(response.getCode() == HTTP_OK) {
				var res = JSON.decode(response.getBody(), MastodonAccount.class);
				userId = res.id();
				return actualRemove(userId);
			}
		}catch(Exception x){
			x.printStackTrace();
		}

		return error(NOT_IMPLEMENTED);
	}

	public Result<Void> actualRemove(String id) {
		try{
			String url = getEndpoint(ACCOUNT_UNFOLLOW_PATH).replaceAll(":id", id);
			final OAuthRequest request = new OAuthRequest(Verb.POST, url);

			service.signRequest(accessToken, request);
			Response response = service.execute(request);
			if(response.getCode() == HTTP_OK) {
				return ok();
			}
		}catch(Exception x){
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
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

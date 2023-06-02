package sd2223.trab1.mastodon.msgs;

import sd2223.trab1.api.Message;

import java.time.Instant;
import java.util.Date;

public record PostStatusResult(String id, String content, String created_at, MastodonAccount account) {
	
	public long getId() {
		return Long.valueOf(id);
	}
	
	long getCreationTime() {
		return Date.from(Instant.parse(created_at)).getTime();
	}
	
	public String getText() {
		return content;
	}
	
	public Message toMessage(String domain) {
		var m = new Message( getId(), account.username(), domain, getText());
		m.setCreationTime( getCreationTime() );
		return m;
	}

	public Message toCleanMessage(String domain) {
		var m = new Message( getId(), account.username(), domain, clean(getText()));
		m.setCreationTime( getCreationTime() );
		return m;
	}

	private String clean(String text) {
		return text.replaceAll("<p>|</p>", "");
	}
}
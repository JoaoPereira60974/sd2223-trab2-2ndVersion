package sd2223.trab1.mastodon.msgs;

import sd2223.trab1.api.Message;

public record PostStatusResult(String id, String content, String created_at, MastodonAccount account) {
	
	public long getId() {
		return Long.valueOf(id);
	}
	
	long getCreationTime() {
		return 0;
	}
	
	public String getText() {
		return content;
	}
	
	public Message toMessage(String domain) {
		var m = new Message( getId(), account.username(), domain, getText());
		m.setCreationTime( getCreationTime() );
		return m;
	}
}
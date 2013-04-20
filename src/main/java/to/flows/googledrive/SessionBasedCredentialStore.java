package to.flows.googledrive;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.http.HttpSession;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialStore;

public class SessionBasedCredentialStore implements CredentialStore{
	
	HttpSession session;
	
	private static final String SESSION_CREDENTIAL = "sessionCredential";

	public SessionBasedCredentialStore( HttpSession session) {
		this.session = session;
	}

	public boolean load(String userId, Credential credential)
			throws IOException {		
		
		SessionCredential c = (SessionCredential) session.getAttribute(SESSION_CREDENTIAL);
		if( c == null ) {
			return false;
		}
		credential.setAccessToken( c.accessToken );
		credential.setExpirationTimeMilliseconds(c.expirationTimeMilliseconds );
		credential.setRefreshToken(c.refreshToken );
		
		return true;
	}

	public void store(String userId, Credential credential) throws IOException {
		
		SessionCredential c = new SessionCredential();
		c.accessToken = credential.getAccessToken();
		c.expirationTimeMilliseconds = credential.getExpirationTimeMilliseconds();
		c.refreshToken = credential.getRefreshToken();
		
		session.setAttribute(SESSION_CREDENTIAL, c);

	}

	public void delete(String userId, Credential credential) throws IOException {
		
		session.removeAttribute(SESSION_CREDENTIAL);
	
	}

	public class SessionCredential implements Serializable {
		
		public String accessToken;
		public String getAccessToken() {
			return accessToken;
		}
		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}
		public Long getExpirationTimeMilliseconds() {
			return expirationTimeMilliseconds;
		}
		public void setExpirationTimeMilliseconds(Long expirationTimeMilliseconds) {
			this.expirationTimeMilliseconds = expirationTimeMilliseconds;
		}
		public String getRefreshToken() {
			return refreshToken;
		}
		public void setRefreshToken(String refreshToken) {
			this.refreshToken = refreshToken;
		}
		public Long expirationTimeMilliseconds;
		public String refreshToken;
	}
}

package to.flows.googledrive;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import to.flows.googledrive.CredentialMediator.InvalidClientSecretsException;
import to.flows.googledrive.CredentialMediator.NoRefreshTokenException;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

public class GoogleOAuth2AuthorizerFilter implements Filter {

	static final Logger LOGGER = Logger.getLogger(GoogleOAuth2AuthorizerFilter.class.getName());
	
	private static final String USER = "user";

	public static final String EXPIRES = "expires";

	public static final String ACCESS_TOKEN = "accessToken";

	private static String clientSecretsFilePath = "client_secrets.json";

	/**
	 * Scopes for which to request access from the user.
	 */
	public static final List<String> SCOPES = Arrays.asList(
			// Required to access and manipulate files.
			"https://www.googleapis.com/auth/drive.file",
			// Required to identify the user in our data store.
			"https://www.googleapis.com/auth/userinfo.email",
			"https://www.googleapis.com/auth/userinfo.profile",
			"https://www.googleapis.com/auth/drive.install",
			"https://www.googleapis.com/auth/drive.appdata");

	/**
	 * JsonFactory to use in parsing JSON.
	 */
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();

	/**
	 * HttpTransport to use for external requests.
	 */
	private static final HttpTransport TRANSPORT = new NetHttpTransport();

	/**
	 * Key of session variable to store user IDs.
	 */
	private static final String USER_ID_KEY = "userId";

	private String googleDriveOpenRedirectPath = "/#/open/";

	private String googleDriveCreateRedirectPath = "/#/create/";
	
	public void init(FilterConfig filterConfig) throws ServletException {
		
		if( filterConfig.getInitParameter("clientSecretsJsonPath") != null ) {
			clientSecretsFilePath = filterConfig.getInitParameter("clientSecretsJsonPath");
		}
		if( filterConfig.getInitParameter("googleDriveOpenRedirectPath") != null ) {
			googleDriveOpenRedirectPath = filterConfig.getInitParameter("googleDriveOpenRedirectPath");
		}

		if( filterConfig.getInitParameter("googleDriveCreateRedirectPath") != null ) {
			googleDriveCreateRedirectPath = filterConfig.getInitParameter("googleDriveCreateRedirectPath");
		}

	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		try {
			LOGGER.info("OAuthCheck for " + ((HttpServletRequest)request).getRequestURI());
			
			CredentialMediator credentialMediator = new CredentialMediator((HttpServletRequest) request,
					getClientSecretsStream(), SCOPES);
			
			Credential credential = credentialMediator.getActiveCredential();
			LOGGER.info("Got Active Credential:" + credential );
			
			HttpSession session = ((HttpServletRequest)request).getSession();
			if( session.getAttribute(USER) == null ) {
				Userinfo info = getUserInfo(credential);
				
				session.setAttribute(USER, info.getEmail());
				session.setAttribute(ACCESS_TOKEN, credential.getAccessToken());
				session.setAttribute(EXPIRES, (Long) credential.getExpirationTimeMilliseconds() );
			}
			checkState( request, response);
			chain.doFilter(request, response);
			
		} catch (InvalidClientSecretsException e) {
			((HttpServletResponse)response).sendError(500);
			e.printStackTrace();
		} catch (NoRefreshTokenException e) {
			LOGGER.info("NoRefreshTokenException - redirecting to " + e.getAuthorizationUrl() );
			((HttpServletResponse)response).sendRedirect(e.getAuthorizationUrl());			
		} catch ( IOException ioe ) {
			ioe.printStackTrace();
			((HttpServletResponse)response).sendError(500);
			((HttpServletResponse)response).getOutputStream().println("Failed to check credentials due to " + ioe);
		}
	}

	private void checkState(ServletRequest request, ServletResponse response) throws IOException {
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		
		// If redirect already done - ignore state.
		if( httpRequest.getPathInfo().startsWith(googleDriveCreateRedirectPath) || httpRequest.getPathInfo().startsWith(googleDriveOpenRedirectPath)) {
			return;
		}
		if( request.getParameter("state") != null ) {
			State state = new State(request.getParameter("state"));
			
			if (state.ids != null && state.ids.size() > 0) {
				LOGGER.info("Redirecting for Opening file from Google Drive: " + googleDriveOpenRedirectPath );
				httpResponse.sendRedirect(googleDriveOpenRedirectPath + state.ids.toArray()[0]);
				return;
			} else if (state.parentId != null) {
				LOGGER.info("Redirecting for Creating file from Google Drive: " + googleDriveCreateRedirectPath );
				httpResponse.sendRedirect(googleDriveCreateRedirectPath + state.parentId);
				return;
			}
		}
	}

	public void destroy() {

	}

	static protected InputStream getClientSecretsStream() {
		return Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(clientSecretsFilePath);
	}
	
	private Userinfo getUserInfo(Credential credential) throws IOException {
		Userinfo userInfo = null;

		// Create a user info service, and make a request to get the user's
		// info.
		Oauth2 userInfoService = new Oauth2.Builder(TRANSPORT, JSON_FACTORY,
				credential).build();
		
		userInfo = userInfoService.userinfo().get().execute();
		LOGGER.info("Got userinfo from Credential:" + userInfo );
		return userInfo;
	}


	static public Drive getGoogleDriveService(HttpSession session) throws InvalidClientSecretsException {
		return getGoogleDriveService( (String) session.getAttribute(ACCESS_TOKEN) , (Long) session.getAttribute(EXPIRES) );
	}

	static public Drive getGoogleDriveService( String accessToken, long expires ) throws InvalidClientSecretsException {
		GoogleClientSecrets secrets;
		try {
			secrets = GoogleClientSecrets.load(JSON_FACTORY,
					getClientSecretsStream());
		} catch (IOException e) {
			throw new InvalidClientSecretsException(
					"client_secrets.json is missing or invalid.");
		}
		
		Credential credentials = new GoogleCredential.Builder().setClientSecrets(secrets)
				.setTransport(TRANSPORT).setJsonFactory(JSON_FACTORY).build()
				.setAccessToken( accessToken )
				.setExpirationTimeMilliseconds( expires );
		
		Drive drive = new Drive.Builder(TRANSPORT, JSON_FACTORY, credentials).build();
		return drive;
	}

	
}

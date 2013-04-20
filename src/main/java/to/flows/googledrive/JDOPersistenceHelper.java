package to.flows.googledrive;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

import com.google.api.client.auth.oauth2.CredentialStore;
import com.google.api.client.extensions.jdo.auth.oauth2.JdoCredentialStore;

public class JDOPersistenceHelper {

	static PersistenceManagerFactory pmf;
	
	static public CredentialStore createJDOCredentialStore( ) {
        if( pmf != null ) {
        	return new JdoCredentialStore(pmf);
        }
		
        pmf = JDOHelper.getPersistenceManagerFactory("jdo.properties");        
        
        return new JdoCredentialStore(pmf);
	}
	
}

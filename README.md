googleoauth2servletfilter
=========================

A ServletFilter allowing your application to get authorization to use Google Drive SDK using OAuth2. 

To use it: 

* Put a jdo.properties file containing your JDO settings in the WEB-INF/classes directory (this is used to persist the refreshToken so that the user doesn't need to grant permissions every time he/she accesses your application). 

javax.jdo.PersistenceManagerFactoryClass=org.datanucleus.api.jdo.JDOPersistenceManagerFactory
datanucleus.ConnectionDriverName=com.mysql.jdbc.Driver
datanucleus.ConnectionURL=<YourURL>
datanucleus.ConnectionUserName=<YourUsername>
datanucleus.ConnectionPassword=<YourPassword>
datanucleus.autoCreateSchema=true

* Configure the servlet filter as per usual in the web.xml. Filter config options include:
	
	googleDriveOpenRedirectPath - Which URL to redirect to when Google Drive asks your application to open a stored file.
	googleDriveCreateRedirectPath - Which URL to redirect to when Google Drive asks your application to create a new file.
	
* Get your client_secrets.json file from the Google API console and store it in WEB-INF/classes.

Done!
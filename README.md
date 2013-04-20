googleoauth2servletfilter
=========================

A ServletFilter allowing your application to get authorization to use Google Drive SDK using OAuth2. It also handles redirects when Google Drive asks the application to open / create new files via the Google Drive UI. 

It's based on the Google API examples, but the classes are tweaked slightly to work as a ServletFilter. It also persists credentials in a RDBMS rather than the AppEngine data store.

To use it: 

* Add the Maven-dependency to your application.

```
	<dependency>
		<groupId>to.flows</groupId>
		<artifactId>GoogleOAuth2ServletFilter</artifactId>
		<version>0.0.1-SNAPSHOT</version>
	</dependency>		
```

* Put a jdo.properties file containing your JDO settings in the WEB-INF/classes directory (this is used to persist the refreshToken so that the user doesn't need to grant permissions every time he/she accesses your application). 

```		
javax.jdo.PersistenceManagerFactoryClass=org.datanucleus.api.jdo.JDOPersistenceManagerFactory
datanucleus.ConnectionDriverName=com.mysql.jdbc.Driver
datanucleus.ConnectionURL=YourURL
datanucleus.ConnectionUserName=YourUsername
datanucleus.ConnectionPassword=YourPassword
datanucleus.autoCreateSchema=true
```
* Configure the servlet filter as per usual in the web.xml. Filter config options include:

```
	<filter>
		<filter-name>OAuth2Filter</filter-name>
		<filter-class>to.flows.googledrive.GoogleOAuth2AuthorizerFilter</filter-class>
		<init-param>
			<param-name>googleDriveOpenRedirectPath</param-name>
			<param-value>/#!graph/</param-value>	
			<!-- Which URL to redirect to when Google Drive asks your application to open a stored file.  -->
		</init-param>
		<init-param>
			<param-name>googleDriveCreateRedirectPath</param-name>
			<param-value>/#!graph/</param-value>
			<!-- Which URL to redirect to when Google Drive asks your application to create a new file.-->
		</init-param>		
	</filter>


```
	
* Get your client_secrets.json file from the Google API console and store it in WEB-INF/classes.

* The first time your user is authenticated by Google he/she will also have to grant permissions for your application to access Google Drive on the users behalf. 

* To access the Google Drive API from your application, use the static helper method found in the GoogleOAuth2ServletFilter class:

```
	static public Drive getGoogleDriveService(HttpSession session) throws InvalidClientSecretsException
```



Done!
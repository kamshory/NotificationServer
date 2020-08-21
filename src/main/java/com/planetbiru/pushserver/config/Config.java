package com.planetbiru.pushserver.config;

import java.util.Properties;

/**
 * Configuration class
 * @author Kamshor, MT
 *
 */
public class Config {
	private static String applicationName = "Planet Notif";
	/**
	 * Create encrypted database configuration
	 */
	private static boolean createConfiguration = false;
	/**
	 * Run service in development mode
	 */
	private static boolean developmentMode = false;
	/**
	 * Run service in debug mode
	 */
	private static boolean debugMode = true;
	/**
	 * Print stack trace
	 */
	private static boolean printStackTrace = false;
	/**
	 * Indicate that configuration is loaded successfully
	 */
	private static boolean readConfigSuccess = false;
	/**
	 * Document root for notification pusher and remover
	 */
	private static String apiDocumentRoot = "notif";
	/**
	 * Pusher
	 */
	private static String pusherContextPusher = "pusher";
	/**
	 * Remover
	 */
	private static String pusherContextRemover = "remover";
	/**
	 * Create group
	 */
	private static String pusherContextCreateGroup = "create-group";
	/**
	 * Register device
	 */
	private static String pusherContextRegisterDevice = "register-device";
	/**
	 * Unregister device
	 */
	private static String pusherContextUnregisterDevice = "unregister-device";

	/**
	 * API version
	 */
	private static String version = "1.0.0";
	private static boolean httpProxyEnabled = false;
	private static String httpAddressForwarder = "x-remote-address-forwarder";
	/**
	 * Properties
	 */
	static Properties properties = new Properties();
	/**
	 * HTTP server port
	 */
	private static int pusherPort = 94;
	/**
	 * HTTPS server port
	 */
	private static int pusherPortSSL = 95;
	/**
	 * Client port
	 */
	private static int notificationPort = 92;
	/**
	 * Client port
	 */
	private static int notificationPortSSL = 93;
	/**
	 * Inspection interval
	 */
	private static long inspectionInterval = 1800000;
	/**
	 * Wait for answer. Server will wait until a connection reply with valid answer
	 */
	private static long waitForAnswer = 30000;
	/**
	 * Filter pusher source
	 */
	private static boolean filterSource = true;
	/**
	 * Require group approval
	 */
	private static boolean groupCreationApproval = true;
	/**
	 * Maximum number of notification on once load
	 */
	private static long limitNotification = 50;
	/**
	 * Maximum number of notification deletion on once load
	 */
	private static long limitTrash = 100;
	/**
	 * Wait for reconnect to database
	 */
	private static long waitDatabaseReconnect = 10000;
	/**
	 * Table prefix. By the table prefix, user can integrate push notification database to their own system without any conflict
	 */
	private static String tablePrefix = "push_";
	/**
	 * Clean up time
	 */
	private static String cleanUpTime = "03:00";
	/**
	 * Keystore file of HTTPS server
	 */
	private static String keystoreFile = "/var/www/keystore.jks";
	/**
	 * Keystore password of HTTPS server
	 */
	private static String keystorePassword = "planet123";
	/**
	 * Encrypted keystore file of HTTPS server
	 */
	private static String keystorePasswordEncrypted = "planet123";	
	/**
	 * Flag that HTTPS server is active or not
	 */
	private static boolean pusherSSLEnabled = false;
	/**
	 * Garbage collection interval
	 */
	private static long gcInterval = 59980;
	/**
	 * Period to delete sent notification (in day). All sent notifications will be deleted after this period
	 */
	private static long deleteNotifSent = 2;
	/**
	 * Period to delete unsent notification (in day). All unsent notifications will be deleted after this period
	 */
	private static long deleteNotifNotSent = 10;
	/**
	 * Key to encrypt and decrypt database configuration.<br>
	 * On the production server, this key is stored in the database and can only be accessed when the service is started. Once the service is running, the database that stores the key must be turned off.
	 */
	private static String encryptionPassword = "1234567890";
	/**
	 * Flag secure content. If set as true, notification will be encrypted using combination of key sent from server and client hash password
	 */
	private static boolean contentSecure = false;
	/**
	 * Redirect user when access document root
	 */
	private static String redirectHome = "https://www.planetbiru.com";
	/**
	 * Mail host
	 */
	private static String mailHost = "localhost";
	/**
	 * Mail port
	 */
	private static int mailPort;
	/**
	 * Mail sender of pusher address approval
	 */
	private static String mailSender = "kamshory@gmail.com";
	/**
	 * Mail template of pusher address approval
	 */
	private static String mailTemplate = "";
	/**
	 * Mail subject of pusher address approval
	 */
	private static String mailSubject = "Pusher Address Confirmation";
	/**
	 * Default URL template of pusher address approval when reading template is failed
	 */
	private static String approvalURLTemplate = "http://push.example.com/approve-address/?auth={auth}";
	/**
	 * Use SMTP authentication
	 */
	private static boolean mailUseAuth = false;
	/**
	 * SMPT username
	 */
	private static String mailUsername = "";
	/**
	 * SMTP password
	 */
	private static String mailPassword = "";
	/**
	 * SMTP encrypted password
	 */
	private static String mailPasswordEncrypted = "";
	private static long waitFreeUpPort = 200;
	private static boolean notificationSSLEnabled = false;
	private static String databaseType = "mariadb";

	/**
	 * Constructor
	 */
	private Config()
	{
		
	}
	
	public static String getApplicationName() {
		return applicationName;
	}

	public static void setApplicationName(String applicationName) {
		Config.applicationName = applicationName;
	}

	public static boolean isCreateConfiguration() {
		return createConfiguration;
	}

	public static void setCreateConfiguration(boolean createConfiguration) {
		Config.createConfiguration = createConfiguration;
	}

	public static boolean isDevelopmentMode() {
		return developmentMode;
	}

	public static void setDevelopmentMode(boolean developmentMode) {
		Config.developmentMode = developmentMode;
	}

	public static boolean isDebugMode() {
		return debugMode;
	}

	public static void setDebugMode(boolean debugMode) {
		Config.debugMode = debugMode;
	}

	public static boolean isPrintStackTrace() {
		return printStackTrace;
	}

	public static void setPrintStackTrace(boolean printStackTrace) {
		Config.printStackTrace = printStackTrace;
	}

	public static boolean isReadConfigSuccess() {
		return readConfigSuccess;
	}

	public static void setReadConfigSuccess(boolean readConfigSuccess) {
		Config.readConfigSuccess = readConfigSuccess;
	}

	public static String getApiDocumentRoot() {
		return apiDocumentRoot;
	}

	public static void setApiDocumentRoot(String apiDocumentRoot) {
		Config.apiDocumentRoot = apiDocumentRoot;
	}

	public static String getPusherContextPusher() {
		return pusherContextPusher;
	}

	public static void setPusherContextPusher(String pusherContextPusher) {
		Config.pusherContextPusher = pusherContextPusher;
	}

	public static String getPusherContextRemover() {
		return pusherContextRemover;
	}

	public static void setPusherContextRemover(String pusherContextRemover) {
		Config.pusherContextRemover = pusherContextRemover;
	}

	public static String getPusherContextCreateGroup() {
		return pusherContextCreateGroup;
	}

	public static void setPusherContextCreateGroup(String pusherContextCreateGroup) {
		Config.pusherContextCreateGroup = pusherContextCreateGroup;
	}

	public static String getPusherContextRegisterDevice() {
		return pusherContextRegisterDevice;
	}

	public static void setPusherContextRegisterDevice(String pusherContextRegisterDevice) {
		Config.pusherContextRegisterDevice = pusherContextRegisterDevice;
	}

	public static String getPusherContextUnregisterDevice() {
		return pusherContextUnregisterDevice;
	}

	public static void setPusherContextUnregisterDevice(String pusherContextUnregisterDevice) {
		Config.pusherContextUnregisterDevice = pusherContextUnregisterDevice;
	}

	public static Properties getProperties() {
		return properties;
	}

	public static void setProperties(Properties properties) {
		Config.properties = properties;
	}

	public static int getPusherPort() {
		return pusherPort;
	}

	public static void setPusherPort(int pusherPort) {
		Config.pusherPort = pusherPort;
	}

	public static int getPusherPortSSL() {
		return pusherPortSSL;
	}

	public static void setPusherPortSSL(int pusherPortSSL) {
		Config.pusherPortSSL = pusherPortSSL;
	}

	public static int getNotificationPort() {
		return notificationPort;
	}

	public static void setNotificationPort(int notificationPort) {
		Config.notificationPort = notificationPort;
	}

	public static int getNotificationPortSSL() {
		return notificationPortSSL;
	}

	public static void setNotificationPortSSL(int notificationPortSSL) {
		Config.notificationPortSSL = notificationPortSSL;
	}

	public static long getInspectionInterval() {
		return inspectionInterval;
	}

	public static void setInspectionInterval(long inspectionInterval) {
		Config.inspectionInterval = inspectionInterval;
	}

	public static long getWaitForAnswer() {
		return waitForAnswer;
	}

	public static void setWaitForAnswer(long waitForAnswer) {
		Config.waitForAnswer = waitForAnswer;
	}

	public static boolean isFilterSource() {
		return filterSource;
	}

	public static void setFilterSource(boolean filterSource) {
		Config.filterSource = filterSource;
	}

	public static boolean isGroupCreationApproval() {
		return groupCreationApproval;
	}

	public static void setGroupCreationApproval(boolean groupCreationApproval) {
		Config.groupCreationApproval = groupCreationApproval;
	}

	public static long getLimitNotification() {
		return limitNotification;
	}

	public static void setLimitNotification(long limitNotification) {
		Config.limitNotification = limitNotification;
	}

	public static long getLimitTrash() {
		return limitTrash;
	}

	public static void setLimitTrash(long limitTrash) {
		Config.limitTrash = limitTrash;
	}

	public static long getWaitDatabaseReconnect() {
		return waitDatabaseReconnect;
	}

	public static void setWaitDatabaseReconnect(long waitDatabaseReconnect) {
		Config.waitDatabaseReconnect = waitDatabaseReconnect;
	}

	public static String getTablePrefix() {
		return tablePrefix;
	}

	public static void setTablePrefix(String tablePrefix) {
		Config.tablePrefix = tablePrefix;
	}

	public static String getCleanUpTime() {
		return cleanUpTime;
	}

	public static void setCleanUpTime(String cleanUpTime) {
		Config.cleanUpTime = cleanUpTime;
	}

	public static String getKeystoreFile() {
		return keystoreFile;
	}

	public static void setKeystoreFile(String keystoreFile) {
		Config.keystoreFile = keystoreFile;
	}

	public static String getKeystorePassword() {
		return keystorePassword;
	}

	public static void setKeystorePassword(String keystorePassword) {
		Config.keystorePassword = keystorePassword;
	}

	public static String getKeystorePasswordEncrypted() {
		return keystorePasswordEncrypted;
	}

	public static void setKeystorePasswordEncrypted(String keystorePasswordEncrypted) {
		Config.keystorePasswordEncrypted = keystorePasswordEncrypted;
	}

	public static boolean isPusherSSLEnabled() {
		return pusherSSLEnabled;
	}

	public static void setPusherSSLEnabled(boolean pusherSSLEnabled) {
		Config.pusherSSLEnabled = pusherSSLEnabled;
	}

	public static long getGcInterval() {
		return gcInterval;
	}

	public static void setGcInterval(long gcInterval) {
		Config.gcInterval = gcInterval;
	}

	public static long getDeleteNotifSent() {
		return deleteNotifSent;
	}

	public static void setDeleteNotifSent(long deleteNotifSent) {
		Config.deleteNotifSent = deleteNotifSent;
	}

	public static long getDeleteNotifNotSent() {
		return deleteNotifNotSent;
	}

	public static void setDeleteNotifNotSent(long deleteNotifNotSent) {
		Config.deleteNotifNotSent = deleteNotifNotSent;
	}

	public static String getEncryptionPassword() {
		return encryptionPassword;
	}

	public static void setEncryptionPassword(String encryptionPassword) {
		Config.encryptionPassword = encryptionPassword;
	}

	public static boolean isContentSecure() {
		return contentSecure;
	}

	public static void setContentSecure(boolean contentSecure) {
		Config.contentSecure = contentSecure;
	}

	public static String getRedirectHome() {
		return redirectHome;
	}

	public static void setRedirectHome(String redirectHome) {
		Config.redirectHome = redirectHome;
	}

	public static String getMailHost() {
		return mailHost;
	}

	public static void setMailHost(String mailHost) {
		Config.mailHost = mailHost;
	}

	public static int getMailPort() {
		return mailPort;
	}

	public static void setMailPort(int mailPort) {
		Config.mailPort = mailPort;
	}

	public static String getMailSender() {
		return mailSender;
	}

	public static void setMailSender(String mailSender) {
		Config.mailSender = mailSender;
	}

	public static String getMailTemplate() {
		return mailTemplate;
	}

	public static void setMailTemplate(String mailTemplate) {
		Config.mailTemplate = mailTemplate;
	}

	public static String getMailSubject() {
		return mailSubject;
	}

	public static void setMailSubject(String mailSubject) {
		Config.mailSubject = mailSubject;
	}

	public static String getApprovalURLTemplate() {
		return approvalURLTemplate;
	}

	public static void setApprovalURLTemplate(String approvalURLTemplate) {
		Config.approvalURLTemplate = approvalURLTemplate;
	}

	public static boolean isMailUseAuth() {
		return mailUseAuth;
	}

	public static void setMailUseAuth(boolean mailUseAuth) {
		Config.mailUseAuth = mailUseAuth;
	}

	public static String getMailUsername() {
		return mailUsername;
	}

	public static void setMailUsername(String mailUsername) {
		Config.mailUsername = mailUsername;
	}

	public static String getMailPassword() {
		return mailPassword;
	}

	public static void setMailPassword(String mailPassword) {
		Config.mailPassword = mailPassword;
	}

	public static String getMailPasswordEncrypted() {
		return mailPasswordEncrypted;
	}

	public static void setMailPasswordEncrypted(String mailPasswordEncrypted) {
		Config.mailPasswordEncrypted = mailPasswordEncrypted;
	}

	public static long getWaitFreeUpPort() {
		return waitFreeUpPort;
	}

	public static void setWaitFreeUpPort(long waitFreeUpPort) {
		Config.waitFreeUpPort = waitFreeUpPort;
	}

	public static boolean isNotificationSSLEnabled() {
		return notificationSSLEnabled;
	}

	public static void setNotificationSSLEnabled(boolean notificationSSLEnabled) {
		Config.notificationSSLEnabled = notificationSSLEnabled;
	}

	public static String getDatabaseType() {
		return databaseType;
	}

	public static void setDatabaseType(String databaseType) {
		Config.databaseType = databaseType;
	}

	public static String getVersion() {
		return version;
	}

	public static boolean isHttpproxyenabled() {
		return isHttpProxyEnabled();
	}

	public static String getHttpaddressforwarder() {
		return getHttpAddressForwarder();
	}

	public static void setVersion(String version) {
		Config.version = version;
	}

	public static boolean isHttpProxyEnabled() {
		return httpProxyEnabled;
	}

	public static void setHttpProxyEnabled(boolean httpProxyEnabled) {
		Config.httpProxyEnabled = httpProxyEnabled;
	}

	public static String getHttpAddressForwarder() {
		return httpAddressForwarder;
	}

	public static void setHttpAddressForwarder(String httpAddressForwarder) {
		Config.httpAddressForwarder = httpAddressForwarder;
	}
	
	
}

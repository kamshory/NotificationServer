package com.planetbiru.pushserver;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.planetbiru.pushserver.config.Config;
import com.planetbiru.pushserver.database.DatabaseTypeException;
import com.planetbiru.pushserver.notification.Notification;
import com.planetbiru.pushserver.utility.QueryParserException;

@RestController
public class HttpRequestHandler 
{
	private Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);
	@SuppressWarnings("unused")
	private final String X_REAL_IP = "x-real-ip";
    @SuppressWarnings("unused")
	private final String HEADER_CLIENT_CODE = "x-client-code";
    @Autowired
    private DataSource dataSource;
    @Autowired
    @Value("${planet.time.zone:Asia/Jakarta}")
	private String timeZone;
    
    @Value("${push.application.name:Planet Notif}")
    private String applicationName;

    /**
	 * Create encrypted database configuration
	 */
    @Value("${push.create.configuration:false}")
	private boolean createConfiguration;
	/**
	 * Run service in development mode
	 */
    @Value("${push.development.mode:false}")
	private boolean developmentMode;
	/**
	 * Run service in debug mode
	 */
    @Value("${push.debug.mode:true}")
	private boolean debugMode;
	/**
	 * Print stack trace
	 */
    @Value("${push.print.stack.trace:true}")
	private boolean printStackTrace;
	/**
	 * Indicate that configuration is loaded successfully
	 */
    @Value("${push.read.configuration.success:false}")
	private boolean readConfigSuccess;
	/**
	 * API version
	 */
    @Value("${push.version:1.0.0}")
	private String version;
    
    @Value("${push.proxy.enabled:false}")
	private boolean httpProxyEnabled;
    
    @Value("${push.http.address.forwarder:x-remote-address-forwarder}")
	private String httpAddressForwarder;
	
	/**
	 * Client port
	 */
    @Value("${push.notification.port.http:96}")
    private int notificationPort;
	/**
	 * Client port
	 */
    @Value("${push.notification.port.https:97}")
	private int notificationPortSSL;
	/**
	 * Inspection interval
	 */
    @Value("${push.inspection.interval:1800000}")
	private long inspectionInterval;
	/**
	 * Wait for answer. Server will wait until a connection reply with valid answer
	 */
    @Value("${push.wait.for.answer:30000}")
	private long waitForAnswer;
	/**
	 * Filter pusher source
	 */
    @Value("${push.filter.source:true}")
	private boolean filterSource;
	/**
	 * Require group approval
	 */
    @Value("${push.group.creation.approval:true}")
	private boolean groupCreationApproval;
	/**
	 * Maximum number of notification on once load
	 */
    @Value("${push.limit.notification:50}")
	private long limitNotification;
	/**
	 * Maximum number of notification deletion on once load
	 */
    @Value("${push.limit.trash:50}")
	private long limitTrash;
	/**
	 * Table prefix. By the table prefix, user can integrate push notification database to their own system without any conflict
	 */
    @Value("${push.table.prefix:push_}")
	private String tablePrefix;
	/**
	 * Clean up time
	 */
    @Value("${push.clean.up.time:03:00}")
	private String cleanUpTime;
	/**
	 * Period to delete sent notification (in day). All sent notifications will be deleted after this period
	 */
    @Value("${push.delete.notif.sent:2}")
	private long deleteNotifSent;
	/**
	 * Period to delete unsent notification (in day). All unsent notifications will be deleted after this period
	 */
    @Value("${push.delete.notif.not.sent:10}")
	private long deleteNotifNotSent;
	/**
	 * Key to encrypt and decrypt database configuration.<br>
	 * On the production server, this key is stored in the database and can only be accessed when the service is started. Once the service is running, the database that stores the key must be turned off.
	 */
    @Value("${push.encryption.password:1234567890}")
	private String encryptionPassword;
	/**
	 * Flag secure content. If set as true, notification will be encrypted using combination of key sent from server and client hash password
	 */
    @Value("${push.content.secure:false}")
	private boolean contentSecure;
	/**
	 * Redirect user when access document root
	 */
    @Value("${push.redirect.home:https://www.planetbiru.com}")
	private String redirectHome;
	/**
	 * Mail host
	 */
    @Value("${push.mail.host:localhost}")
	private String mailHost;
	/**
	 * Mail port
	 */
    @Value("${push.mail.port:25}")
	private int mailPort;
	/**
	 * Mail sender of pusher address approval
	 */
    @Value("${push.mail.sender:kamshory@gmail.com}")
	private String mailSender;
	/**
	 * Mail template of pusher address approval
	 */
    @Value("${push.mail.template}")
	private String mailTemplate;
	/**
	 * Mail subject of pusher address approval
	 */
    @Value("${push.mail.subject:Pusher Address Confirmation}")
	private String mailSubject;
	/**
	 * Default URL template of pusher address approval when reading template is failed
	 */
    @Value("${push.approval.url.template:http://push.example.com/approve-address/?auth={auth}}")
	private String approvalURLTemplate;
	/**
	 * Use SMTP authentication
	 */
    @Value("${push.mail.use.auth:false}")
	private boolean mailUseAuth;
	/**
	 * SMPT username
	 */
    @Value("${push.mail.username}")
	private String mailUsername;
	/**
	 * SMTP password
	 */
    @Value("${push.mail.password}")
	private String mailPassword;
	/**
	 * SMTP encrypted password
	 */
    @Value("${push.mail.password.encrypted}")
	private String mailPasswordEncrypted;
    @Value("${push.wait.free.up.port:200}")
	private long waitFreeUpPort;
    @Value("${push.database.type:mariadb}")
	private String databaseType;

    
    private NotificationServer notificationServer = null;
    
    @PostConstruct
    private void init() 
    {
    	Config.setApplicationName(applicationName);
    	/**
    	 * Create encrypted database configuration
    	 */
    	Config.setCreateConfiguration(createConfiguration);
    	/**
    	 * Run service in development mode
    	 */
    	Config.setDevelopmentMode(developmentMode);
    	/**
    	 * Run service in debug mode
    	 */
    	Config.setDebugMode(debugMode);
    	/**
    	 * Print stack trace
    	 */
    	Config.setPrintStackTrace(printStackTrace);
    	/**
    	 * Indicate that configuration is loaded successfully
    	 */
    	Config.setReadConfigSuccess(readConfigSuccess);

    	/**
    	 * API version
    	 */
    	Config.setVersion(version);
    	Config.setHttpProxyEnabled(httpProxyEnabled);
    	Config.setHttpAddressForwarder(httpAddressForwarder);
    	/**
    	 * Client port
    	 */
    	Config.setNotificationPort(notificationPort);
    	/**
    	 * Client port
    	 */
    	Config.setNotificationPortSSL(notificationPortSSL);
    	/**
    	 * Inspection interval
    	 */
    	Config.setInspectionInterval(inspectionInterval);
    	/**
    	 * Wait for answer. Server will wait until a connection reply with valid answer
    	 */
    	Config.setWaitForAnswer(waitForAnswer);
    	/**
    	 * Filter pusher source
    	 */
    	Config.setFilterSource(filterSource);
    	/**
    	 * Require group approval
    	 */
    	Config.setGroupCreationApproval(groupCreationApproval);
    	/**
    	 * Maximum number of notification on once load
    	 */
    	Config.setLimitNotification(limitNotification);
    	/**
    	 * Maximum number of notification deletion on once load
    	 */
    	Config.setLimitTrash(limitTrash);
    	/**
    	 * Table prefix. By the table prefix, user can integrate push notification database to their own system without any conflict
    	 */
    	Config.setTablePrefix(tablePrefix);
    	/**
    	 * Clean up time
    	 */
    	Config.setCleanUpTime(cleanUpTime);
    	/**
    	 * Period to delete sent notification (in day). All sent notifications will be deleted after this period
    	 */
    	Config.setDeleteNotifSent(deleteNotifSent);
    	/**
    	 * Period to delete unsent notification (in day). All unsent notifications will be deleted after this period
    	 */
    	Config.setDeleteNotifNotSent(deleteNotifNotSent);
    	/**
    	 * Key to encrypt and decrypt database configuration.<br>
    	 * On the production server, this key is stored in the database and can only be accessed when the service is started. Once the service is running, the database that stores the key must be turned off.
    	 */
    	Config.setEncryptionPassword(encryptionPassword);
    	/**
    	 * Flag secure content. If set as true, notification will be encrypted using combination of key sent from server and client hash password
    	 */
    	Config.setContentSecure(contentSecure);
    	/**
    	 * Redirect user when access document root
    	 */
    	Config.setRedirectHome(redirectHome);
    	/**
    	 * Mail host
    	 */
    	Config.setMailHost(mailHost);
    	/**
    	 * Mail port
    	 */
    	Config.setMailPort(mailPort);
    	/**
    	 * Mail sender of pusher address approval
    	 */
    	Config.setMailSender(mailSender);
    	/**
    	 * Mail template of pusher address approval
    	 */
    	Config.setMailTemplate(mailTemplate);
    	/**
    	 * Mail subject of pusher address approval
    	 */
    	Config.setMailSubject(mailSubject);
    	/**
    	 * Default URL template of pusher address approval when reading template is failed
    	 */
    	Config.setApprovalURLTemplate(approvalURLTemplate);
    	/**
    	 * Use SMTP authentication
    	 */
    	Config.setMailUseAuth(mailUseAuth);
    	/**
    	 * SMPT username
    	 */
    	Config.setMailUsername(mailUsername);
    	/**
    	 * SMTP password
    	 */
    	Config.setMailPassword(mailPassword);
    	/**
    	 * SMTP encrypted password
    	 */
    	Config.setMailPasswordEncrypted(mailPasswordEncrypted);
    	Config.setWaitFreeUpPort(waitFreeUpPort);
    	Config.setDatabaseType(databaseType);
    	
    	this.notificationServer = new NotificationServer(this.dataSource, Config.getNotificationPort());
		this.notificationServer.start();

    }
	@GetMapping(path="/")
	public ResponseEntity<String> handleGetHome(@RequestHeader HttpHeaders headers, HttpServletRequest request)
	{
		String responseBody = "";
		HttpHeaders responseHeaders = new HttpHeaders();
		HttpStatus httpStatus = HttpStatus.OK;
		return (new ResponseEntity<>(responseBody, responseHeaders , httpStatus));
	}
	@GetMapping(path="/ping")
	public ResponseEntity<String> handleGetPing(@RequestHeader HttpHeaders headers, HttpServletRequest request)
	{
		String responseBody = "";
		HttpHeaders responseHeaders = new HttpHeaders();
		HttpStatus httpStatus = HttpStatus.OK;
		return (new ResponseEntity<>(responseBody, responseHeaders , httpStatus));
	}
	@PostMapping(path="/notif/1.0.0/pusher")
	public ResponseEntity<String> handlePostPusher(@RequestHeader HttpHeaders headers, @RequestBody String requestBody, HttpServletRequest request)
	{
		return this.handlePost(headers, requestBody, request);
	}
	@PostMapping(path="/notif/1.0.0/remover")
	public ResponseEntity<String> handlePostRemover(@RequestHeader HttpHeaders headers, @RequestBody String requestBody, HttpServletRequest request)
	{
		return this.handlePost(headers, requestBody, request);
	}
	@PostMapping(path="/notif/1.0.0/create-group")
	public ResponseEntity<String> handlePostCreateGouup(@RequestHeader HttpHeaders headers, @RequestBody String requestBody, HttpServletRequest request)
	{
		return this.handlePost(headers, requestBody, request);
	}
	@PostMapping(path="/notif/1.0.0/register-device")
	public ResponseEntity<String> handlePostRegisterDevice(@RequestHeader HttpHeaders headers, @RequestBody String requestBody, HttpServletRequest request)
	{
		return this.handlePost(headers, requestBody, request);
	}
	@PostMapping(path="/notif/1.0.0/unregister-device")
	public ResponseEntity<String> handlePostUegisterDevice(@RequestHeader HttpHeaders headers, @RequestBody String requestBody, HttpServletRequest request)
	{
		return this.handlePost(headers, requestBody, request);
	}
	
	public ResponseEntity<String> handlePost(HttpHeaders headers, String requestBody, HttpServletRequest request)
	{
		String remoteAddress = request.getRemoteAddr();
		String responseBody = "";
		HttpHeaders responseHeaders = new HttpHeaders();
		HttpStatus httpStatus;
		String command = "";
		try 
		{
			Notification notification = new Notification(this.dataSource);			
			if(Config.isHttpproxyenabled())
	    	{
	    		String ra = headers.getFirst(Config.getHttpaddressforwarder());
	    		if(ra == null)
	    		{
	    			ra = "";
	    		}
    			if(ra.length() > 2)
    			{
    				remoteAddress = ra;
    			}
	    	}
			String applicationVersion = headers.getFirst("x-application-version");
			String userAgent = headers.getFirst("user-agent");
			String authorization = headers.getFirst("authorization");
			if(authorization.startsWith("Bearer "))
			{
				authorization = authorization.substring("Bearer ".length());
			}
			JSONObject requestJSON = new JSONObject(requestBody);
			if(notification.authentication(authorization, remoteAddress, applicationName, applicationVersion, userAgent))
			{
				JSONObject responseJSON = new JSONObject(requestBody);
				command = responseJSON.optString("command", "");
				if(command.equals("push-notification"))
				{
					responseJSON = this.insert(notification, requestJSON);
				}
				else if(command.equals("remove-notification"))
				{
					responseJSON = this.delete(notification, requestJSON);
				}
				else if(command.equals("register-device"))
				{
					responseJSON = this.registerDevice(notification, requestJSON);
				}
				else if(command.equals("unregister-device"))
				{
					responseJSON = this.unregisterDevice(notification, requestJSON);
				}
				else if(command.equals("create-group"))
				{
					responseJSON = this.createGroup(notification, requestJSON, remoteAddress, applicationName, applicationVersion, userAgent);
				}
				responseBody = responseJSON.toString();	
				httpStatus = HttpStatus.OK;
			}
			else
			{
				httpStatus = HttpStatus.UNAUTHORIZED;
			}
		}
		catch (NoSuchAlgorithmException e) 
		{
			logger.error(e.getMessage());
			httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		catch (QueryParserException e) 
		{
			logger.error(e.getMessage());
			httpStatus = HttpStatus.BAD_REQUEST;
		}
		catch (JSONException | MessagingException e) 
		{
			logger.error(e.getMessage());
			httpStatus = HttpStatus.BAD_REQUEST;
		}
		catch (SQLException | DatabaseTypeException e) 
		{
			logger.error(e.getMessage());
			httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		catch (ClassCastException | NullPointerException | IllegalArgumentException e) 
		{
			logger.error(e.getMessage());
			httpStatus = HttpStatus.ACCEPTED;
		} 
		
		return (new ResponseEntity<>(responseBody, responseHeaders , httpStatus));
	}
	private JSONObject insert(Notification notification, JSONObject requestJSON) throws SQLException, DatabaseTypeException {
		return notification.insert(requestJSON);
	}

	private JSONObject delete(Notification notification, JSONObject requestJSON) throws SQLException, DatabaseTypeException {
		return notification.delete(requestJSON);
	}

	private JSONObject registerDevice(Notification notification, JSONObject requestJSON) throws SQLException, DatabaseTypeException {
		return notification.registerDevice(requestJSON);
	}

	private JSONObject unregisterDevice(Notification notification, JSONObject requestJSON) throws SQLException, DatabaseTypeException {
		return notification.unregisterDevice(requestJSON);
	}

	private JSONObject createGroup(Notification notification, JSONObject requestJSON, String remoteAddress, String applicationName,
			String applicationVersion, String userAgent) throws NoSuchAlgorithmException, DatabaseTypeException, SQLException, MessagingException {
		return notification.createGroup(requestJSON, remoteAddress, applicationName, applicationVersion, userAgent);
	}
	

}


import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.tooling.ExecuteAnonymousResult;
import com.sforce.soap.tooling.ToolingConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.transport.SoapConnection;



/**
 * Login utility.
 */
public class MetadataLoginUtil {

	public static PartnerConnection sfdc;
	public static MetadataConnection sfdcMetadata;

	//default params
	//public static String endpoint = "https://login.salesforce.com/services/Soap/u/40.0";
	//public static String username = "username";
	//public static String password = "password";




	public static MetadataConnection login(String [] credential)  throws ConnectionException {

		sfdc = connect(credential);
		
		System.out.println("Connecting to Metadata API");
		sfdcMetadata = metadataConnect(sfdc);

		return sfdcMetadata;	

	}

	public static MetadataConnection login(PartnerConnection pc)  throws ConnectionException {


		System.out.println("Connecting to Metadata API");
		sfdcMetadata = metadataConnect(pc);

		return sfdcMetadata;	

	}


	public static PartnerConnection connect(String [] credential) {
		sfdc = null;
		String username = null;
		String password = null;
		String endpoint = null;
		for(int i = 0; i < credential.length - 1; i++){
			endpoint=credential[0];
			username=credential[1];
			password=credential[2];
		}
		try {
			ConnectorConfig cc = new ConnectorConfig();
			cc.setUsername(username);
			cc.setPassword(password);
			cc.setAuthEndpoint(endpoint);			
			sfdc = new PartnerConnection(cc);
			
			System.out.println("Username and Orgid for connection: " + sfdc.getUserInfo().getUserName()+ " "+sfdc.getUserInfo().getOrganizationId());
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to log into Salesforce: " + e.getMessage());
		}

		return sfdc;
	}

	public static MetadataConnection metadataConnect(PartnerConnection pc) throws ConnectionException {
		try {
			ConnectorConfig cc = pc.getConfig();

			LoginResult lr = pc.login(cc.getUsername(), cc.getPassword());
			ConnectorConfig cc2 = new ConnectorConfig();
			cc2.setSessionId( cc.getSessionId());
			cc2.setServiceEndpoint(lr.getMetadataServerUrl());	 
			System.out.println("Connection Accepted");
			return new MetadataConnection(cc2);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to log into Salesforce: " + e.getMessage());
		}		
	}
	
	public static ToolingConnection toolingConnection(String[] sourceCredential) throws ConnectionException{
		
		//set partner connection
		PartnerConnection scriptConnection = MetadataLoginUtil.connect(sourceCredential);

		//start a config for tooling api
		ConnectorConfig toolingConfig = new ConnectorConfig();

		//transfer session id from partner connection
		toolingConfig.setSessionId(scriptConnection.getSessionHeader().getSessionId());
		toolingConfig.setServiceEndpoint(scriptConnection.getConfig().getServiceEndpoint().replace('u', 'T'));
		
		//start a tooling connection
		 ToolingConnection toolingConnection = com.sforce.soap.tooling.Connector.newConnection(toolingConfig);
		
		 return toolingConnection;
	}

}
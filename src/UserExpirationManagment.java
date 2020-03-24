import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

public class UserExpirationManagment {

	//program can currently try to update users to inactive in sbxs
	//need alt method to set passwords to random values and boof emails to ensure that passwords cannot be reset if users are tied to the following:

	//Remove user from account teams
	//Remove user from opportunity teams of closed opportunities
	//Remove user from opportunity teams of open opportunities
	//Remove user from predefined case teams
	//Remove user from ad hoc case teams

	private static final int globalBatchSize = 200;

	public static String dummyPassword = "Asashf213sd!2";

	public static HashMap<String, String> globalMap = new HashMap<String, String>();

	//stores a connection
	public static PartnerConnection sfdcPartner;

	//PROD code is specifically designed to look for users form PROD when they dont exist in the target sandbox
	static String[] sourceCredential = { "https://login.salesforce.com/services/Soap/u/46.0", "username", "password" };
	//SANDBOX
	static String[] targetCredential = { "https://test.salesforce.com/services/Soap/u/46.0", "username", "password" };

	//where we can store and go over multiple credentials once info is fetched
	static HashMap<String,GeneralMethods> targetCredentials = new HashMap<String,GeneralMethods>();

	public static void main(String[] args) throws ConnectionException {
		// TODO Auto-generated method stub

		if(System.getenv("sourceCredential")!=null && !System.getenv("sourceCredential").equals("") ){
			sourceCredential=System.getenv("sourceCredential").split(",");
		} else {

			if(System.getenv("link")!=null && !System.getenv("link").equals("") ){
				sourceCredential[0] =System.getenv("link");
			}

			if(System.getenv("uname")!=null && !System.getenv("uname").equals("") ){
				sourceCredential[1] =System.getenv("uname");
			}

			if(System.getenv("password")!=null && !System.getenv("password").equals("") ){
				sourceCredential[2]=System.getenv("password");
			}


		}

		//sets random password
		dummyPassword = getRandomPassord();

		//set SNDBOX credential from env variables
		if(System.getenv("targetCredentials")!=null && !System.getenv("targetCredentials").equals("") ){
			String [] splitCredentials=System.getenv("targetCredentials").split(";");

			for (String cred: splitCredentials) {
				//test all target Credentials and put in map
				validateConnection(cred);
			}
		}

		//get all recent prod data for users expired
		//need usernames
		GeneralMethods prodConnector = new GeneralMethods (sourceCredential);

		//From audit trail
		//SELECT Field1 FROM SetupAuditTrail where createddate = this_month and action='deactivateduser'

		//Select username from user where isactive=false and lastmodifieddate=this_month and usertype='standard' and lastlogindate>=LAST_N_MONTHS:8
		String expiredSoql = "Select username from user where isactive=false and lastmodifieddate>=Yesterday and usertype='standard'";
		List<SObject> records = prodConnector.queryRecords(expiredSoql);

		ArrayList<String> usernames = getUserList(records);


		//iterate over multiple SBXs to expire users
		//need to handle runtime exceptions so all envs have a chance of removing target users
		for(Entry<String, GeneralMethods> es:targetCredentials.entrySet()) {

			try {
				System.out.println("Will attempt to fix users in: " +es.getKey());
				inActivateUsers(es.getKey(),es.getValue(),usernames);
			}catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}

	}

	private static void inActivateUsers(String key, GeneralMethods value, ArrayList<String> usernames) throws ConnectionException, IOException {
		// TODO Auto-generated method stub
		String sbName = key;
		ArrayList<String> usernamesSB  = convertList(usernames, sbName);
		int listSize = usernamesSB.size();
		String batchQuery = "Select id, email from user where isactive=true and username in ";

		ArrayList<SObject> finalList = new ArrayList<SObject>();

		//batching mechanism
		for (int batch = 0; usernamesSB.size()>batch;){

			List<String> batchList = usernamesSB.subList(batch, batch = batch + 200 >listSize-1 ? listSize : batch + 200 );

			String inList = inQueryList(batchList);
			String finalQuery = batchQuery + inList;

			finalList.addAll(value.queryRecords(finalQuery));

		}

		//Issues with bulk removal will be for these ideas
		//Remove user from account teams
		//Remove user from opportunity teams of closed opportunities
		//Remove user from opportunity teams of open opportunities
		//Remove user from predefined case teams
		//Remove user from ad hoc case teams

		globalMap = setMapIdtoEmail(finalList);

		finalList = setSObjectFieldsInActivate(finalList);



		System.out.println("Attempting to update target list for record size: " + finalList.size());

		ArrayList<SaveResult[]> results = updateRecords(value.sfdcPartner, finalList);

		//print results
		GeneralMethods.saveResultsArray(results,key+".csv");

		ArrayList<String> idsFailed = getRemainder(results);

		if(idsFailed.size()>0) {
			boofRemainder(value,sbName, idsFailed);
		}else {
			System.out.println("There was no Remainder that needed to be boofed");
		}

		System.out.println("All updates completed");


	}
	
	public static ArrayList<SObject> batchingInQuery(GeneralMethods cred, ArrayList<String> listForQuery, String query) throws ConnectionException, IOException {
		// TODO Auto-generated method stub
		int listSize = listForQuery.size();
		String batchQuery = query;

		ArrayList<SObject> finalList = new ArrayList<SObject>();

		//batching mechanism
		for (int batch = 0; listForQuery.size()>batch;){

			List<String> batchList = listForQuery.subList(batch, batch = batch + 200 >listSize-1 ? listSize : batch + 200 );

			String inList = inQueryList(batchList);
			String finalQuery = batchQuery + inList;

			finalList.addAll(cred.queryRecords(finalQuery));

		}
		
		return finalList;

	}

	private static HashMap<String, String> setMapIdtoEmail(ArrayList<SObject> finalList) {
		// TODO Auto-generated method stub
		HashMap<String, String> map = new HashMap<String, String>();
		for(SObject r : finalList) {
			map.put(r.getId().toString(), r.getField("Email").toString());

		}
		return map;
	}

	private static void boofRemainder(GeneralMethods value, String sbName, ArrayList<String> idsFailed) throws ConnectionException, IOException {
		// TODO Auto-generated method stub
		//first boof passwords

		value.resetPasswords(idsFailed, dummyPassword);

		//update records to dummy email
		ArrayList<SObject> listEmailUpdate = setSObjectFieldsBoof(idsFailed);

		p("Attempting to boof emails");

		ArrayList<SaveResult[]> results = updateRecords(value.sfdcPartner, listEmailUpdate);

		//print results
		GeneralMethods.saveResultsArray(results,sbName+"boofEmail"+".csv");

	}

	//generates random password
	public static String getRandomPassord() {

		int n = 15;
		String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
				+ "0123456789"
				+ "abcdefghijklmnopqrstuvxyz"; 

		StringBuffer r = new StringBuffer(n); 

		for (int i = 0; i < n; i++) { 

			int index = (int) (AlphaNumericString.length()*Math.random());

			r.append(AlphaNumericString.charAt(index)); 
		} 

		return r.toString();

	}

	private static ArrayList<String> getRemainder(ArrayList<SaveResult[]> results) {
		// TODO Auto-generated method stub
		ArrayList<String> badIds = new ArrayList<String>();
		for(SaveResult[] res:results) {
			for(SaveResult r:res) {
				if(r.getErrors().length>0){
					badIds.add(r.getId());
				}
			}
		}
		return badIds;
	}

	//create new clean list which sets values to false
	private static ArrayList<SObject> setSObjectFieldsBoof(ArrayList<String> finalList) {
		// TODO Auto-generated method stub
		ArrayList<SObject> records = new ArrayList<SObject>();
		for(String r : finalList) {
			SObject t = new SObject();
			t.setType("User");
			t.setField("Id", r);
			t.setField("Email", globalMap.get(r)+".boofed");

			records.add(t);

		}
		return records;
	}

	//create new clean list which sets values to false
	private static ArrayList<SObject> setSObjectFieldsInActivate(ArrayList<SObject> finalList) {
		// TODO Auto-generated method stub
		ArrayList<SObject> records = new ArrayList<SObject>();
		for(SObject r : finalList) {
			SObject t = new SObject();
			t.setType("User");
			t.setField("Id", r.getField("Id").toString());
			t.setField("isActive", false);

			records.add(t);

		}
		return records;
	}

	//method that fixes a set of comma separated user list to format for an in query
	public static String inQueryList (List<String> listOfUsers){
		//fix usernames for query
		String users="";

		int length = listOfUsers.size();
		int count=0;
		if(length>0) {
			users="(";
			for (String a : listOfUsers) {

				if(length-1>count) {
					users= users+"'"+a+"'"+",";
					count++;
				}else {
					users= users+"'"+a+"')";
				}

			}

			return users;

		}else {
			return "('')";
		}


	}

	//soap call at 200 records a batch
	public static ArrayList<SaveResult[]> updateRecords(PartnerConnection connection, ArrayList<SObject> finalList) throws ConnectionException {
		ArrayList<SaveResult[]> srsAll= new ArrayList<SaveResult[]>();
		SaveResult[] srs=null;
		int listSize = finalList.size();
		for (int batch = 0; finalList.size()>batch;){

			List<SObject> batchList = finalList.subList(batch, batch = batch + globalBatchSize >listSize-1 ? listSize : batch + globalBatchSize );
			srs = connection.update(batchList.toArray(new SObject[0]));
			srsAll.add(srs);

			System.out.printf("Received %d SaveResults back\n", srs.length);
			int srIndex = 0;
			for (SaveResult sr : srs) {
				if (!sr.isSuccess()) {
					SObject mo = finalList.get(srIndex);
					System.out.printf("Error Updating %s \n", mo.getField("Id"));
					sr.setId(mo.getField("Id").toString());
					com.sforce.soap.partner.Error[] errors = sr.getErrors();
					for (com.sforce.soap.partner.Error error : errors) {
						System.out.println(error.getMessage());
					}
				}
				srIndex++;
			}

		}
		return srsAll;
	}


	private static ArrayList<String> convertList(ArrayList<String> usernames, String sbName) {
		// TODO Auto-generated method stub
		ArrayList<String> list = new ArrayList<String> ();
		for(String s: usernames) {
			list.add(s+"."+sbName);
		}
		return list;
	}

	private static ArrayList<String> getUserList(List<SObject> records) {
		// TODO Auto-generated method stub
		ArrayList<String> list = new ArrayList<String>();
		for(SObject r: records) {
			list.add(r.getField("Username").toString());
		}
		return list;
	}

	private static void validateConnection(String cred) throws ConnectionException {

		String [] splitCredential = cred.split(",");
		GeneralMethods pc = new GeneralMethods(splitCredential);
		pc.setPartnerConnection();
		if(pc.checkConnectionOpen(pc.sfdcPartner)) {
			String userName = pc.sfdcPartner.getUserInfo().getUserName();
			String sbxName = substringAfterLast(userName,"\\.");
			targetCredentials.put(sbxName, pc);
		};


	}

	private static String substringAfterLast(String userName, String string) {
		// TODO Auto-generated method stub
		String [] split = userName.split(string);
		int size = split.length;
		if(size>0) {
			for(int i=1; size>=i; i++) {
				if(size==i) {
					return(split[i-1]);
				}
			}

		}else{
			return null;
		}

		return userName;

	}

	//quickprint
	public static void p(String string) {

		System.out.println(string);

	}


}

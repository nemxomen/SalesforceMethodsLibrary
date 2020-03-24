import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.sforce.soap.metadata.DescribeMetadataObject;
import com.sforce.soap.metadata.DescribeMetadataResult;
import com.sforce.soap.metadata.DescribeValueTypeResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.ValueTypeField;
import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.fault.ApiQueryFault;
import com.sforce.soap.partner.fault.InvalidSObjectFault;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.soap.tooling.ToolingConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.bind.XmlObject;


/**
 *  Helpful Salesforce Methods which have 3 different connections to connect to Salesforce Instances
 *  When instantiated this can maintain each credential per instantiation
 * 
 * @author raman kansal
 * 
 * In order for email functions to work you will need to update to an open smtphost
 */


public class GeneralMethods {

	
	public PartnerConnection sfdcPartner;
	public ToolingConnection sfdcTooling;
	public MetadataConnection sfdcMetadata;
	public static PartnerConnection sfdcPartnerStatic;
	public String[] sourceCredential = { "https://test.salesforce.com/services/Soap/u/46.0", "username", "password" };
	public static String smtp_host="mail.internal.server.com";
	public static HashMap<String, String> headerToValidHeaderMap = new HashMap<String, String>();
	public static TimeZone tz = TimeZone.getTimeZone("PST");


	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Dont run me!");
	}

	public GeneralMethods(String [] credential)  {

		//any time this class is instantiated, we need a crendential for running all the non static methods
		//framework is designed to have a new GM for each crendential you want to maintain and houses 3 types of connections using setMethods
		setCrendential(credential);

	}

	/**
	 * Checks to see if a PartnerConnection is open already
	 * 
	 * @param check PartnerConnection
	 * @return Boolean
	 */
	public boolean checkConnectionOpen(PartnerConnection check) {
		try {

			if(check.getSessionHeader()!=null) {
				//System.out.println("Connection exists already");
				return true;
			}
			//System.out.println("Connection does not exist");
			return false;

		}catch(NullPointerException e) {
			//System.out.println("Connection does not exist");
			return false;	
		}


	}

	/**
	 * Checks to see if a ToolingConnection is open already
	 * 
	 * @param check ToolingConnection
	 * @return Boolean
	 */

	public boolean checkConnectionOpen(ToolingConnection check) {

		try {

			if(check.getSessionHeader()!=null) {
				//System.out.println("Connection exists already");
				return true;
			}
			//System.out.println("Connection does not exist");
			return false;

		}catch(NullPointerException e) {
			//System.out.println("Connection does not exist");
			return false;	
		}

	}

	/**
	 * Checks to see if a MetadataConnection is open already
	 * 
	 * @param check MetadataConnection
	 * @return Boolean
	 */

	public boolean checkConnectionOpen(MetadataConnection check) {

		try {

			if(check.getSessionHeader()!=null) {
				//System.out.println("Connection exists already");
				return true;
			}
			//System.out.println("Connection does not exist");
			return false;

		}catch(NullPointerException e) {
			//System.out.println("Connection does not exist");
			return false;	
		}


	}

	/**
	 * Query method to gather all records for any query passed for salesforce, need a partner connection and query for this to work
	 * 
	 * @param connection PartnerConnection
	 * @param soqlQuery SOQL QUERY as a string to be passed
	 * @return List<SObject> 
	 * @throws ConnectionException
	 */

	//query method to gather all records for any query passed for salesforce, need a partner connection and query for this to work
	public static List<SObject> queryRecords(PartnerConnection connection, String soqlQuery) throws ConnectionException {
		List<SObject> records = new  ArrayList<SObject>();
		QueryResult qResult = null;
		while(true){
			try {
				System.out.println("Attempting to Query: " + soqlQuery);
				qResult = connection.query(soqlQuery);
				boolean done = false;
				if (qResult.getSize() > 0) {
					System.out.println("Query retrieved "
							+ qResult.getSize() + " records.");
					while (!done) {

						records.addAll(Arrays.asList(qResult.getRecords()));

						if (qResult.isDone()) {
							done = true;
							return records;
						} else {
							qResult = connection.queryMore(qResult.getQueryLocator());

						}
					} 
				}
				else {
					System.out.println("No records found.");
				}
				System.out.println("\nQuery succesfully executed."); 
				return records;

			}	catch(ConnectionException e){
				if(!e.toString().contains("ApiQueryFault")&&!e.toString().contains("INVALID_TYPE_FOR_OPERATION")){

					ConnectorConfig cc = connection.getConfig();
					LoginResult lr = sfdcPartnerStatic.login(cc.getUsername(), cc.getPassword());

				} else{
					throw new RuntimeException("Issue with Connection or Query: " + e.toString());
				}
			}catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Unable to log into Salesforce: " + e.getMessage());
			}
		}
	}

	/**
	 *  Non static Query method to gather all records for any query passed for salesforce, need a partner connection and query for this to work
	 * 
	 * @param connection PartnerConnection
	 * @param soqlQuery SOQL QUERY as a string to be passed
	 * @return List<SObject> 
	 * @throws ConnectionException
	 */

	//query method to gather all records for any query passed for salesforce, need a partner connection and query for this to work
	public List<SObject> queryRecords(String soqlQuery) throws ConnectionException {
		setPartnerConnection();
		List<SObject> records = new  ArrayList<SObject>();
		QueryResult qResult = null;
		while(true){
			try {
				System.out.println("Attempting to Query: " + soqlQuery);
				qResult = sfdcPartner.query(soqlQuery);
				boolean done = false;
				if (qResult.getSize() > 0) {
					System.out.println("Query retrieved "
							+ qResult.getSize() + " records.");
					while (!done) {

						records.addAll(Arrays.asList(qResult.getRecords()));
						//		            for (int i = 0; i < records.; ++i) {
						//		               SObject pull = (SObject) records[i];		            
						//		              }
						if (qResult.isDone()) {
							done = true;
							return records;
						} else {
							qResult = sfdcPartner.queryMore(qResult.getQueryLocator());
							//  Print record batches getting pulled from salesforce  
							//  System.out.println(qResult.getQueryLocator());
						}
					} 
				}
				else {
					System.out.println("No records found.");
				}
				System.out.println("\nQuery succesfully executed."); 
				return records;

			}	catch(ConnectionException e){
				if(!e.toString().contains("ApiQueryFault")&&!e.toString().contains("INVALID_TYPE_FOR_OPERATION")){

					ConnectorConfig cc = sfdcPartner.getConfig();
					LoginResult lr = sfdcPartner.login(cc.getUsername(), cc.getPassword());

				} else{
					throw new RuntimeException("Issue with Connection or Query: " + e.toString());
				}
			}catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Unable to log into Salesforce: " + e.getMessage());
			}
		}
	}

	/**
	 * Gets all fields mapped as a HashSet given an SObject with correct notation
	 * fields are mapped as .notation for multiple relationsips
	 *  i.e Order.ContractId
	 * @param so SObject
	 * @return HashSet<String> List of fields
	 */

	//get a flattened list of all fields in Sobject with . notation as you see in SOQL
	public static HashSet<String> getAllFields(SObject so) {
		HashSet<String> fieldList = new HashSet<String>();	
		//set iterator
		Iterator<XmlObject> xo = so.getChildren();

		while (xo.hasNext()) {
			//set first XMLobject read
			XmlObject firstChild = xo.next();

			if(!firstChild.hasChildren()) {
				if(hasValue(firstChild)){
					fieldList.add(firstChild.getName().getLocalPart().toString());
					//fields.add(firstChild.getName().getLocalPart().toString());
				}

			}else if(firstChild.hasChildren()) {
				fieldList.addAll((hasMore(firstChild.getName().getLocalPart().toString(),firstChild)));
			}

		}

		return fieldList;

	}

	/**
	 * Child method for getAllFields methods
	 *
	 * @param base String from previous method
	 * @param xo XML Object
	 * @return
	 */
	public static HashSet<String> hasMore(String base, XmlObject xo) {
		//System.out.println("Base: " + base);
		Iterator<XmlObject> xo2 = xo.getChildren();
		HashSet<String> fieldList = new HashSet<String>();

		while (xo2.hasNext()) {

			XmlObject t = xo2.next();

			String newbase=base+"."+t.getName().getLocalPart().toString();

			//System.out.println("newbase: " + newbase);

			if(!t.hasChildren()) {
				if(hasValue(t)){
					fieldList.add(newbase);
				}

			}else if(t.hasChildren()) {
				fieldList.addAll(hasMore(newbase, t));
			}

		}

		return fieldList;
	}

	/**
	 * Checks to see whether the XMLobject has a value and that its not a type object
	 * @param xo XML object
	 * @return Boolean
	 */
	public static boolean hasValue(XmlObject xo) {


		if(xo.getValue()==null || (xo.getName().getLocalPart().toString().equals("type"))) {
			return false;
		}else {
			//System.out.println("Objectcheck: " + xo.getValue());
			return true;
		}

	}

	/**
	 * Given an SObject and fieldname we walk the SObject to get the proper name
	 * @param so SObject
	 * @param fieldname String
	 * @return the corrected fieldname as a String
	 */

	public static ArrayList<String> getField(SObject so, String fieldname) {
		// The getField and getSObjectField methods are case sensitive, so we
		// need to work around this somehow
		String[] parts = fieldname.split("\\.");
		SObject here = so;
		int last = parts.length - 1;
		ArrayList<String> fullField = new ArrayList<String>();
		for (int i = 0; i < last; i++) {
			// Need to walk the "dotted field notation" parts
			String partField = getCorrectedName(here,parts[i]);
			here = (SObject) here.getSObjectField(partField);
			fullField.add(partField);
		}
		String correctedName = getCorrectedName(here,parts[last]);
		fullField.add(correctedName);

		return fullField;

	}

	/**
	 * Given an SObject and fieldname we walk the SObject to get the proper name
	 * @param so SObject
	 * @param fieldname String
	 * @return the corrected fieldname as a String
	 */

	public static String getCorrectedName(SObject so, String name) {
		Iterator<XmlObject> xoi = so.getChildren();
		boolean done = false;
		for (XmlObject xo = xoi.next(); !done;xo = xoi.next()) {
			String correctedName = xo.getName().getLocalPart();
			// log("Comparing (%s) - (%s)", name, correctedName);
			if (name.trim().toLowerCase().equals(correctedName.trim().toLowerCase())) {
				return correctedName;
			}
			done = !xoi.hasNext();
		}
		System.out.printf("Unable to find %s", name);
		return null;
	}

	//builds up a key value map per field, then builds a list, this effectively reduces any error when reading sobjects and manipulating later
	public static ArrayList<HashMap<String,String>> readSobjectList(List<SObject> queryResults) {

		//main container for all records
		ArrayList<HashMap<String,String>> listRecords = new ArrayList<HashMap<String,String>>();

		//create list of fields that we need to map for
		ArrayList<String> fieldNames = new ArrayList<String>(getAllFields(queryResults.get(0)));

		//iterate through the full query results
		for(SObject r:queryResults) {

			//build temp map for field to value mapping per record
			HashMap<String,String> map = new HashMap<String,String>();

			for(String name: fieldNames) {
				String fieldValue = getFieldValue(name,r);

				map.put(name, fieldValue);

			}
			listRecords.add(map);

		}

		return listRecords;

	}

	//gets a field value from an sobject given the full path to the field
	private static String getFieldValue(String name, SObject r) {
		String empty ="";
		String [] split = name.split("\\.");
		if(split.length>0) {
			for(int i = 0; split.length>i; i++) {
				if (split.length-1> i){
					r = (SObject) r.getChild(split[i].toString());
				}else {
					try {
						return r.getField(split[i].toString()).toString();
					}catch(NullPointerException e) {
						return empty;	
					}
				}
			}
		}else {
			try {
				return r.getField(name.toString()).toString();
			}catch(NullPointerException e) {
				return empty;	
			}
		}

		return empty;

	}

	protected static boolean saveToFile(String soql, List<HashMap<String,String>> data, String filename) {
		return saveToFile(soql, data, filename, true);
	}

	protected static boolean saveToFile(String soql ,List<HashMap<String,String>> data, String filename, boolean overwrite) {
		String buildNumber ="";
		buildNumber = (System.getenv("BUILD_NUMBER")!= null) ? System.getenv("BUILD_NUMBER")+"/Salesforce/": "Salesforce/";
		filename = buildNumber + filename;

		File check = new File(buildNumber);

		if(!check.exists()) {
			check.mkdir();
		}
		// Let's figure out what the list of all headers looks like:
		HashSet<String> headerSet = new HashSet<String>();
		for (HashMap<String,String> account : data) {
			headerSet.addAll(account.keySet());
		}

		// Sorting HashSet using List 
		List<String> list = new ArrayList<String>(headerSet); 

		list=sortFromOrignalQuery(soql,list);

		String[] headers = list.toArray(new String[0]);

		List<String[]> records = new ArrayList<String[]>();
		HashMap<Integer,Integer> headerPaddings = setPaddings(headers);

		try {
			CSVWriter writer = new CSVWriter(new FileWriter(filename, !overwrite));
			writer.writeNext(headers);

			for (HashMap<String,String> account : data) {
				String[] values = new String[headers.length];
				for (int i = 0; i < headers.length; i++) {
					values[i] = account.get(headers[i]);
					if (headerPaddings.containsKey(i)) {
						if (headerPaddings.get(i) < values[i].length()) {
							headerPaddings.put(i, values[i].length()+1);
						}
					} else {
						headerPaddings.put(i, values[i].length()+1);
					}
				}

				records.add(values);

				writer.writeNext(values);
			}

			writer.close();

			//if we dont return too many records we can print about 200 to the console else check csv
			if(records.size()< 201) {
				//hard jenkins UI limit 180, actually 168 when printing java line characters
				if(getMaxChar(headerPaddings)<168) {
					//eclipse can handle scrolling and formatting out
					printTablular(headers, records, headerPaddings);
				}else {
					//basic data for jenkins
					printBasic(headers, records, headerPaddings);
				}

			}


			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return true;
		}



	}

	private static void printBasic(String[] headers, List<String[]> records, HashMap<Integer, Integer> headerPaddings) {
		// TODO Auto-generated method stub
		// Print the list objects in tabular format.
		String seperator ="------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------";
		System.out.println(seperator);
		printBasicLineArray(headers,headerPaddings);
		System.out.println(seperator);
		printBasicListAray(records,headerPaddings);
		System.out.println(seperator);
	}


	private static void printBasicListAray(List<String[]> records, HashMap<Integer, Integer> headerPaddings) {
		// TODO Auto-generated method stub
		for(String [] r:records) {
			printBasicLineArray(r,headerPaddings);
		}
	}


	private static void printBasicLineArray(String[] stringArray, HashMap<Integer, Integer> headerPaddings) {
		// TODO Auto-generated method stub
		StringBuffer sb = new StringBuffer();
		for(int i = 0; stringArray.length>i; i++) {
			if(stringArray.length>i+1) {
				sb.append(stringArray[i]+",");
			}else {
				sb.append(stringArray[i]);
			}

		}
		String str = sb.toString();
		System.out.println(str);
	}

	//set padding when we first start based on the headers
	private static HashMap<Integer, Integer> setPaddings(String[] headers) {
		// TODO Auto-generated method stub
		HashMap<Integer,Integer> map = new HashMap<Integer, Integer>();
		for(int i=0; headers.length>i; i++) {
			map.put(i, headers[i].length()+1);;
		}
		return map;
	}

	//prints out table based on headers and list of records and a padding map
	public static void printTablular(String [] headers, List<String[]> records, HashMap<Integer, Integer> headerPaddings) {
		// Print the list objects in tabular format.
		String seperator ="-----------------------------------------------------------------------------";
		seperator=updateSeparator(headerPaddings);
		System.out.println(seperator);
		printFormattedLineArray(headers,headerPaddings);
		System.out.println(seperator);
		printFormattedListAray(records,headerPaddings);
		System.out.println(seperator);
	}

	//prints out our array to the final printformatline method
	private static void printFormattedListAray(List<String[]> records, HashMap<Integer, Integer> headerPaddings) {
		// TODO Auto-generated method stub
		for(String [] r:records) {
			printFormattedLineArray(r,headerPaddings);
		}

	}

	//convert arrays for printing
	public static void printFormattedLineArray(String[] stringArray, HashMap<Integer, Integer> headerPaddings) {
		//   System.out.printf("%10s %30s %20s %5s %5s", "STUDENT ID", "EMAIL ID", "NAME", "AGE", "GRADE");
		StringBuffer sb = new StringBuffer();
		for(int i = 0; stringArray.length>i; i++) {
			sb.append(String.format("|%"+headerPaddings.get(i)+"s| ", stringArray[i]));

		}
		String str = sb.toString();
		System.out.println(str);

	}

	//sets our length and separator line
	private static String updateSeparator(HashMap<Integer, Integer> headerPaddings) {
		// TODO Auto-generated method stub
		int charNeeded = getMaxChar(headerPaddings);
		StringBuilder pad = new StringBuilder();

		for(int i =0; charNeeded>=i; i++) {
			pad.append("_");
		}

		return pad.toString();

	}


	public static int getMaxChar(HashMap<Integer, Integer> headerPaddings) {

		int charTotal =0;
		for (Integer s: headerPaddings.values()) {
			charTotal = charTotal + s;
		}

		int addPad = charTotal/9;
		return charTotal + addPad;

	}


	//sort data
	private static List<String> sortFromOrignalQuery(String soql, List<String> list) {
		// TODO Auto-generated method stub
		HashMap<Integer,String> map = new HashMap<Integer,String> ();

		for(String s : list) {
			int i = soql.toLowerCase().indexOf(s.toLowerCase());

			map.put(i, s);
		}

		// TreeMap to store values of HashMap 
		TreeMap<Integer,String>  sorted = new TreeMap<>(); 

		// Copy all data from hashMap into TreeMap 
		sorted.putAll(map); 

		ArrayList<String> values = new ArrayList<String>(sorted.values());

		return values;

	}

	//convert arrays for printing 

	public static void printLineArray(String[] stringArray) {
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < stringArray.length; i++) {
			sb.append(stringArray[i]);
		}
		String str = Arrays.toString(stringArray);

		System.out.println(str);

	}
	//quick query result, not meant for many records
	public static QueryResult query(PartnerConnection stub, String query) throws RemoteException, ConnectionException {
		return stub.query(query);
	}

	//join method to join a string array
	public static String join(String[] sa) {
		return join(sa, ",");
	}
	//child method of join
	public static String join(String[] sa, String joinWith) {
		return join(sa, joinWith, null);
	}
	//child method of join
	public static String join(String[] sa, String joinWith, String wrap) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < sa.length; i++) {
			String s = sa[i];
			if (wrap != null) {
				s = String.format("%2$s%1$s%2$s", s, wrap);
			}
			sb.append(s);
			if (i < sa.length - 1) {
				sb.append(joinWith);
			}
		}
		return sb.toString();
	}

	//unzipping method need to provide the zip file path, and the extract path when calling this method
	protected static void unzip(String zipfile, String extractPath) throws IOException {
		ZipInputStream zis = new ZipInputStream(new FileInputStream(zipfile));
		for (ZipEntry zi = zis.getNextEntry(); zi != null; zi = zis.getNextEntry()) {

			if(zi.getName().contains("*") && System.getProperty("os.name").toLowerCase().contains("windows")){

				System.out.println("Current System is : " + System.getProperty("os.name") + ", We are removing profile because it cannot be written to Windows Filesystem : " + zi.getName());
				continue;
			}
			//print(String.format("zi.getName() returned %s\n", zi.getName()));
			String filename = String.format("%s/%s", extractPath, zi.getName());
			// System.out.printf("Filename=%s\n", filename);
			if (zi.isDirectory()) {
				// System.out.println("Creating directory");
				new File(filename).mkdirs();
			} else {
				// System.out.println("Creating file");
				//System.out.println(filename);
				List<String> parts = Arrays.asList(filename.split("[/\\\\]"));
				parts = parts.subList(0, parts.size() - 1);
				String path = join(parts.toArray(new String[0]), "/");
				// System.out.printf("Creating directory %s\n", path);
				new File(path).mkdirs();
				StringBuffer sb = new StringBuffer();
				byte[] ba = new byte[1024];
				while (zis.available() > 0) {
					int count = zis.read(ba);
					if (count > 0) {
						sb.append(new String(ba, 0, count));
					}
				}
				FileWriter fw = new FileWriter(filename);
				fw.write(sb.toString());
				fw.close();
			}
		}
		zis.close();
	}

	public static void zip (String directory, String filename) {

		String zipFile = filename;
		ArrayList<String> srcFiles = new ArrayList<String>();

		File dir = new File(directory);
		if (! dir.exists()){
			return;
		}else {

		}
		File folder = new File(directory);

		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {

			srcFiles.add(file.getAbsolutePath());
		}

		//zip the file to package

		try {

			// create byte buffer
			byte[] buffer = new byte[1024];

			FileOutputStream fos = new FileOutputStream(filename);

			ZipOutputStream zos = new ZipOutputStream(fos);

			for (int i=0; i < srcFiles.size(); i++) {

				File srcFile = new File(srcFiles.get(i));
				FileInputStream fis = new FileInputStream(srcFile);

				// begin writing a new ZIP entry, positions the stream to the start of the entry data
				//quick work around for writing correct folder paths in the zip dir need a better scalable method going forward
				zos.putNextEntry(new ZipEntry(srcFile.getName()));

				//System.out.println(srcFile.getName());
				int length;

				while ((length = fis.read(buffer)) > 0) {
					zos.write(buffer, 0, length);
				}

				zos.closeEntry();

				// close the InputStream
				fis.close();

			}

			// close the ZipOutputStream
			zos.close();
		}catch (IOException ioe) {
			System.out.println("Error creating zip file: " + ioe); 
		}

	}

	public void describeMetadata() {
		try {
			FileBasedDeployAndRetrieve.metadataConnection = MetadataLoginUtil.login(sfdcPartner);
			// Assuming that the SOAP binding has already been established.
			DescribeMetadataResult res = 
					FileBasedDeployAndRetrieve.metadataConnection.describeMetadata(44.0);
			StringBuffer sb = new StringBuffer();
			if (res != null && res.getMetadataObjects().length > 0) {
				for (DescribeMetadataObject obj : res.getMetadataObjects()) {
					sb.append(obj.getXmlName()+ ",\n");
				}
			} else {
				sb.append("Failed to obtain metadata types.");
			}
			System.out.println(sb.toString());
		} catch (ConnectionException ce) {
			ce.printStackTrace();
		}
	}


	//describe object method
	public static DescribeSObjectResult describObjectName(PartnerConnection connection, String objectName) throws ConnectionException {
		DescribeSObjectResult object = connection.describeSObject(objectName);
		return object;
	}

	//describe objectList Method bulkifies for list of objects, if we dont return it means the snytax or object reference doesnt exist in source
	public static void describObjects(PartnerConnection connection, String [] objects) throws ConnectionException {
		try {
			DescribeSObjectResult[] objectList = connection.describeSObjects(objects);
			return;
		}catch(InvalidSObjectFault e){
			System.out.println(e.getExceptionMessage());
			System.out.println("Declared Objects do not exist, job is aborting" );
			System.exit(1);
		}

	}

	//bulk method for validating the single fls objects, first we have to iterate and store unique objects and then test fields per object request
	@SuppressWarnings("null")
	public static void describFLSObjects(PartnerConnection connection, String [] objects) throws ConnectionException {
		try {

			HashMap <String, ArrayList<String>> objectToField = new HashMap <String, ArrayList<String>>();
			for (int i =0; i<objects.length; i++){
				String inputKey;
				ArrayList<String> input = new ArrayList<String>();

				//System.out.println(objects[i].split("\\.")[0]);
				input = objectToField.get(objects[i].split("\\.")[0]);

				if(input == null){
					input= new ArrayList<String>();
					input.add(objects[i].split("\\.")[1]);
					inputKey=objects[i].split("\\.")[0].toLowerCase();
					objectToField.put(inputKey, input);

				} else{
					input.add(objects[i].split("\\.")[1]);
					objectToField.put(objects[i].split("\\.")[0].toLowerCase(), input);
				}

			}
			String [] objectListExtract = new String[objectToField.keySet().size()];

			int i = 0;
			for(String a: objectToField.keySet()){
				objectListExtract[i] = a.toLowerCase();
				//System.out.println(a.toLowerCase());
				i++;
			}


			DescribeSObjectResult[] objectList = connection.describeSObjects(objectListExtract);

			for (DescribeSObjectResult d:objectList){
				//System.out.println(d.getName().toLowerCase());
				if(objectToField.containsKey(d.getName().toLowerCase())){
					//System.out.println(d.getName());
					ArrayList<String> listFields = objectToField.get(d.getName().toLowerCase());
					ArrayList<Field> listFieldsD = new ArrayList<Field>();
					Field[] allFields = d.getFields();
					ArrayList<String> convertList = new ArrayList<String>();
					for(Field b:allFields){
						convertList.add(b.getName().toLowerCase());
					}

					for(String f:listFields){
						if (convertList.contains(f.toLowerCase())){

						}else{
							System.out.println("Doesnt contain field: " + f + " Object: " + d.getName());
							System.exit(1);
						}

					}

				}

			}


			return;
		}catch(InvalidSObjectFault e){
			System.out.println(e.getExceptionMessage());
			System.out.println("Declared Objects do not exist, job is aborting" );
			System.exit(1);
		}

	}

	//provide xml document to fix and output might need to update file name 
	public static void fixDocument(Document xml, String filename) throws Exception {
		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		//Writer out = new StringWriter();

		File directory = new File("deploy/objects");
		if (! directory.exists()){
			directory.mkdirs();
			// If you require it to make the entire directory path including parents,
			// use directory.mkdirs(); here instead.

		}
		xml.normalize();
		FileOutputStream fos = new FileOutputStream("deploy/objects/" + filename);
		StreamResult file = new StreamResult(fos);
		tf.transform(new DOMSource(xml), file);
		//System.out.println(out.toString());
	}

	//remove node from a xml node list, need to generate a list and provide a node to be removed
	public static void removeNode(Node node) {
		if (node != null) {
			while (node.hasChildNodes()) {
				removeNode(node.getFirstChild());
			}

			Node parent = node.getParentNode();
			if (parent != null) {
				parent.removeChild(node);
				NodeList childNodes = parent.getChildNodes();
				if (childNodes.getLength() > 0) {
					List<Node> lstTextNodes = new ArrayList<Node>(childNodes.getLength());
					for (int index = 0; index < childNodes.getLength(); index++) {
						Node childNode = childNodes.item(index);
						if (childNode.getNodeType() == Node.TEXT_NODE) {
							lstTextNodes.add(childNode);
						}
					}
					for (Node txtNodes : lstTextNodes) {
						removeNode(txtNodes);
					}
				}
			}
		}
	}


	public static void sendEmail(String emailids, String jobname, String outputfile) throws ConnectionException {	
		Session javamailSession = null;
		Properties mailprops = new Properties();
		mailprops.put("mail.smtp.host", smtp_host);
		javamailSession = Session.getInstance(mailprops);
		//		MimeMessage msg = new MimeMessage(javamailSession);
		String buildNumber ="";
		buildNumber = (System.getenv("BUILD_NUMBER")!= null) ? System.getenv("BUILD_NUMBER"): "";

		try {
			Message message = new MimeMessage(javamailSession);
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("This is the automated email from: " + jobname)).append(System.lineSeparator());
			sb.append(String.format("Attached any results file to the email")).append(System.lineSeparator());

			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailids));
			message.setFrom(new InternetAddress("no-reply@salesforce.com"));
			message.setSubject(String.format("Automated Job: " + jobname + " "+ buildNumber));
			message.setSentDate(new Date());

			// Create the message part
			BodyPart messageBodyPart = new MimeBodyPart();

			// Now set the actual message
			messageBodyPart.setText(sb.toString());

			// Create a multipar message
			Multipart multipart = new MimeMultipart();

			// Set text message part
			multipart.addBodyPart(messageBodyPart);


			try {
				for(String f:outputfile.split(",")){
					String filename = f;
					if(Files.exists(Paths.get(filename))) {
						// Part two is attachment
						messageBodyPart = new MimeBodyPart();
						DataSource source = new FileDataSource(filename);
						messageBodyPart.setDataHandler(new DataHandler(source));
						messageBodyPart.setFileName(filename);

						multipart.addBodyPart(messageBodyPart);

					}
				}


			}catch(InvalidPathException e) {

			}
			// Send the complete message parts
			message.setContent(multipart);

			// Send message
			Transport.send(message);
			System.out.println("Email Succesfully Sent");
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void sendEmailMessage(String emailids, String jobname, String text) throws ConnectionException {	
		Session javamailSession = null;
		Properties mailprops = new Properties();
		mailprops.put("mail.smtp.host", smtp_host);
		javamailSession = Session.getInstance(mailprops);
		//		MimeMessage msg = new MimeMessage(javamailSession);
		String buildNumber ="";
		buildNumber = (System.getenv("BUILD_NUMBER")!= null) ? System.getenv("BUILD_NUMBER"): "";

		try {
			Message message = new MimeMessage(javamailSession);
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("This is the automated email from: " + jobname)).append(System.lineSeparator());
			sb.append(String.format(text)).append(System.lineSeparator());
			sb.append(String.format("Attached any results file to the email")).append(System.lineSeparator());

			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailids));
			message.setFrom(new InternetAddress("no-reply@salesforce.com"));
			message.setSubject(String.format("Automated Job: " + jobname + " "+ buildNumber));
			message.setSentDate(new Date());

			// Create the message part
			BodyPart messageBodyPart = new MimeBodyPart();

			// Now set the actual message
			messageBodyPart.setText(sb.toString());

			// Create a multipar message
			Multipart multipart = new MimeMultipart();

			// Set text message part
			multipart.addBodyPart(messageBodyPart);

			// Send the complete message parts
			message.setContent(multipart);

			// Send message
			Transport.send(message);
			System.out.println("Email Succesfully Sent");
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	//method to do a forupdate against existing data, specifically to update presentinrms flag to true for all data passed here.
	public static SaveResult[] updateEmail(PartnerConnection connection, Map<ArrayList<String>,HashSet<String>> emailList) throws ConnectionException {
		ArrayList<SObject> finalList = new ArrayList<SObject>();
		SaveResult[] srs=null;
		for (ArrayList<String> iter : emailList.keySet()) {
			SObject user = new SObject();
			user.setType("User");
			user.setField("id", iter.get(0));
			user.setField("Email", iter.get(1));
			finalList.add(user);
		}
		int listSize = finalList.size();
		for (int batch = 0; finalList.size()>batch;){
			//firstid = list.get(list.size()==0?0:list.size()-1);
			List<SObject> batchList = finalList.subList(batch, batch = batch + 200 >listSize-1 ? listSize : batch + 200 );
			srs = connection.update(batchList.toArray(new SObject[0]));
			//SaveResult[] srs = connection.update(finalList.toArray(new SObject[0]));


			System.out.printf("Received %d SaveResults back\n", srs.length);
			int srIndex = 0;
			for (SaveResult sr : srs) {
				if (!sr.isSuccess()) {
					SObject mo = finalList.get(srIndex);
					System.out.printf("Error inserting %s %s %s \n", mo.getField("Username"),mo.getField("Email"), mo.getField("id"));
					com.sforce.soap.partner.Error[] errors = sr.getErrors();
					for (com.sforce.soap.partner.Error error : errors) {
						System.out.println(error.getMessage());
					}
				}
				srIndex++;
			}

		}
		return srs;
	}


	//method to do a forupdate against existing data, update it to one value
	public static SaveResult[] updateRecordsFixedValue(PartnerConnection connection, Map<ArrayList<String>,ArrayList<String>> validatedList, String objectType,String fieldForUpdate,String valueOffield,String fieldType) throws ConnectionException, ParseException {
		ArrayList<SObject> finalList = new ArrayList<SObject>();
		SaveResult[] srs=null;
		boolean convertValueOffield= Boolean.valueOf(valueOffield);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat sdtf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		TimeZone tz = TimeZone.getTimeZone("PST");
		sdf.setTimeZone(tz);
		sdtf.setTimeZone(tz);
		Calendar dateTime = new GregorianCalendar();


		for (ArrayList<String> iter : validatedList.keySet()) {
			SObject objectToUpdate = new SObject();
			objectToUpdate.setType(objectType);
			objectToUpdate.setField("Id", iter.get(0));
			if(fieldType =="String"){
				objectToUpdate.setField(fieldForUpdate,valueOffield);
			}else if(fieldType=="boolean"){
				objectToUpdate.setField(fieldForUpdate,convertValueOffield);	
			}else if (fieldType=="date"){
				objectToUpdate.setField(fieldForUpdate,sdf.parse(valueOffield));
			}else if (fieldType=="datetime"){
				//objectToUpdate.setField(fieldForUpdate,valueOffield);
				dateTime.setTime(sdtf.parse(valueOffield));
				objectToUpdate.setField(fieldForUpdate,dateTime);
			}else {
				objectToUpdate.setField(fieldForUpdate,valueOffield);
			}
			finalList.add(objectToUpdate);
		}
		int listSize = finalList.size();
		for (int batch = 0; finalList.size()>batch;){
			//firstid = list.get(list.size()==0?0:list.size()-1);
			List<SObject> batchList = finalList.subList(batch, batch = batch + 200 >listSize-1 ? listSize : batch + 200 );
			srs = connection.update(batchList.toArray(new SObject[0]));
			//SaveResult[] srs = connection.update(finalList.toArray(new SObject[0]));


			System.out.printf("Received %d SaveResults back\n", srs.length);
			int srIndex = 0;
			for (SaveResult sr : srs) {
				if (!sr.isSuccess()) {
					SObject mo = finalList.get(srIndex);
					System.out.printf("Error inserting %s %s \n", mo.getField(fieldForUpdate), mo.getField("Id"));
					com.sforce.soap.partner.Error[] errors = sr.getErrors();
					for (com.sforce.soap.partner.Error error : errors) {
						System.out.println(error.getMessage());
					}
				}
				srIndex++;
			}

		}
		return srs;
	}

	public static SaveResult[] updateUserEmails(PartnerConnection connection,List<SObject> queryResults) throws ConnectionException {
		// TODO Auto-generated method stub
		System.out.println("Attempting to update data in env, list is: " + queryResults.size());
		List<SObject> finalList = queryResults;
		SaveResult[] srs=null;

		int listSize = finalList.size();
		for (int batch = 0; finalList.size()>batch;){
			//firstid = list.get(list.size()==0?0:list.size()-1);
			List<SObject> batchList = finalList.subList(batch, batch = batch + 200 >listSize-1 ? listSize : batch + 200 );
			srs = connection.update(batchList.toArray(new SObject[0]));
			//SaveResult[] srs = connection.update(finalList.toArray(new SObject[0]));


			System.out.printf("Received %d SaveResults back\n", srs.length);
			int srIndex = 0;
			for (SaveResult sr : srs) {
				if (!sr.isSuccess()) {
					SObject mo = finalList.get(srIndex);
					System.out.printf("Error inserting %s %s %s \n", mo.getField("Email"),mo.getField("Username"), mo.getField("id"));
					com.sforce.soap.partner.Error[] errors = sr.getErrors();
					for (com.sforce.soap.partner.Error error : errors) {
						System.out.println(error.getMessage());
					}
				}
				srIndex++;
			}

		}
		return srs;
	}

	public static SaveResult[] insertUsers(PartnerConnection connection,List<SObject> queryResults) throws ConnectionException {
		// TODO Auto-generated method stub
		System.out.println("Attempting to insert data for list size: " + queryResults.size());
		List<SObject> finalList = queryResults;
		SaveResult[] srs=null;

		int listSize = finalList.size();
		for (int batch = 0; finalList.size()>batch;){
			//firstid = list.get(list.size()==0?0:list.size()-1);
			List<SObject> batchList = finalList.subList(batch, batch = batch + 200 >listSize-1 ? listSize : batch + 200 );
			srs = connection.create(batchList.toArray(new SObject[0]));
			//SaveResult[] srs = connection.update(finalList.toArray(new SObject[0]));


			System.out.printf("Received %d SaveResults back\n", srs.length);
			int srIndex = 0;
			for (SaveResult sr : srs) {
				if (!sr.isSuccess()) {
					SObject mo = finalList.get(srIndex);
					System.out.printf("Error inserting %s %s %s \n", mo.getField("Email"),mo.getField("Username"), mo.getField("id"));
					com.sforce.soap.partner.Error[] errors = sr.getErrors();
					for (com.sforce.soap.partner.Error error : errors) {
						System.out.println(error.getMessage());
					}
				}
				srIndex++;
			}

		}
		return srs;
	}

	//id in list conversion method for salesforce querying
	public static String idInConversion(ArrayList<String> tempIds) {
		// TODO Auto-generated method stub
		if (tempIds.size()>0) {
			String collection ="(";
			for(String s:tempIds) {
				collection = collection + "'"+s+"',";
			}

			collection = collection.substring(0, collection.lastIndexOf(","));

			collection = collection + ")";

			return collection;

		}else {
			return null;
		}

	}

	//need to provide a sourceCredential which consists of an array of three things uname, password and link

	public void setCrendential (String[] credential) {

		sourceCredential=credential;

	}

	//in each method in this class we should use this to set the connection based on the credential being provided
	public void setPartnerConnection () {

		if(!checkConnectionOpen(sfdcPartner)) {
			sfdcPartner = MetadataLoginUtil.connect(sourceCredential);
		}

	}

	//in each method in this class we should use this to set the connection based on the credential being provided
	public void setToolingConnection () throws ConnectionException {

		if(!checkConnectionOpen(sfdcTooling)) {
			sfdcTooling = MetadataLoginUtil.toolingConnection(sourceCredential);
		}
	}

	//in each method in this class we should use this to set the connection based on the credential being provided
	public void setMetadataConnection () throws ConnectionException {

		if(!checkConnectionOpen(sfdcMetadata)) {
			sfdcMetadata = MetadataLoginUtil.login(sourceCredential);
		}
	}


	//get a salesforce file declare filename to search for and file path to write to specifically
	public void getContentVersion(String filename, String filepath) throws ConnectionException {

		//sfdcPartner = MetadataLoginUtil.connect(sourceCredential);
		setPartnerConnection ();

		String [] nameType = filename.split("\\.");

		String contentQuery = String.format("SELECT id FROM Contentversion where contentdocumentid in (select id from contentdocument where title ='%s' and fileextension='%s' and createdby.profile.name like 'System%%') order by createddate desc limit 1",nameType[0],nameType[1]);

		com.sforce.soap.partner.QueryResult temp = sfdcPartner.query(contentQuery);

		if(temp.getSize()==0) {
			System.out.println("No file on Salesforce Server yet by the name of: " +filename);
			return;
		}

		String id =temp.getRecords()[0].getId();

		//String [] ids = {"0690M00000A5qidQAB"};
		String [] ids = {id};
		//tooling.retrieve("", "properties", ids);

		SObject[] fC = null;
		String fileContent = null;
		byte[] encodedString  =null;
		try {
			fC = sfdcPartner.retrieve("VersionData", "ContentVersion", ids);
			fileContent = fC[0].getField("VersionData").toString();
			encodedString = Base64.getDecoder().decode(fileContent.getBytes());
			//System.out.println(encodedString);
		} catch (ConnectionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//System.out.println(fileContent);
		//Date date = new Date();
		// String attachName = "status.properties";
		File outputFile = new File(filepath);
		try {
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile));
			bos.write(encodedString);
			bos.flush();
			bos.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("File written out to: " + filepath);
		return;
	}

	//get endpoint login returns something like this https://org62.my.salesforce.com
	public static String getEndPoint(PartnerConnection pc) {
		String endPoint = null;
		endPoint = pc.getConfig().getServiceEndpoint().split("/services/")[0];

		return endPoint;

	}

	//new method can be created for full public visibility for files using ContentDistribution

	public static String setContentDistribution(String id){
		return id;

		//validate contentdocument id

		//insert a new ContentDistribution record

		//return newId

	}


	//getContentDistributionPublicLink

	public static String getContentDistributionPublicLink(String id){
		return id;

		//query object

		//return field DistributionPublicUrl or ContentDownloadUrl

	}


	//call this method to set company viewing on your document
	public String pushContentDocumentLink(String id) throws ConnectionException, IOException {
		setPartnerConnection ();

		String newId = null;
		String LinkedEntityId = getLinkedEntityId();

		//review if its contentdocument or version first
		if(id.contains("069")) {
			SObject cVerLink = new SObject();
			cVerLink.setType("ContentDocumentLink");
			cVerLink.setField("ContentDocumentId", id);;
			cVerLink.setField("Visibility","AllUsers");
			cVerLink.setField("ShareType","V");
			cVerLink.setField("LinkedEntityId",LinkedEntityId);

			SObject[] cVL = {cVerLink};

			try {
				SaveResult[] result = sfdcPartner.create(cVL);
				newId = result[0].getId();
				return newId;
			} catch (ConnectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return newId;
		}else if(id.contains("068")) {
			String newCDId = getContentDocumentid(id);
			newCDId = pushContentDocumentLink(newCDId);
			return newCDId;
		} else {
			throw new NullPointerException(id +" not of ContentVersionType or ContentDocumentType");
		}


	}

	public String getLinkedEntityId() throws ConnectionException {
		// TODO Auto-generated method stub
		String id = null;
		id=getOrgId();
		return id;
	}

	public String getOrgId() throws ConnectionException {
		// TODO Auto-generated method stub
		setPartnerConnection();

		String id = null;
		id=sfdcPartner.getUserInfo().getOrganizationId();
		return id;
	}

	public String getContentDocumentid(String id) throws ConnectionException {
		// TODO Auto-generated method stub
		setPartnerConnection ();

		String cDID =null;
		String contentQuery = String.format("SELECT ContentDocumentId FROM ContentVersion where id ='%s' limit 1",id);

		com.sforce.soap.partner.QueryResult temp = sfdcPartner.query(contentQuery);

		if(temp.getSize()==0) {

		} else {
			cDID = temp.getRecords()[0].getField("ContentDocumentId").toString();
			return cDID;
		}

		return cDID;
	}



	//call this method if your file is in the parent directory which means we do not need to have two variables
	public void getContentVersion(String filename) throws ConnectionException {

		getContentVersion(filename, filename);

	}


	//call this method if your file is in the parent directory which means we do not need to have two variables
	public void pushContentVersion(String filename) throws ConnectionException, IOException {

		pushContentVersion(filename, filename);

	}


	//parent method to write a Salesforce file, provide a file path and a filename to send off to salesforce
	//child methods will be called if file exists or doesnt exist
	//will return id of new salesforce file if created
	public String pushContentVersion(String file, String filename) throws ConnectionException, IOException {

		//sfdcPartner = MetadataLoginUtil.connect(sourceCredential);
		setPartnerConnection ();

		String[] fileType = filename.split("\\.");

		//System.out.println(fileType[0] + fileType[1]);

		String contentQuery = String.format("select id from contentdocument where title ='%s' and fileextension='%s' and createdby.profile.name like 'System%%' limit 1",fileType[0],fileType[1]);

		com.sforce.soap.partner.QueryResult temp = sfdcPartner.query(contentQuery);

		//System.out.println(contentQuery);

		if(temp.getSize()==0) {

			String newCVid = pushFirstContentVersion(file, filename);

			System.out.println("New ContentVersion was created: "+ newCVid);

			return newCVid;


		} else {

			String currentCVid = temp.getRecords()[0].getId();

			String newCVid = pushNewContentVersion(file, filename, currentCVid);

			System.out.println("New ContentVersion was created: " + newCVid);

			return newCVid;

		}

	}

	public String pushFirstContentVersion (String file, String filename) throws IOException {

		setPartnerConnection ();

		String [] nameType = filename.split("\\.");
		String newId = null;

		byte[] bytes = Files.readAllBytes(new File(file).toPath());


		SObject cVer = new SObject();
		cVer.setType("ContentVersion");
		cVer.setField("VersionData", bytes);;
		cVer.setField("ContentDocumentId",null);
		cVer.setField("ReasonForChange","Automation");
		cVer.setField("PathOnClient",filename);
		SObject[] cV = {cVer};

		try {
			SaveResult[] result = sfdcPartner.create(cV);
			newId = result[0].getId();
			return newId;
		} catch (ConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newId;


	}

	public String pushNewContentVersion (String file, String filename, String currentCVid) throws IOException {

		setPartnerConnection ();

		String [] nameType = filename.split("\\.");
		String newId = null;

		byte[] bytes = Files.readAllBytes(new File(file).toPath());


		SObject cVer = new SObject();
		cVer.setType("ContentVersion");
		cVer.setField("VersionData", bytes);;
		cVer.setField("ContentDocumentId",currentCVid);
		cVer.setField("ReasonForChange","Automation");
		cVer.setField("PathOnClient",filename);
		SObject[] cV = {cVer};

		try {
			SaveResult[] result = sfdcPartner.create(cV);
			newId = result[0].getId();
			return newId;
		} catch (ConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newId;


	}

	//method to turn file into bytes for uploading
	public static byte[] readFileToByteArray(File file){
		FileInputStream fis = null;
		// Creating a byte array using the length of the file
		// file.length returns long which is cast to int
		byte[] bArray = Base64.getEncoder().encode(new byte[(int) file.length()]);
		try{
			fis = new FileInputStream(file);
			fis.read(bArray);
			fis.close();        

		}catch(IOException ioExp){
			ioExp.printStackTrace();
		}
		return bArray;
	}

	public void describeMetadataTypes() {

		try {
			setMetadataConnection();
			// Assuming that the SOAP binding has already been established.
			DescribeMetadataResult res = sfdcMetadata.describeMetadata(44.0);
			StringBuffer sb = new StringBuffer();
			if (res != null && res.getMetadataObjects().length > 0) {
				for (DescribeMetadataObject obj : res.getMetadataObjects()) {
					sb.append(obj.getXmlName()+ ",\n");
				}
			} else {
				sb.append("Failed to obtain metadata types.");
			}
			System.out.println(sb.toString());
		} catch (ConnectionException ce) {
			ce.printStackTrace();
		}
	}


	public void describeMetadataSearch(String search) {
		try {
			setMetadataConnection();
			// Assuming that the SOAP binding has already been established.
			DescribeMetadataResult res = sfdcMetadata.describeMetadata(44.0);
			StringBuffer sb = new StringBuffer();
			if (res != null && res.getMetadataObjects().length > 0) {
				for (DescribeMetadataObject obj : res.getMetadataObjects()) {
					if(obj.getXmlName().toString().equals(search)) {
						sb.append("MetadataType: " + obj.getXmlName()+ "\n");
						sb.append("Folder Dir: " +obj.getDirectoryName()+ "\n");
						for (String s1:obj.getChildXmlNames()) {
							sb.append("ChildXmlName: " +s1+ "\n");

						}
					}


				}
			} else {
				sb.append("Failed to obtain metadata types.");
			}
			System.out.println(sb.toString());
		} catch (ConnectionException ce) {
			ce.printStackTrace();
		}
	}


	public void describeValueType(String string) throws ConnectionException {
		doDescribe(String.format("{http://soap.sforce.com/2006/04/metadata}%s",string));
	}

	public void doDescribe(String type) throws ConnectionException {
		setMetadataConnection();

		DescribeValueTypeResult result = sfdcMetadata.describeValueType(type);
		StringBuffer sb = new StringBuffer();

		sb.append("Describing " + type + " ...\n");

		if (result.getApiCreatable() == true) {
			sb.append("Is API creatable.\n");
		} else {
			sb.append("Is not API creatable.\n");
		}

		ValueTypeField parentField = result.getParentField();
		if (parentField != null) {
			sb.append("** Parent type fields **\n");
			if (parentField.getIsForeignKey()) {
				sb.append("This field is a foreign key.\n");
				for (String fkDomain : parentField.getForeignKeyDomain()) { 
					sb.append("Foreign key domain: " + fkDomain + "\n");
				}
			}              
		}

		sb.append("** Value type fields **\n");
		for(ValueTypeField field : result.getValueTypeFields()) {
			sb.append("***************************************************\n");
			sb.append("Name: " + field.getName() + "\n");
			sb.append("SoapType: " + field.getSoapType() + "\n");
			for(ValueTypeField f:field.getFields()) {

				sb.append("fieldname: " + f.getName() + "\n");
				//				for(PicklistEntry v:f.getPicklistValues()) {
				//					sb.append("picklist: " + v.getValue() + "\n");
				//				}
			}
			if (field.getIsForeignKey()) {
				sb.append("This field is a foreign key.\n");
				for (String fkDomain : field.getForeignKeyDomain()) { 
					sb.append("Foreign key domain: " + fkDomain + "\n");
				}
			}
			sb.append("***************************************************\n");
		}
		System.out.println(sb.toString());
	}

	public void listMetadata(String type) {
		try {
			setMetadataConnection();
			ListMetadataQuery query = new ListMetadataQuery();
			query.setType(type);
			//query.setFolder(null);
			double asOfVersion = 48.0;
			// Assuming that the SOAP binding has already been established.
			FileProperties[] lmr = sfdcMetadata.listMetadata(
					new ListMetadataQuery[] {query}, asOfVersion);
			if (lmr != null) {
				for (FileProperties n : lmr) {
					System.out.println("Component id: " +n.getId());
					System.out.println("Component fullName: " + n.getFullName());
					System.out.println("Component type: " + n.getType());
				}
			}            
		} catch (ConnectionException ce) {
			ce.printStackTrace();
		}
	}

	public void describeSobjectType(String type) {
		try {
			setPartnerConnection();
			DescribeSObjectResult res = sfdcPartner.describeSObject(type);
			StringBuffer sb = new StringBuffer();
			sb.append("OjbectXMLname: " +res.getName() +" ObjectlabelName: " + res.getLabel() +"\n");
			for (Field r: res.getFields()) {

				sb.append("FieldXMLname: " +r.getName() +" FieldlabelName: " + r.getLabel() +"\n");


			}

			System.out.println(sb);
		}catch (ConnectionException ce) {
			ce.printStackTrace();
		}

	}

	public void partnerGetUserPermissions() {
		try {
			setPartnerConnection();
			DescribeSObjectResult res = sfdcPartner.describeSObject("Profile");
			StringBuffer sb = new StringBuffer();
			for (Field r: res.getFields()) {
				if(r.getName().contains("Permissions")){
					sb.append("PermissionXMLName: " +r.getName().replace("Permissions", "") +" PermissionName: " + r.getLabel() +"\n");
				}

			}

			System.out.println(sb);
		}catch (ConnectionException ce) {
			ce.printStackTrace();
		}

	}

	//get unique name based on email/username
	public static String getName(String u) {
		// TODO Auto-generated method stub
		return u.toLowerCase().replaceAll("@salesforce.com", "").replaceAll("\\+","").replaceAll("\\-","").replaceAll("\\_","").replaceAll("\\/","").replaceAll("\\:", "").replaceAll("\\&","").replaceAll("\\(", "").replaceAll("\\)","").replaceAll("\\.", "");
	}


	//method to do a forupdate against all data from file, each field type needs to be parsed correctly and added to list of sobjects
	public static ArrayList<SObject> setRecordsForUpdate(ArrayList<HashMap<String, String>> records, String objectType, HashMap<String, String> validHeadersToType, HashMap<String, String> headerToValidHeaderMap) throws ConnectionException, ParseException {
		//container for final list of sobjects
		ArrayList<SObject> finalList = new ArrayList<SObject>();

		for (HashMap<String, String> r : records) {
			SObject objectToUpdate = new SObject();
			objectToUpdate.setType(objectType);
			//all headers have been lowercased for consistency
			objectToUpdate.setField("Id", r.get("id"));

			for(Entry<String, String> es: r.entrySet()) {

				if(headerToValidHeaderMap.containsKey(es.getKey())){
					String properName = headerToValidHeaderMap.get(es.getKey());
					String fieldType = validHeadersToType.get(properName);
					if(fieldType =="string"){
						objectToUpdate.setField(properName,es.getValue());
					}else if(fieldType=="boolean"){
						objectToUpdate.setField(properName,setBoolean(es.getValue()));	
					}else if (fieldType=="date"){
						objectToUpdate.setField(properName,setDate(es.getValue()));
					}else if (fieldType=="datetime"){
						objectToUpdate.setField(properName,setDateTime(es.getValue()));
					}else if (fieldType=="reference"){
						objectToUpdate.setField(properName,setReference(es.getValue()));
					}else if (fieldType=="currency"){
						objectToUpdate.setField(properName,setCurrency(es.getValue()));
					}else if (fieldType=="double"){
						objectToUpdate.setField(properName,setDouble(es.getValue()));
					}else if (fieldType=="percent"){
						objectToUpdate.setField(properName,setPercent(es.getValue()));
					}else if (fieldType=="email"){
						objectToUpdate.setField(properName,setEmail(es.getValue()));
					}else {
						//will try to set whatever is left to string and let salesforce tell us whats wrong
						objectToUpdate.setField(properName,es.getValue());
					}
				}
			}

			finalList.add(objectToUpdate);
		}

		return finalList;

	}

	//method to check and set email
	public static String setEmail(String v) {
		// TODO Auto-generated method stub
		if(v.contains("@")&& v.toLowerCase().contains(".com")) {
			return v;
		}else if (v.equals("")){
			return v;
		}else {
			throw new NullPointerException("Value: " +v+" not of Email type");

		}

	}

	//method for percents
	public static double setPercent(String v)throws ParseException, NumberFormatException, NullPointerException{
		// TODO Auto-generated method stub
		double d = Double.parseDouble(v);
		return d;
	}

	//method for doubles
	public static double setDouble(String v)throws ParseException, NumberFormatException, NullPointerException{
		// TODO Auto-generated method stub
		double d = Double.parseDouble(v);
		return d;
	}

	//method for currency
	public static double setCurrency(String v) throws ParseException, NumberFormatException, NullPointerException {
		// TODO Auto-generated method stub
		double d = Double.parseDouble(v);
		return d;

	}

	//method for reference/id type
	public static String setReference(String v) {
		// TODO Auto-generated method stub
		if(v.length()==15||v.length()==18) {
			return v;
		}else if (v.equals("")){
			return v;
		}else {
			throw new NullPointerException("Value: " +v+" not of Reference type");

		}

	}

	//method for datetime
	public static Calendar setDateTime(String v) throws ParseException{
		Calendar dateTime = new GregorianCalendar();
		SimpleDateFormat sdtf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		sdtf.setTimeZone(tz);
		dateTime.setTime(sdtf.parse(v));
		return dateTime;
	}

	//method for date
	public static Date setDate(String v) throws ParseException{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		sdf.setTimeZone(tz);
		return sdf.parse(v);
	}

	//method can be more optimized maybe with enumerated class state
	public static Boolean setBoolean(String v) throws ParseException{
		//helper lists
		String [] accTrueValues = {"1","yes","true"};
		String [] accFalseValues = {"0","no","false"};

		for(String s:accFalseValues) {
			if(v.toLowerCase().equals(s)) {
				return false;
			}
		}

		for(String s:accTrueValues) {
			if(v.toLowerCase().equals(s)) {
				return true;
			}
		}

		throw new NullPointerException("Value: " +v+" not Boolean type");


	}

	//return string
	public static String setString(String v){
		return v;
	}

	//soap call at 200 records a batch
	public static ArrayList<SaveResult[]> updateRecords(PartnerConnection connection, ArrayList<SObject> finalList) throws ConnectionException {
		ArrayList<SaveResult[]> srsAll= new ArrayList<SaveResult[]>();
		SaveResult[] srs=null;
		int listSize = finalList.size();
		for (int batch = 0; finalList.size()>batch;){

			List<SObject> batchList = finalList.subList(batch, batch = batch + 200 >listSize-1 ? listSize : batch + 200 );
			srsAll.add(connection.update(batchList.toArray(new SObject[0])));

			System.out.printf("Received %d SaveResults back\n", srs.length);
			int srIndex = 0;
			for (SaveResult sr : srs) {
				if (!sr.isSuccess()) {
					SObject mo = finalList.get(srIndex);
					sr.setId(mo.getField("Id").toString());
					System.out.printf("Error Updating %s \n", mo.getField("Id"));
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

	//call to validate SObject and get all fields about object
	/**
	 * Calls Salesforce and passes an Sobject for Description. This will describeSObject 
	 * if Salesforce finds it else it hits a Api Query Fault
	 * 
	 *
	 * @param apiObjectName The <code>SObject.name</code> to be checked against
	 * @param pConnection Partner Connection 
	 * @throws Exception 
	 */
	public static DescribeSObjectResult validateObject(String apiObjectName, PartnerConnection pConnection) throws Exception {
		// TODO Auto-generated method stub

		try {	
			DescribeSObjectResult sObj = pConnection.describeSObject(apiObjectName);
			return sObj;
		}catch (ApiQueryFault e) {

			throw new Exception("Sobject invalid: " + apiObjectName + ", Cannot continue");
		}

	}

	/**
	 * 
	 * @param headers
	 * @param sObjResult
	 * @return
	 */

	//validate headers against fields in describe result
	public static HashMap<String, String> validateHeaders(String[] headers, DescribeSObjectResult sObjResult) {
		// TODO Auto-generated method stub
		HashMap<String, String> map = new HashMap<String, String>();

		Field[] list = sObjResult.getFields();


		for(String s:headers){
			for(Field f : list) {
				if(f.getName().toLowerCase().equals(s.toLowerCase())){
					//System.out.println(s);
					map.put(f.getName(),f.getType().toString());
					headerToValidHeaderMap.put(s,f.getName());
				}
			}

		}

		return map;

	}

	//get headers from list map
	public static String[] getHeaders(ArrayList<HashMap<String, String>> records) {
		// TODO Auto-generated method stub

		HashSet<String> headerSet = new HashSet<String>();
		for (HashMap<String,String> r : records) {
			headerSet.addAll(r.keySet());
		}

		String[] headers = headerSet.toArray(new String[0]);

		return headers;

	}

	//get file data from large csv, also handles character conversion for 15 digit ids to 18 digit ids
	public static ArrayList<HashMap<String, String>> getFileData(String filename) throws IOException {
		ArrayList<HashMap<String,String>> ids = new ArrayList<HashMap<String,String>>();
		try {
			CSVReader reader = new CSVReader(new FileReader(filename));
			String[] row = reader.readNext();
			String[]  headers = null;
			while (row != null) {
				if (headers == null) {
					if (String.join(", ", row).toLowerCase().contains("id")) {
						headers = row;
					}
					else {
						headers = new String[] {"id"};
						HashMap<String,String> ht = new HashMap<String,String>();
						ht.put("id", convertID(row[0]));
						ids.add(ht);
					}
				}
				else {
					HashMap<String,String> ht = new HashMap<String,String>();
					for (int i = 0; i < headers.length; i++) {
						String header = headers[i].toLowerCase();
						if ((i < row.length)) {
							if(headers[i].toLowerCase().equals("id")) {
								ht.put(header, convertID(row[i]));
							}else {
								ht.put(header, row[i]);
							}

						}else {
							ht.put(header,null);
						}

					}
					String id = ht.get("id");
					if (id != null && !"".equals(id.trim())) {
						ids.add(ht);
					}
				}
				row = reader.readNext();
			}
			reader.close();
		}
		catch (FileNotFoundException e) {

		}
		return ids;
	}

	//truncate 18 to 15
	public static String char15format(String test) {
		// TODO Auto-generated method stub
		String sub = test.substring(0, (test.length() < 15)? test.length() : 15);
		return sub;
	}

	//method that converts from 15 char to 18 
	public static String convertID(String id)
	{
		if(id.length() >= 18 || id.length() < 15 ) return id;

		String suffix = "";
		for(int i=0;i<3;i++){

			Integer flags = 0;

			for(int j=0;j<5;j++){
				String c = id.substring(i*5+j,i*5+j+1);

				if(c.compareTo("A")  >= 0 && c.compareTo("Z") <= 0){

					flags += 1 << j;
				}
			}

			if (flags <= 25) {

				suffix += "ABCDEFGHIJKLMNOPQRSTUVWXYZ".substring(flags,flags+1);

			}else suffix += "012345".substring(flags-26,flags-26+1);
		}

		return id+suffix;
	}

	//check if file exists
	public static boolean doesFileExist(String queryMap) {
		File test = new File(queryMap);
		if(!test.exists()) {
			return false;
		}
		return true;
	}

	//basic out for saved results
	public static void saveResults(SaveResult[] results) throws IOException {
		// TODO Auto-generated method stub
		String filename = "SuccessResults.csv";
		CSVWriter writer = new CSVWriter(new FileWriter(filename));	
		String[] header = {"id","success","errors"};
		writer.writeNext(header);
		for(SaveResult r:results) {
			String [] line = {r.getId(),String.valueOf(r.isSuccess()),(r.getErrors().length>0 ? r.getErrors()[0].getMessage():"")};
			writer.writeNext(line);
		}
		writer.close();

		p("Printed results out to: " + filename);
	}

	//basic out for saved results
	public static void saveResultsArray(ArrayList<SaveResult[]> results, String string) throws IOException {
		// TODO Auto-generated method stub
		String filename = "SuccessResults" + string;
		CSVWriter writer = new CSVWriter(new FileWriter(filename));	
		String[] header = {"id","success","errors"};
		writer.writeNext(header);
		for(SaveResult[] res:results) {
			for(SaveResult r:res) {
				String [] line = {r.getId(),String.valueOf(r.isSuccess()),(r.getErrors().length>0 ? r.getErrors()[0].getMessage():"")};
				writer.writeNext(line);
			}
		}

		writer.close();

		p("Printed results out to: " + filename);
	}
	
	//soap call at 200 records a batch
	public static ArrayList<DeleteResult[]> deleteRecords(PartnerConnection connection, ArrayList<String> finalList) throws ConnectionException {
		ArrayList<DeleteResult[]> drsAll= new ArrayList<DeleteResult[]>();
		DeleteResult[] drs=null;
		int listSize = finalList.size();
		for (int batch = 0; finalList.size()>batch;){

			List<String> batchList = finalList.subList(batch, batch = batch + 200 >listSize-1 ? listSize : batch + 200 );
		    String [] deleteBatch =batchList.toArray(new String [0]);

		    drs = connection.delete(batchList.toArray(deleteBatch));
			drsAll.add(drs);
			System.out.printf("Received %d SaveResults back\n", drs.length);
			int srIndex = 0;
			for (DeleteResult dr : drs) {
				if (!dr.isSuccess()) {
					String mo = finalList.get(srIndex);
					dr.setId(mo);
					System.out.printf("Error Updating %s \n", mo);
					com.sforce.soap.partner.Error[] errors = dr.getErrors();
					for (com.sforce.soap.partner.Error error : errors) {
						System.out.println(error.getMessage());
					}
				}
				srIndex++;
			}

		}
		return drsAll;
	}

	//basic out for saved results
	public static void deleteResultsArray(ArrayList<DeleteResult[]> results, String string) throws IOException {
		// TODO Auto-generated method stub
		String filename = "DeleteResults" + string;
		CSVWriter writer = new CSVWriter(new FileWriter(filename));	
		String[] header = {"id","success","errors"};
		writer.writeNext(header);
		for(DeleteResult[] res:results) {
			for(DeleteResult r:res) {
				String [] line = {r.getId(),String.valueOf(r.isSuccess()),(r.getErrors().length>0 ? r.getErrors()[0].getMessage():"")};
				writer.writeNext(line);
			}
		}

		writer.close();

		p("Printed results out to: " + filename);
	}

	//method to do a forupdate against all data from file, each field type needs to be parsed correctly and added to list of sobjects
	public static ArrayList<String> setRecordsForDelete(ArrayList<HashMap<String, String>> records) throws ConnectionException, ParseException {
		//container for final list of sobjects
		ArrayList<String> finalList = new ArrayList<String>();

		for (HashMap<String, String> r : records) {
			//all headers have been lowercased for consistency
			finalList.add(r.get("id"));
		}

		return finalList;

	}

	//quickprint
	public static void p(String string) {

		System.out.println(string);

	}

	public static void resetPasswords(PartnerConnection pConn, List<String> ids, String dummyPassword) throws ConnectionException{
		
		if(ids.size()>0){

			System.out.println("Setting passwords to dummy password");
			for(String id:ids){
				pConn.setPassword(id, dummyPassword);
			}

		}else{
			System.out.println("Id list is null, no need for reset yet");
		}
	}
	
   public void resetPasswords(List<String> ids, String dummyPassword) throws ConnectionException{
		
	   setPartnerConnection();
		if(ids.size()>0){

			System.out.println("Setting passwords to dummy password");
			for(String id:ids){
				try {
				sfdcPartner.setPassword(id, dummyPassword);
				}catch (ConnectionException e) {
					p(e.getMessage() + " " + id);
				}
			}

		}else{
			System.out.println("Id list is null, no need for reset yet");
		}
	}

}

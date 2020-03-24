import java.util.ArrayList;
import java.util.List;

public class TestMethods {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		List<String> test = new ArrayList<String>();
		
		test.add("sadfs");


		System.out.println(inQueryList(test));
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

}

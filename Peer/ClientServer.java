/*=================================================================*/
/*								   */
/*			PEER SERVER - 1 			   */
/*							           */
/*								   */
/*=================================================================*/
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

//PeerServer
class ListenerPort implements Runnable {
	public String connectionMsg;
	int listen_port;
	Socket connection;
	//Boolean flag;                            //declarations
	ServerSocket server;


	public ListenerPort(int listen_port) {
		this.listen_port = listen_port;
		//flag = true;//Initial Idle state
		connectionMsg = "Waiting For PEER Connection";
	}


	public void run() {
		if(listen_port == 9001){
			try{
				server = new ServerSocket(9001);
				while (true) {                                                                       //Listen for Download request
					connection = server.accept();
					System.out.println("Connection Received From " + connection.getInetAddress().getHostName()+" For Download\n");
					ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
					connectionMsg = (String)in.readObject();
					ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
					out.flush();
					String str="";

					try
					{
						FileReader fr = new FileReader(connectionMsg);                 //Reads the filename into Filereader
						BufferedReader br = new BufferedReader(fr);
						String value=new String();
						while((value=br.readLine())!=null)                //Appending the content read from the BufferedReader object until it is null and stores it in str
							str=str+value+"\r\n";
						br.close();
						fr.close();
					} catch(Exception e){
						System.out.println("Cannot Open File");
					}

					out.writeObject(str);
					out.flush();
					in.close();
					connection.close();
				}
			}

			catch(ClassNotFoundException noclass){                                            //To Handle Exception for Data Received in Unsupported or Unknown Formats
				System.err.println("Data Received in Unknown Format");
			}
			catch(IOException ioException){                                                   //To Handle Input-Output Exceptions
				ioException.printStackTrace();
			}
		}

		if(listen_port == 9002){
			try {
				server = new ServerSocket(9002);
				int bytesRead;

				while (true) {                                                                       //Listen for Download request
					connection = server.accept();
					System.out.println("Connection Received From " + connection.getInetAddress().getHostName()+" For Download\n");

					InputStream in = connection.getInputStream();

					DataInputStream clientData = new DataInputStream(in);

					String fileName = clientData.readUTF();
					OutputStream output = new FileOutputStream(fileName);
					long size = clientData.readLong();
					byte[] buffer = new byte[1024];
					while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1)
					{
						output.write(buffer, 0, bytesRead);
						size -= bytesRead;
					}

					output.close();


				}
			}

			catch(IOException ioException){                                                   //To Handle Input-Output Exceptions
				ioException.printStackTrace();
			}
		}
		if(listen_port == 9003){
			try{
				server = new ServerSocket(9003);
				while (true) {                                                                       //Listen for Download request
					connection = server.accept();
					System.out.println("Connection Received From " + connection.getInetAddress().getHostName()+" For Download\n");
					ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
					connectionMsg = (String)in.readObject();
					String filename = connectionMsg.split("$")[0];
					String addedcontent = connectionMsg.split("$")[1];
					ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
					out.flush();
					String str="";

					File f = new File(filename);
					BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
					bw.append(addedcontent);
					bw.close();
					connection.close();
				}
			}

			catch(ClassNotFoundException noclass){                                            //To Handle Exception for Data Received in Unsupported or Unknown Formats
				System.err.println("Data Received in Unknown Format");
			}
			catch(IOException ioException){                                                   //To Handle Input-Output Exceptions
				ioException.printStackTrace();
			}
		}

	}
}

/* PeerServer Class Begin */
public class ClientServer {

	//public String CIS_ip = "10.0.0.13";       //============>IP-address of the CentralIndxServer has to be specified here
	public String ServerIp = "localhost";       //============>IP-address of the CentralIndxServer has to be specified here
	//int peer_id;
	String regmessage,searchfilename;
	ObjectOutputStream out;
	Socket requestSocket;

	public ClientServer() throws IOException {


		try
		{
			FileReader fr = new FileReader("indxip.txt");//read the filename in to filereader object    
			String val1=new String();
			BufferedReader br = new BufferedReader(fr);	
			val1 = br.readLine();
			System.out.println("IndexServer IP is:" + val1);
			ServerIp = val1;
			br.close();
			fr.close();
		} catch(Exception e){
			System.out.println("Could not read indexserver ip from indxip.txt");
		}


		System.out.println("||========================================================================================||");
		System.out.println("||                           PEER-TO-PEER FILE SHARING SYSTEM                             ||");
		System.out.println("||                       ========================================                         ||");
		System.out.println("||                                       MENU:                                            ||");
		System.out.println("||========================================================================================||");

		FileDownload();

		while (true){

			System.out.println("============================================================================================\n");
			System.out.println("Enter The Option :\n==================\n1. Registering the File \n \n2. Searching On CentralIndxServer \n \n3. Downloading From Peer Server \n \n4. Delete from CentralIndxServer \n \n5. Restore File  \n \n6.Create New File  \n \n7.Update File  \n \n8.Read File  \n \n9. Exit\n");
			Scanner in = new Scanner(System.in);
			regmessage = in.nextLine();
			if (regmessage.equals("1")){

				System.out.println("Enter the String in Format: 4Digit id and File Names separated by Space");
				//	Scanner in = new Scanner(System.in);  
				regmessage = in.nextLine();
				//To Collect peer/client id from input string
				String[] val;
				val = regmessage.split(" ");                        //Split the peerid and filename separated with a space
				//int PearPort  = Integer.parseInt(val[0]);

				RegisterWithIServer(val[1],"");

				AttendFileDownloadRequest();

			}		
			if (regmessage.equals("2") || regmessage.equals("4") || regmessage.equals("5")){
				SearchWithIServer(regmessage);                            //Search Method call
			}
			if (regmessage.equals("3")){
				DownloadFromPeerServer(regmessage);                       //Download Method call
			}
			if(regmessage.equals("6")){
				System.out.println("Enter File Name");
				String fileName = in.nextLine();
				searchfilename = fileName+"$2";
				if(!searchInServer(searchfilename)){
					System.out.println("Enter Content to insert");
					String fileContent = in.nextLine();
					System.out.println("Assign File Permission for File Type");
					System.out.println("1 - read only");
					System.out.println("2 - read and write");
					System.out.println("3 - private");
					String filePermission = in.nextLine();
					createFileInSystem(fileName,fileContent,filePermission);
				}else{
					System.out.println("File exist already please use update option");
				}
				//check in local directory unregistered files also

			}
			if (regmessage.equals("7")){
				System.out.println("Enter File Name");
				String fileName = in.nextLine();
				System.out.println("Enter File Name");
				//SearchWithIServer(regmessage);
				searchfilename = fileName+"$2";
				if(!searchInServer(searchfilename)){
					System.out.println("File not found Please create new File");
					String fileContent = in.nextLine();
					createFileInSystem(fileName,fileContent,"");
				}else{
					if(!searchInServer(searchfilename)){
						File f = new File(fileName);
						System.out.println("Enter Content to insert");
						String fileContent = in.nextLine();
						BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
						bw.append(fileContent);
						bw.close();
						updateReplicates(fileName,fileContent);
					}

				}
			}
			if (regmessage.equals("8")){
				System.out.println("Reading File.");
				DownloadFromPeerServer(regmessage);

			}
			if (regmessage.equals("9")){
				System.out.println("Exiting.");
				System.exit(0);
			}

		}
	}

	/* Main Method Begin */
	public static void main(String[] args) throws IOException {

		ClientServer psFrame = new ClientServer();

	}

	public void updateReplicates(String filename, String filecontent){
		try {
			requestSocket = new Socket(ServerIp, 2003);
			System.out.println("\nUpdate Replicated files\n");
			//2. To Get Input and Output streams
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			out.writeObject(filename);
			out.flush();
			ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
			String peerAndIps = (String) in.readObject();
			out.close();
			requestSocket.close();
			String ips[] = peerAndIps.split(";");
			for (String ipList : ips) {
				requestSocket = new Socket(String.valueOf(ips), 9003);
				System.out.println("\nConnected to peerid : "+"\n");
				//2. To Get Input and Output streams
				out = new ObjectOutputStream(requestSocket.getOutputStream());
				out.flush();
				out.writeObject(searchfilename);
				out.flush();
				requestSocket.close();

			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

	}
	public void RegisterWithIServer(String file_name,String filePermission)                             //Register with CentralIndxServer Method
	{
		try {
			//1. Creating a socket to connect to the server
			requestSocket = new Socket(ServerIp, 2001);
			System.out.println("\nConnected to Register on CentralIndxServer on port 2001\n");
			//2. To Get Input and Output streams
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			regmessage = regmessage+" "+filePermission;
			out.writeObject(regmessage);
			System.out.println("Registered Successfully!!\n");
			out.flush();

			ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
			String peerAndIps = (String) in.readObject();
			out.close();
			requestSocket.close();
			String ips[] = peerAndIps.split(";");
			for (String ipList : ips) {
				requestSocket = new Socket(ipList, 9002);

				File myFile = new File(file_name);
				byte[] mybytearray = new byte[(int) myFile.length()];

				FileInputStream fis = new FileInputStream(myFile);
				BufferedInputStream bis = new BufferedInputStream(fis);

				DataInputStream dis = new DataInputStream(bis);
				dis.readFully(mybytearray, 0, mybytearray.length);

				OutputStream os = requestSocket.getOutputStream();

				//Sending file name and file size to the server
				DataOutputStream dos = new DataOutputStream(os);
				dos.writeUTF(myFile.getName());
				dos.writeLong(mybytearray.length);
				dos.write(mybytearray, 0, mybytearray.length);
				dos.flush();
				out.close();
				requestSocket.close();
			}
		}
		catch(UnknownHostException unknownHost){                                             //To Handle Unknown Host Exception
			System.err.println("Cannot Connect to an Unknown Host!");
		}
		catch(IOException ioException){                                                      //To Handle Input-Output Exception
			ioException.printStackTrace();
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		} finally{
			//4: Closing connection
			try{
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}

		public void createFileInSystem(String filename,String filecontent,String filePermission){

		Path fileNameObj = Path.of(filename);
		try {
			Files.writeString(fileNameObj, filecontent);
			regmessage = "1001 "+ filename;
			RegisterWithIServer(filename,filePermission);                          //Register Method call
			AttendFileDownloadRequest();
		} catch (IOException e) {
			System.err.println("Error while file creation");
			throw new RuntimeException(e);
		}

	}

	public void SearchWithIServer(String value)                              //Search on the CentralIndexServer Method
	{
			if(value.equals("2")){
				System.out.println("Enter the File Name to Search");
			}else if(value.equals("4")){
				System.out.println("Enter the File Name to Delete");
			}else{
				System.out.println("Enter the File Name to Revert");
			}
			Scanner in1 = new Scanner(System.in);                                        //Takes Input from the Peer to search the desired file
			searchfilename = in1.nextLine();
			searchfilename = searchfilename.trim()+"$"+value;
			searchInServer(searchfilename);

	}

	public boolean searchInServer(String filename){
		try{

			//1. Creating a socket to connect to the Index server
			requestSocket = new Socket(ServerIp, 2002);
			System.out.println("\nConnected to Search on CentralIndxServer on port 2002\n");
			//2. To Get Input and Output streams
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			out.writeObject(searchfilename);                                            //Writes the Search Filename to the Output Stream
			out.flush();
			ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
			String strVal = (String)in.readObject();
			//  For File Not Found Print Condition
			if(strVal.equals("File is being used by other user please try again later") || strVal.equals("File is Locked You can continue Edit")){
				return false;
			}
			if  (strVal.equals("File Not Found\n")) {

				System.out.println("FILE Does Not Exist !!\n");
				return false;
			}
			else {
				System.out.println( "File:'"+searchfilename+ "' found at peers:"+strVal+"\n");

			}

		}
		catch(UnknownHostException unknownHost){                                           //To Handle Unknown Host Exception
			System.err.println("Cannot Connect to an Unknown Host!");
		}
		catch(IOException ioException){                                                    //To Handle Input-Output Exception
			ioException.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		finally{
			//4: Closing connection
			try{
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
		//check this true
		return true;
	}

	public void writetoFile(String s)
	{
		try
		{  
			//To Append String s to Existing File
			String fname = searchfilename;
			FileWriter fw = new FileWriter(fname,true);
			fw.write(s);                                      //Write to file, the contents
			fw.close();

		} catch(Exception e){
			System.out.println("error while writing to file");
			//	System.out.println("Cannot Open File");     // To Mask Print on Console
		}

	}


	public void DownloadFromPeerServer(String msg)                            //Download Function Method
	{

		System.out.println("Enter Peer id:");                       
		Scanner in1 = new Scanner(System.in);                       //Takes from the user the 4Digit Peer ID as input 
		String peerid = in1.nextLine();

		System.out.println("Enter pear IP Address to download file:");
		String ipadrs = in1.nextLine();
		System.out.println("Enter the File Name to be Downloaded:");      
		searchfilename = in1.nextLine();                              //Takes from user the desired filename to be downloaded

		//int peerid1 = Integer.parseInt(peerid);
		try{

			//1. Creating a socket to connect to the Index server
			requestSocket = new Socket(ipadrs, 9001);
			System.out.println("\nConnected to peerid : "+"\n");
			//2. To Get Input and Output streams
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();			
			out.writeObject(searchfilename);
			out.flush();
			ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
			String strVal = (String)in.readObject();

			if(msg.equals("3")){
				System.out.println( searchfilename+": Downloaded\n");
				writetoFile(strVal);
			}else{
				System.out.println( strVal);
			}

		}
		catch(UnknownHostException unknownHost){                                             //To Handle Unknown Host Exception
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){                                                      //To Handle Input-Output Exception

			System.err.println("FILE not Found at the Following PEER !!");      
			System.err.println("Please enter a valid PEER ID!");      // To Avoid StackTrace Print on Console and Inform User
			DownloadFromPeerServer(msg);                    // Calling Download Function Again to enable user to enter valid Filename and Port Number
			//	ioException.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		finally{
			try{
				//	in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
	public void AttendFileDownloadRequest()                                //FileDownload Request Thread
	{
		Thread dthread = new Thread (new ListenerPort(9001));
		dthread.setName("AttendFileDownloadRequest");
		dthread.start();
	}

	public void FileDownload()
	{
		Thread sthread = new Thread (new ListenerPort(9002));
		sthread.setName("AttendFileDownloadRequest");
		sthread.start();

	}

}

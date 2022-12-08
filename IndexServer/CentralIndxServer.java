/*=========================================================*/
/*       					           */ 
/*	          CENTRAL INDEX SERVER		           */
/*						           */
/*=========================================================*/

//CentralIndxServer Implementation
import java.io.*;

//PeerServer
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


class PortListener implements Runnable {

	ServerSocket server;
	Socket connection;
	BufferedReader br = null;
	Boolean flag;
	public String strVal;
	int port;
    final String DB_URL = "jdbc:oracle:thin:@localhost:1521:orcl";
	final String JDBC_Driver_Class = "oracle.jdbc.driver.OracleDriver";
	final String USER = "system";
	final String PASS = "oracle";


	public PortListener(int port) {
		this.port = port;
		flag = true;//Initial Idle state
		strVal = "Waiting For PEER Connection";
	}

	/* Beginning of Run Method */	
	public void run() {
		if(port==2001)               //Listening on 2001 for registration
		{
			try {
				server = new ServerSocket(2001);
				while (true) {
					connection = server.accept();
					String ipstring = connection.getInetAddress().getHostName();
					System.out.println("Connection Received For Registration");
					ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
					strVal = (String)in.readObject();
					System.out.println(strVal);
					System.out.println("File Registered ");
					//Split string "strVal" using Space as Delimeter store as {peerid ,filename} format;
					String[] var;
					var = strVal.split(" ");
					//int aInt = Integer.parseInt(var[0]);

					/* print substrings */
					try{
						System.out.println("Database connect");
						DriverManager.registerDriver(new oracle.jdbc.OracleDriver());

						Class.forName(JDBC_Driver_Class);
						Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
						Statement stmt = conn.createStatement();
						String sql;

						//for(int x = 1; x < var.length-1 ; x++){
							sql = "INSERT into FileMap values('" + var[1]+ "','" + ipstring + "'," + var[0] + "," +0+ ","+ var[2] +",'',"+0+")";
							ResultSet rs = stmt.executeQuery(sql);
							System.out.println(
									"inserted successfully : " + rs);

						//}
						sql = "select distinct hostipaddress from FileMap where HOSTIPADDRESS NOT IN ('"+ ipstring+"') ORDER BY DBMS_RANDOM.RANDOM  fetch  first 3 rows only";
						ResultSet resultSet = stmt.executeQuery(sql);
//						List<String> peersAndIps = new ArrayList<String>();
						String IpList = "";//+connection.getInetAddress().getHostName()+";";
						while (resultSet.next()) {
							//peersAndIps.add(rs.getString(2));
							IpList += (resultSet.getString(1)+";");
						}
						IpList = IpList.substring(0,IpList.length()-1);
						//IpList = "127.0.0.1";
						sql = "update FileMap set REPLICATE_IPADDRESS ='"+IpList+"' where filename='"+var[1]+"'";
						stmt.executeQuery(sql);
						ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
						out.flush();
						out.writeObject(IpList);                        //Write the List of peer id's to the output stream
						out.flush();
						out.close();
						conn.close();
					}catch (Exception e) {
						System.out.println(e.getMessage());
					}
					in.close();
					connection.close();   				
				}
			} 

			catch(ClassNotFoundException noclass){                                    //To Handle Exceptions for Data Received in Unsupported/Unknown Formats
				System.err.println("Data Received in Unknown Format");
			}
			catch(IOException ioException){                                           //To Handle Input-Output Exceptions
				ioException.printStackTrace();
			} finally {
			}

		}
		if(port==2002)                                //Listening for Search on port 2002
		{
			try {
				server = new ServerSocket(2002);

				while (true) {
					connection = server.accept();
					String senderIp = connection.getInetAddress().getHostName();
					System.out.println("Connection Received From " +senderIp+ " For Search");
					ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
					strVal = (String)in.readObject();
					String action_type = strVal.substring(strVal.length()-1);
					strVal = strVal.substring(0,strVal.length()-2);
					String retval = "";
					//	Peer-id's separated by space are returned for given file
					DriverManager.registerDriver(new oracle.jdbc.OracleDriver());

					Class.forName(JDBC_Driver_Class);
					Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
					Statement stmt = conn.createStatement();
					String sql;
					if(action_type.equals("0")){
						sql = "SELECT * from  FileMap where filename ='"+strVal+"' and deleted = 0 and file_permission in (1,2) union SELECT * from  FileMap where HOSTIPADDRESS ='"+senderIp+"' and deleted = 0";
						//action_type = "7";
					}else{
						sql = "SELECT * from  FileMap where filename ='"+strVal+"' and deleted = 0 and file_permission in (1,2)";
					}
					if(action_type.equals("5")){
						sql = sql.replaceAll("and deleted = 0","");
					}
					ResultSet rs = stmt.executeQuery(sql);
					if(rs.next() == false){
						retval = "File Not Found\n";
					}else{
						if(action_type.equals("2")){
							String peerId = rs.getString("PEERID");
							String HostIPAddress = rs.getString("HOSTIPADDRESS");
							String OtherIPAddress = rs.getString("REPLICATE_IPADDRESS");
							retval = retval + peerId + "("+HostIPAddress+ OtherIPAddress +")\n\r ";
						}else if(action_type.equals("4")){
							sql = "UPDATE FileMap set deleted = 1 where filename ='"+strVal+"'";
							stmt.executeQuery(sql);
							retval = "File Deletion Succesful";
						}else if(action_type.equals("7")){
							sql = "select is_locked from filemap where filename='"+strVal+"' and deleted = 0";
							ResultSet rs1 = stmt.executeQuery(sql);
							rs1.next();
							if(rs1.getInt(1) == 1){
								retval = "File is being used by other user please try again later";
							}else{
								sql = "update filemap set  is_locked = 1 where filename='"+strVal+"' and deleted = 0";
								stmt.executeQuery(sql);
								retval = "File is Locked You can continue Edit";
							}
						}else if(action_type.equals("0")){
//							sql = "UPDATE FileMap set deleted = 1 where filename ='"+strVal+"'";
//							stmt.executeQuery(sql);
							retval = "File Found";
						}else{
							sql = "UPDATE FileMap set deleted = 0 where filename ='"+strVal+"'";
							stmt.executeQuery(sql);
							retval = "File Reverted Succesful";
						}
					}


					System.out.println(retval);
					System.out.println("<=====Searched=====>\n");

					ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
					out.flush();			
					out.writeObject(retval);                        //Write the List of peer id's to the output stream
					out.flush();			
					in.close();
					out.close();
					conn.close();
					connection.close();   				
				}
			} 

			catch(ClassNotFoundException noclass){                                      //To Handle Exceptions for Data Received in Unsupported/Unknown Formats 
				System.err.println("Data Received in Unknown Format");
			}
			catch(IOException ioException){                                             //To Handle Input-Output Exceptions
				ioException.printStackTrace();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			} finally {
			}

		}

		if(port==2003)                                //Listening for Search on port 2003
		{
			try {
				server = new ServerSocket(2003);

				while (true) {
					connection = server.accept();
					String senderIp = connection.getInetAddress().getHostName();
					System.out.println("Connection Received From " +senderIp+ " For Search");
					ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
					strVal = (String)in.readObject();
					//String action_type = strVal.substring(strVal.length()-1);
					//strVal = strVal.substring(0,strVal.length()-2);
					String retval = "";
					//	Peer-id's separated by space are returned for given file
					System.out.println("Database connect");
					DriverManager.registerDriver(new oracle.jdbc.OracleDriver());

					Class.forName(JDBC_Driver_Class);
					Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
					Statement stmt = conn.createStatement();
					String sql;

					sql = "select Replicate_IPADDRESS From filemap where filename='"+strVal+"'";
					ResultSet rs = stmt.executeQuery(sql);
					String IpList = "";
					if(rs.next()){
						IpList = rs.getString(1);
					}


					ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
					out.flush();
					out.writeObject(IpList);                        //Write the List of peer id's to the output stream
					out.flush();
					out.close();
					conn.close();
				}
			}

			catch(ClassNotFoundException noclass){                                      //To Handle Exceptions for Data Received in Unsupported/Unknown Formats
				System.err.println("Data Received in Unknown Format");
			}
			catch(IOException ioException){                                             //To Handle Input-Output Exceptions
				ioException.printStackTrace();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			} finally {
			}

		}

		if(port==2004)                                //Listening for Search on port 2003
		{
			try {
				server = new ServerSocket(2004);

				while (true) {
					connection = server.accept();
					String senderIp = connection.getInetAddress().getHostName();
					ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
					strVal = (String)in.readObject();


					System.out.println("Database connect");
					DriverManager.registerDriver(new oracle.jdbc.OracleDriver());

					Class.forName(JDBC_Driver_Class);
					Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
					Statement stmt = conn.createStatement();
					String sql;
					sql = "update filemap set  is_locked = 0 where filename='"+strVal+"' and deleted = 0";
					stmt.executeQuery(sql);
					conn.close();
				}
			}

			catch(ClassNotFoundException noclass){                                      //To Handle Exceptions for Data Received in Unsupported/Unknown Formats
				System.err.println("Data Received in Unknown Format");
			}
			catch(IOException ioException){                                             //To Handle Input-Output Exceptions
				ioException.printStackTrace();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

	}
}


/*CentralIndxServer Class Begin*/
public class CentralIndxServer {

	public CentralIndxServer() {
		RegisterRequestThread();                           //RegisterRequest and SearchRequest Threads
		SearchRequestThread();
		FileUpdateThread();
		FileUnlockThread();
	}

	public static void main(String[] args) {

		System.out.println("||========================================================================================||");
		System.out.println("||                           PEER-TO-PEER FILE SHARING SYSTEM                             ||");
		System.out.println("||                       ========================================                         ||");
		System.out.println("||========================================================================================||");
		System.out.println("\n <CENTRAL INDEX SERVER IS UP AND RUNNING....>");
		System.out.println(" ============================================\n");


		CentralIndxServer mainFrame = new CentralIndxServer();

	}
	public void RegisterRequestThread()
	{
		Thread rthread = new Thread (new PortListener(2001));                     //Register Request Thread
		rthread.setName("Listen For Register");
		rthread.start();
	}
	public void SearchRequestThread()
	{
		Thread sthread = new Thread (new PortListener(2002));                    //Search Request Thread
		sthread.setName("Listen For Search");
		sthread.start();

	}

	public void FileUpdateThread()
	{
		Thread futhread = new Thread (new PortListener(2003));                    //Search Request Thread
		futhread.setName("Listen For Fileupdate");
		futhread.start();

	}

	public void FileUnlockThread()
	{
		Thread ulthread = new Thread (new PortListener(2004));                    //Search Request Thread
		ulthread.setName("Listen For Fileupdate");
		ulthread.start();

	}
}

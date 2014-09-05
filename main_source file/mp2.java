import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.awt.*;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

//every server will send just his own heart beats to all servers he know
//as long as some server doesn't receive from a server for some time it will set it as not activated
//if he received messages from a new server, it will add him to his list
//if it received message of the not activated one, it will revive the dead server
//when a server first connect to the network it will send information to threshold and threshold will send him information about every other server
//all message are in form of "port_number heart_beats"

public class mp2 {
	
	//declare socket to send and receive message
 static DatagramSocket sendSock;
 static DatagramSocket recvSock;
 static int myPort;//the port number that every one will used to recieve
 static int wholePort;//port number of threshold
 static InetAddress thresholdIP;//the IP of the threshold，门番!
 static int myheartBeats = 1000;
 static a_server[] servers;
 static Thread listener;
 static Thread checker;
 static Thread door;//threshold only
 static Thread watcher;
 static int maxServerNum = 5;
 static int serversWeHave = 0;
 static int connectionSteps = 0;
 static boolean holder = false;
 static a_server waitOne;
 static int sendNum = 1;//the number of information send
 static long myPreTime = 0;
 static int serve_welcome = 0;
 static Logger logger1;
	
	//used to record the information of its peers...
	public static class a_server{
		int port;//the port number
		InetAddress IPAd;//IP address
		long preTime;//last time it is renewed
		int heartBeats;//heartbeat number right now
		boolean alive;//does it still alive
		boolean isServer;
		
		public a_server()
		{
			port = 0;
			IPAd = null;
			preTime = 0;
			heartBeats = 1000;
			alive = false;
			isServer = false;
		}
	}
	
	public static void main(String argv[]) throws IOException
	{
		//log test
		{
			PropertyConfigurator.configure("log4j.properties");
			   
	        logger1 = Logger.getLogger(mp2.class);

	        logger1.setLevel(Level.DEBUG);//

	        
	        //logger.debug("This is debug.");

	        // These requests will be enabled.
	        /*logger.info("This is an info.");
	        logger.warn("This is a warning.");
	        logger.error("This is an error.");
	        logger.fatal("This is a fatal error.");
	        logger.info("I love my little ponies!!!");
	        logger.warn("But among them...");
	        logger.error("Derpy is my favorite");
	        logger.fatal("I used to love fluttershy though...");*/
	        
	        
	     
	        
		}
		//getting started
		System.out.println("Welcome to my network");
		servers = new a_server[maxServerNum];
		//declare place for servers
		for(int i = 0; i < maxServerNum; i++)
		{
			servers[i] = new a_server();
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Please type in your port number:");//type in your port number
		String s = in.readLine();
		myPort = Integer.parseInt(s);
		System.out.println("Are you the thresholder? y/n");//check if you will be the holder of the whole system
		s = in.readLine();
		if(s.equals("y"))
			holder = true;
		if(!holder)//if you are not holder then you should input the information of holder
		{
			System.out.println("please type in threshold's IP address:");//holder's ip
			s = in.readLine();
			thresholdIP = InetAddress.getByName(s);
			System.out.println("Please type in threshold's port number:");//holder's port
			s = in.readLine();
			wholePort = Integer.parseInt(s);
			
		}
		/*else//or you should have the information of all machines if this is the time you come back
		{
			System.out.println("please type in all other processes'(those currently in network) IP addresses and port numbers or type 'pony' to enter:");
			while(true)
			{
				s = in.readLine();
				if(s.equals("pony"))
					break;
				String[] temp_a_p = s.split(" ");
				servers[serversWeHave].IPAd = InetAddress.getByName(temp_a_p[0]);
				servers[serversWeHave].port = Integer.parseInt(temp_a_p[1]);
				servers[serversWeHave].preTime = new Date().getTime();
				servers[serversWeHave].heartBeats = 1000;
				servers[serversWeHave].alive = true;
				serversWeHave++;
			}
				
		}*/
		waitOne = new a_server();
		recvSock = new DatagramSocket(myPort);//declare receive sock
		sendSock = new DatagramSocket();//declare send sock
		
		
		
		
		if(!holder)
		//connect to the threshold and get information of others by: first send message to threshold so it will add this server as a member send information back
		getIntoNetwork();
	
		//this is used to listen to informations
		listener = new Thread(new listen());
		listener.start();
		//start to check heart beat
		checker = null;
		//then get to work
		for(int i = 0; i < maxServerNum; i++)
		{
			if(servers[i].IPAd == null)
				continue;
			//servers[i].preTime = new Date().getTime();
			servers[i].alive = true;
		}
		
		while(true)
		{
			
			
			if(new Date().getTime() - myPreTime <= 500)
				continue;
			myPreTime = new Date().getTime();
			myheartBeats++;
			String message = Integer.toString(myPort) + " " + Integer.toString(myheartBeats) +" dsdad";
			byte[] message_b  = message.getBytes();//the message we want to send
			for(int i = 0; i < maxServerNum; i++)
			{
				if(servers[i].alive || servers[i].isServer)
				{
					DatagramPacket sendData = new DatagramPacket(message_b, 0, message_b.length, servers[i].IPAd, servers[i].port);
					sendSock.send(sendData);
				}
			}
			if(checker == null)//start to check after build connection with others
				{checker = new Thread(new check());
				checker.start();
				
				}
		}
	}
	
	public static class listen implements Runnable
	{
		@Override
		public void run() {
			System.out.println("listener initializing");
			// TODO Auto-generated method stub
			DatagramPacket recvData = new DatagramPacket(new byte[512], 512);
			try {
				//receive message, if the message is from sb we know, then update its information, or we add it to our list
				while(true)
				{
					
					recvSock.receive(recvData);
					byte[] byte_t = new byte[512];
					byte_t = recvData.getData();
					String temp = new String(byte_t);
					String temp1 = temp.substring(0,4);
					
					if(temp1.equals("yooo"))
					{
						
						if(serversWeHave >= maxServerNum)
						{
							System.out.println("sorry we don't accept any more servers...");
							continue;
						}
						if(serve_welcome != 0)
						{
							System.out.println("system is busy...please try later...");
							continue;
						}
						
						
							waitOne = new a_server();
							waitOne.IPAd = recvData.getAddress();
							waitOne.port = recvData.getPort();//for the accepting part, servers will send message from "recvsock"
							//System.out.println("receiving a new server... ( " + recvData.getAddress()+ " " + recvData.getPort() +")" );
							logger1.info("receiving a new server... ( " + recvData.getAddress()+ " " + recvData.getPort() +")");
							if(waitOne.IPAd != null)
								System.out.println("please stand by and don't add any more before this is done...");
							serve_welcome = 1;
							if(serversWeHave == 0)
								{
								
								sendNum = 0;
								}
							door = new Thread(new welcome());
							door.start();
							
							continue;
						
						}
					
					String tem[] = temp.split(" ");
					if(tem[0].equals("joining"))
					{
						//System.out.println(tem[1]);
						if(serve_welcome == 0)
							continue;
						//System.out.println(tem[0].length());
						String temp_string = tem[1];
						int temp_num = Integer.parseInt(temp_string);
						//System.out.println("here!!!!!!!!!!!!!!!!!!!"+temp_num);
						if(temp_num == 100)
							{
								serve_welcome = 0;
								sendNum = 1;
								waitOne = null;
								door.join();
								System.out.println("done adding server!");
							}
						else if(temp_num == sendNum)
							{
							
							System.out.println("another servers' messages been sent to the new server...");
							
							if((sendNum + 1) > serversWeHave)
							{
								sendNum = 0;
								for(int i = 0; i < maxServerNum; i++)
								{
								
								if(servers[i].alive)
									continue;
								servers[i].IPAd = waitOne.IPAd;
								servers[i].port = waitOne.port;
								servers[i].preTime = new Date().getTime();
								servers[i].heartBeats = 1000;
								servers[i].alive = true;
								serversWeHave++;
								int ttttt = serversWeHave + 1;
								//System.out.println("finish adding new server! servers we have: " + serversWeHave);
								logger1.info("finish adding new server! servers we have: " + ttttt);
								break;
								}
							}
							else
								sendNum ++;
							}
						continue;
					}
					temp = tem[1];
					if(tem[1].equals("Derpy"))
						continue;
					int heartBeats_t = Integer.parseInt(temp);//get the heart beat number
					InetAddress address_t = recvData.getAddress();//get sender's address
					temp = tem[0];
					int port_t = Integer.parseInt(temp);//get sender's port number
					boolean exist = false;
					int empty_pos = 0;
					for(int i = 0; i < maxServerNum; i++)
					{
						if(!servers[i].alive)
							empty_pos = i;
						if(servers[i].IPAd != null && servers[i].IPAd.equals(address_t) &&servers[i].port == port_t && (servers[i].alive || servers[i].isServer))
						{
							exist = true;
						
							if(servers[i].isServer && !servers[i].alive)
							{
								logger1.info("the contact server is back!");
								servers[i].alive = true;
								servers[i].heartBeats = 999;
								serversWeHave++;
							}
							if(servers[i].heartBeats < heartBeats_t)
								{
								
								servers[i].heartBeats = heartBeats_t;
								servers[i].preTime = new Date().getTime();
								
								break;
								}
							
							
						}
					}
					if(exist)
						continue;
					//or we add it to list!
					
					servers[empty_pos].heartBeats = heartBeats_t;
					servers[empty_pos].port = port_t;
					servers[empty_pos].IPAd = address_t;
					servers[empty_pos].preTime = new Date().getTime();
					servers[empty_pos].alive = true;
					serversWeHave++;
					int tttt = serversWeHave + 1;
					//System.out.println("adding a new server! servers we have: " + serversWeHave);
					logger1.info("adding new server! servers we have: " + tttt);
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public static class check implements Runnable
	{

		@Override
		public void run() 
		{
			System.out.println("checker initializing");
			while(true)
			{
				long temp = new Date().getTime();
				for(int i = 0; i < maxServerNum; i++)
				{
					if(servers[i].alive)
					{
						
						//System.out.println("he is alive...");
						if(temp - servers[i].preTime > 3000)
						{
							servers[i].alive = false;
							
							
							
						}
						if(servers[i].alive == false)
						{
							serversWeHave--;
							System.out.println("attention:");
							//System.out.println("server " + servers[i].IPAd.toString() + " " + servers[i].port +" has failed!");
							logger1.info("server " + servers[i].IPAd.toString() + " " + servers[i].port +" has failed!");
						}
					}
				}
			// TODO Auto-generated method stub
			
			}
		}
	}
	
	public static class welcome implements Runnable
	{

		@Override
		public void run() {
			System.out.println("start welcoming!");
			
			// TODO Auto-generated method stub
			long start_t = new Date().getTime();
			while(true)
			{
				if((new Date().getTime() - start_t) > 10000)
				{
					//System.out.println("joining server not responding for too long...ending receiving process...");
					logger1.info("joining server not responding for too long...ending receiving process...");
					serve_welcome = 0;
					sendNum = 1;
					waitOne = null;
					return;
				}
				if(serve_welcome == 0||waitOne == null ||waitOne.IPAd == null)
				{
					//there is possibility that after this line is passed then the main thread make waitOne = null...but it doesnt matter even this fail!!!!!!!!!
					break;
				}
				String message = null;
				if(sendNum != 0)
				{
					int sendNum_t = sendNum;
					for(int i = 0; i < maxServerNum; i++)
					{
						if(servers[i].alive)
							sendNum_t--;
						if(sendNum_t == 0)
						{
						message = Integer.toString(sendNum) +" " +  servers[i].IPAd.toString() +" " + Integer.toString(servers[i].port) + " dsasd";
						break;
						}
					}
				}
				else
					{
					message = "0 Derpy really loves my little ponies!";
					
				
					
					}
				byte[] message_b = message.getBytes();
				DatagramPacket temp = new DatagramPacket(message_b, 0, message_b.length, waitOne.IPAd, waitOne.port);
				try {
					sendSock.send(temp);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//System.out.println(waitOne.IPAd.toString() + waitOne.port);
				
			}
			return;
		}
		
	}
	
	public static void getIntoNetwork() throws IOException
	{
		byte[] data = new byte[512];
		String dataS = "yooo";
		data = dataS.getBytes();
		boolean connected = false;
		System.out.println("connecting to server... please stand by...");
		
		DatagramPacket temp1 = new DatagramPacket(data, 0, data.length, thresholdIP, wholePort);
		//System.out.println(thresholdIP +" " + wholePort + " " + new String(data));
		recvSock.send(temp1);
		
		
		//now we throw a thread to watch the condition of connection
		connectionSteps = 0;
		watcher = new Thread(new watch());
		watcher.start();
		
		while(true)
		{
			DatagramPacket temp = new DatagramPacket(new byte[512], 512);
			recvSock.receive(temp);
			
			if(connectionSteps != 1)
			{
				//if we recevied message, then we are connected to threshold
			//System.out.println("connected!");
			connectionSteps = 1;
			}
			data = temp.getData();
			dataS = new String(data);
	
			String[] datas = dataS.split(" ");
			int num = Integer.parseInt(datas[0]);
			//System.out.println(num);
			//System.out.println(serversWehave);
			//System.out.println(num);
			//if all information got
			if(num == 0)
			{
				connectionSteps++;
				//add threshold to the list
				servers[serversWeHave].alive = false;
				servers[serversWeHave].heartBeats = 1000;
				servers[serversWeHave].preTime = new Date().getTime();
				servers[serversWeHave].port = wholePort;
				servers[serversWeHave].IPAd = thresholdIP;
				servers[serversWeHave].isServer = true;
				serversWeHave++;
				dataS = "joining 100 blablabla";
				data = dataS.getBytes();
				temp = new DatagramPacket(data, 0, data.length, thresholdIP, wholePort);
				recvSock.send(temp);//send him the last signal
				System.out.println("getting info done!");
				return;
			}
			if((num - serversWeHave) <= 0)
				{
				continue;
				}
			if((num - serversWeHave) > 1)
			{
				System.out.println("unknown errot!");
				System.exit(0);
			}
			else
			{
			servers[serversWeHave].alive = false;
			servers[serversWeHave].heartBeats = 1000;
			servers[serversWeHave].preTime = new Date().getTime();
			servers[serversWeHave].port = Integer.parseInt(datas[2]);
			InetAddress tta =  InetAddress.getByName(datas[1].substring(1, datas[1].length()));
			servers[serversWeHave].IPAd = tta;
			serversWeHave++;
			System.out.println("another server's data accepted!");
			dataS = "joining " + Integer.toString(serversWeHave) + " dasdada";
			data = dataS.getBytes();
			temp = new DatagramPacket(data, 0, data.length, thresholdIP, wholePort);
			recvSock.send(temp);//send him signal to tell him I received the info
			
			}
			
		}
		
		
	}
	
	public static class watch implements Runnable
	{

		@Override
		public void run() {
			System.out.println("watcher start!");
			// TODO Auto-generated method stub
			long my_time = new Date().getTime();
			while(connectionSteps == 0)//connect to servers
			{
				if(new Date().getTime() - my_time > 5000)
					{
					
					System.out.println("connection out of time!");
					System.exit(0);
					}
			}
			my_time = new Date().getTime();
			while(connectionSteps == 1)//get information about peers
			{
				if(new Date().getTime() - my_time > 15000)
					{
					System.out.println("getting info out of time!");
					System.exit(0);
					}
			}
			
		}
	}

	

}

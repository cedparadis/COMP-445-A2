package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import static java.util.Arrays.asList;

public class Server {

	private Socket socket;
	private String directory;
	private ServerSocket server;
	private BufferedReader in;
	private PrintStream out;


	//method for security purpose, can not access forbidden files

	private boolean unAuthorized(String file) {
		File f = new File(file);
		File currentDir = new File(directory);

		if(f.isDirectory()) {
			return true;
		}
		//check if file is outside current dir
		try {
			if(f.getCanonicalPath().contains(currentDir.getCanonicalPath())) {
				return false; // able to write here
			}
		} catch(IOException e) {
			return true;
		}

		return false;

	}



	// if just "/" returns 200 OK + files in dir
	// else return specific file and its content
	//success : 200 OK
	//not authorized: 401 unauthorized
	// not found: 405 Not Found

	private void handleGet(String file) {



		//responseWriter = new PrintWriter(socket.getOutputStream());

		if(file.equals("/")) {
			File dir = new File(directory);
			File[]list = dir.listFiles();
			StringBuilder response = new StringBuilder();
			StringBuilder directoryBuilder = new StringBuilder("========\n");
			int i = 0;
			while(i < list.length) {
				if(list[i].isFile()) {
					directoryBuilder.append("File: ");
				}
				else if(list[i].isDirectory()) {
					directoryBuilder.append("Directory: ");
				}
				else
					continue;
				directoryBuilder.append(list[i].getName() + "\n");
				i++;
			}
			response.append("HTTP/1.0 200 Ok\n");
			response.append("Date: " + java.time.LocalDate.now() +"\n");
			response.append("Server: localhost\n");
			response.append("\n\n");
			response.append(directoryBuilder.toString());
			System.out.println(directoryBuilder);
			out.println(response.toString());
			out.flush();
			out.close();

		}
		else {
			File f = new File(directory + file);
			try {



				if(!f.exists()) {
					out.println("HTTP/1.0 405 Not Found");
					out.println("Date: " + java.time.LocalDate.now());
					out.println(f);
					out.flush();
					out.close();
				}
				else if(unAuthorized(file)) {
					out.println("HTTP/1.0 403 Forbidden");
					out.println("Date: " + java.time.LocalDate.now());
					out.flush();
					out.close();
				}
				else if(f.isFile()) {
					BufferedReader reader = new BufferedReader(new FileReader(f));
					StringBuilder content = new StringBuilder("Content: \n");
					StringBuilder response = new StringBuilder();
					String line = reader.readLine();
					while(line!=null) {
						content.append(line);
						content.append("\n");
						line = reader.readLine();
					}
					reader.close();
					response.append("HTTP/1.0 200 Ok\n");
					response.append("Date: " + java.time.LocalDate.now() +"\n");
					response.append("Server: localhost\n");
					response.append("\n\n");
					response.append(content.toString());
					out.println(response.toString());
					out.flush();
					out.close();
				}

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch(IOException i) {
				System.out.println(i);
			}
		}



	}

	//handle post request
	//writes data to the file mentionned
	//

	private void handlePost(String file, String body) {

		PrintWriter fileWriter;

		if(unAuthorized(file)) {
			out.println("HTTP/1.0 403 Forbidden");
			out.println("Date: " + java.time.LocalDate.now());
			out.flush();
			out.close();
		}
		else {
			try {
				fileWriter = new PrintWriter(new FileOutputStream(directory+file, false));
				fileWriter.println(body);
				fileWriter.flush();
				fileWriter.close();

				out.println("HTTP/1.0 201 Created");
				out.println("Date: " + java.time.LocalDate.now());
				out.println("Server: localhost");
				out.println("Content-length: " + file.length());
				out.println();
				out.flush();
				out.close();

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch(IOException i) {
				System.out.println(i);
			}
		}
	}

	public Server(int port, String directory, boolean verbose) {

		try {
			this.directory = directory;
			//start server
			server = new ServerSocket(port);
			if(verbose == true) {
				System.out.println("Server started");

				System.out.println("waiting for client...");
			}
				//create a socket specifically for client if initiate connection
				socket = server.accept();
				System.out.println("Client accepted");

			//attributes to read request from client
			String line = "";
			String requestMethod;
			String[] separator;
			String file;

			//read and writing from-to client 
			in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
			out = new PrintStream(socket.getOutputStream());


			//read line and extract info
			line = in.readLine();
			separator = line.split(" ");
			requestMethod = separator[0];
			file = separator[1].replace("HTTP/1.0", "");
			if(verbose == true) {
				System.out.println(requestMethod);
			}
			System.out.println(file);



			if(file.length() > 1) {
				file = file.startsWith("/") ? file.substring(1) : file;
			}

			//check request method

			if(requestMethod.equals("GET")) {
				handleGet(file);
			}
			else if(requestMethod.equals("POST")) {
				//get the content of the body
				while(line != null) {
					if(line.isEmpty()) {
						line = in.readLine();
						break;
					}
					line = in.readLine();

				}
				String body = line;
				System.out.println(line);

				handlePost(file, line);

			}






			System.out.println("Closing connection");
			socket.close();
			in.close();



		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(e);
		}
	}



	//Httpfs class
	public static void main(String args[])
	{
		//Create parser with joptsimple library
		OptionParser parser = new OptionParser();
		parser.acceptsAll(asList("httpfs"), "HTTPFS");
		if(!args[0].equals("httpfs")){
			System.out.println("invalid command ");
			System.exit(0);

		}
		//Verbose option
		OptionSpec<Void> verbose_args = parser.accepts("v");
		boolean verbose = false;

		//port option
		OptionSpec<Integer> port_arg = parser.accepts("p").withRequiredArg().ofType(Integer.class);
		int port = 80;

		//directory option
		OptionSpec<String> dir_arg = parser.accepts("d").withRequiredArg().ofType(String.class);
		String dir = "";

		//parse arguments
		OptionSet opts = parser.parse(args);
		port = opts.valueOf(port_arg);
		verbose = opts.has(verbose_args);
		dir = opts.valueOf(dir_arg);
		Server server = new Server(port, dir, verbose);
	}
}

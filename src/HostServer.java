/* Benjamin Chavez */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.net.ServerSocket;
import java.net.Socket;

/*
 *------------------------------------------------------------------------------
 * AgentWorker Class
 * -----------------------------------------------------------------------------
 */
class AgentWorker extends Thread {
  // Declare our AgentWorker Class Variables
  Socket sock;
  int localPort;
  AgentHolder parentAgentHolder;

  // Declare our AgentWork Class Constructor
  AgentWorker(Socket S, int LP, AgentHolder AH) {
    sock = S;
    localPort = LP;
    parentAgentHolder = AH;
  }

  public void run() {

    PrintStream out = null;
    BufferedReader in = null;

    // Note: in a production level HostServer we would not hardcode the server ***
    String newHost = "localhost";

    int newHostMainPort = 4242;
    String buf = "";
    int newPort;

    Socket clientSock;
    BufferedReader fromHostServer;
    PrintStream toHostServer;

    try {
      out = new PrintStream(sock.getOutputStream());
      in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

      // Begin reading input from client
      String inLine = in.readLine();

      StringBuilder htmlString = new StringBuilder();


      System.out.println();
      System.out.println("Request line: " + inLine);

      // Check if the incoming client request is a `migrate` request
      if (inLine.indexOf("migrate") > -1) {
        clientSock = new Socket(newHost, newHostMainPort);
        fromHostServer = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));

        toHostServer = new PrintStream(clientSock.getOutputStream());
        toHostServer.println("Please host me. Send my port! [State=" + parentAgentHolder.agentState + "]");
        toHostServer.flush();

        // NO idead what this is doing***
        for (;;) {
          buf = fromHostServer.readLine();
          if (buf.indexOf("[Port=") > -1) {
            break;
          }
        }


        String tmpBuf = buf.substring(buf.indexOf("[Port=") + 6, buf.indexOf("]", buf.indexOf("[Port=")));
        newPort = Integer.parseInt(tmpBuf);
        System.out.println("newPort is: " + newPort);

        htmlString.append(AgentListener.sendHtmlHeader(newPort, newHost, inLine));
        htmlString.append("<h3>View the source of this page to see how the client is informed of the new location.</h3> \n");
        htmlString.append(AgentListener.sendHtmlSubmit());

        System.out.println("Killing parent listening loop.");

        // Whye isn't this also called servSock?***
        ServerSocket ss = parentAgentHolder.sock;
        ss.close();
      } else if (inLine.indexOf("person") > -1) {

        parentAgentHolder.agentState++;

        htmlString.append(AgentListener.sendHtmlHeader(localPort, newHost, inLine));
        htmlString.append("<h3>We are having a conversation with state   " + parentAgentHolder.agentState + "</h3>\n");
        htmlString.append(AgentListener.sendHtmlSubmit());

      } else {
        htmlString.append(AgentListener.sendHtmlHeader(localPort, newHost, inLine));
        htmlString.append("You have not entered a valid request!\n");
        htmlString.append(AgentListener.sendHtmlSubmit());
      }

      AgentListener.sendHtmlToStream(htmlString.toString(), out);
      sock.close();

    } catch (IOException e) {
      System.out.println(e);
    }
  }
}

/*
 * -----------------------------------------------------------------------------
 * AgentHolder Class
 * -----------------------------------------------------------------------------
 */
class AgentHolder {
  ServerSocket sock;
  int agentState;

  AgentHolder(ServerSocket s) {
    sock = s;
  }
}



/*
 * -----------------------------------------------------------------------------
 * AgentListener Class
 * -----------------------------------------------------------------------------
 */
class AgentListener extends Thread {
  // Declare our AgentListener Class Variables
  Socket sock;
  int localPort;

  // Declare our AgentListener Class Constructor
  AgentListener(Socket ALSocket, int P) {
    sock = ALSocket;
    localPort = P;
  }

  /*
   * Define and set our inital agent state. Note, for the sake of this program,
   * our agentState is very simple. However, this state could be expanded to
   * become
   * quite complex in a production level HostServer.
   */
  int agentState = 0;

  // Our run() method begins when we create a new instance of our AgentListener
  // class
  public void run() {
    BufferedReader in = null;
    PrintStream out = null;
    String newHost = "localhost";
    // System.out.println("Running AgentListener Thread...");
    System.out.println("In AgentListener Thread");

    try {
      // Declare our String buffer to store user input
      String buf;

      // Declare our input and output streams to ***
      out = new PrintStream(sock.getOutputStream());
      in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

      buf = in.readLine();    // Read user input and store it in our String buffer


      System.out.println("INDEX OF BUF: " + buf.indexOf("[State="));
      System.out.println("BUFFER: " + buf);
      System.out.println("");
      if (buf != null && buf.indexOf("[State=") > -1) {

        String tmpBuf = buf.substring(buf.indexOf("[State=") + 7, buf.indexOf("]", buf.indexOf("[State=")));
        agentState = Integer.parseInt(tmpBuf);

        // String tmpBuf = buf.substring(buf.indexOf("[State=") + 7, buf.indexOf("]", buf.indexOf("[State=")));
        // agentState = Integer.parseInt(tmpBuf);
        // Printour agentState to the user console***
        System.out.println("agentState is: " + agentState);
      }

      System.out.println(buf);
      // Define our stringBuilder to hold our HTML Response.
      // Note: the use of the StringBuilder class, which is mutable while the
      // String class is immutable.
      StringBuilder htmlResponse = new StringBuilder();

      // Begin building our HTML response.
      htmlResponse.append(sendHtmlHeader(localPort, newHost, buf));
      htmlResponse.append("Now in Agent Looper starting Agent Listening Loop\n<br />\n");
      htmlResponse.append("[Port=" + localPort + "]<br/>\n");
      htmlResponse.append(sendHtmlSubmit());
      // Call our sendHtmlToStream() Method to ***
      sendHtmlToStream(htmlResponse.toString(), out);
      // ***
      ServerSocket servSock = new ServerSocket(localPort, 2);
      AgentHolder agentHold = new AgentHolder(servSock);
      agentHold.agentState = agentState;

      // Enter infinite Loop: Blocked waiting for connections
      while (true) {
        sock = servSock.accept();
        System.out.println("Received a connection to agent at port: " + localPort);

        // Once a connection is accepted, we spawn a new worker/AgentWorker
        // thread to handle the connections
        new AgentWorker(sock, localPort, agentHold).start();
      }

    } catch (IOException e) {
      // We catch an exception when an error occurs or simply when a mobile agent
      // is migrated to a new port ***
      String eMsg = "Either the connection failed, or just killed the listener loop for agent at port: ";
      System.out.println(eMsg + localPort);
      System.out.println(e);
    }

  }


  // Build our HTML Header
  static String sendHtmlHeader(int localPort, String newHost, String inLine) {
    StringBuilder htmlString = new StringBuilder();

    // Append each line of our Html
    htmlString.append("<html><head> </head><body>\n");
    htmlString.append("<h2>This is for submission to PORT " + localPort + " on " + newHost + "</h2>\n");
    htmlString.append("<h3>You sent: " + inLine + "</h3>");
    htmlString.append("\n<form method=\"GET\" action=\"http://" + newHost + ":" + localPort + "\">\n");
    htmlString.append("Enter text or <i>migrate</i>:");
    htmlString.append("\n<input type=\"text\" name=\"person\" size=\"20\" value=\"YourTextInput\" /> <p>\n");

    return htmlString.toString();
  }

  //***
  static String sendHtmlSubmit() {
    return "<input type=\"submit\" value=\"Submit\"" + "</p>\n</form></body></html>\n";
  }

  // Send the HTML to our ouput stream
  static void sendHtmlToStream(String html, PrintStream out) {
    // Send the HTML headers line by line to our output stream
    out.println("HTTP/1.1 200 OK");
    out.println("Content-Length: " + html.length());
    out.println("Content-Type: text/html");
    out.println("");

    // Send our html body to the output stream
    out.println(html);

  }
}



/*
 *------------------------------------------------------------------------------
 * HostServer
 * -----------------------------------------------------------------------------
 */
public class HostServer {
  //
  public static int nextPort = 3000; // ***

  public static void main(String[] args) throws IOException {
    int qLen = 6; // Define the max number of requests that can be handled at a time
    int port = 4242; // Define the port where our HostServer will be located
    Socket sock; // Allocate our socket object

    ServerSocket servSock = new ServerSocket(port, qLen); // New ServerSocket started with our previously defined params
    System.out.println("Master Receiver started on port 4242");
    System.out.println("Connect up to 3 browsers using \"http:\\\\localhost:4242\"\n");

    // Enter into our infinite Loop to begin listening for connections from mobile
    // agents
    while (true) {
      // Upon receiving a new connection request, we increment our port number to serve the
      // Mobile Agent on a new port
      nextPort = nextPort + 1;

      // When a connection is accepted we put it into a `sock`
      sock = servSock.accept();
      System.out.println("Starting AgentListener on port: " + nextPort);

      // Once the connection is accepted, we spawn a new worker/AgentListener
      // thread to handle the connections
      new AgentListener(sock, nextPort).start();
    }

  }
}

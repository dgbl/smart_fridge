package fridge;

import store.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import thrift.*;

/**
 *
 * @author DNS
 */
public class Zentrale {

    public final static int udpPort = 9999;
    public final static int httpPort = 8888;
    public final static int tPort = 7777;
    String serverHostName = "localhost";
    String url = "http://localhost:" + httpPort;
    private DatagramSocket serverSocket = null;
    private static final WareList wList = new WareList();
    private static int MAX = 10;
    ServerSocket httpServerSocket = null;
//    private final InetAddress SensorIP = InetAddress.getByName("141.100.42.140");
    InetAddress IPAddress = null;
    long start;
            

    public static void main(String[] args) {
        try {
            Zentrale zentrale = new Zentrale();
            zentrale.RunServer();
            Store s = new Store();

        } catch (SocketException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public Zentrale() throws SocketException, IOException {

        InetAddress IPAddress = InetAddress.getByName(serverHostName);
        serverSocket = new DatagramSocket(udpPort);
        httpServerSocket = new ServerSocket(httpPort);
        System.out.println("---ZENTRALE: IPAdress: " + IPAddress + " | Datagram Socket Port: " + udpPort + "\n");
        initWares();
        displayWares();
    }

    public void RunServer() {
        Thread udpThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    udpServer(udpPort);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread httpThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    httpServer();
                } catch (IOException e) {
                    e.printStackTrace();
                };
            }
        });

        Thread thriftThread = new Thread(new Runnable() {
            @Override
            public void run() {
                thriftServer(new Order.Processor<>(new StoreHandler()));
            }
        });

        udpThread.start();
        httpThread.start();
        thriftThread.start();
    }

//Kommunikation mit Sensor (Client)
    private void udpServer(int port) throws SocketException, IOException {

        while (true) {
            System.out.println("---Zentrale: Sensor Port: " + port);

            //Empfangen
            byte[] receiveData = new byte[1000];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            try {

                String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                IPAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                System.out.println("---Zentrale: RECEIVED from " + IPAddress + " : " + sentence);

                //Vergleich Empfangen == Warenliste
                String sentenceCmp = sentence.trim();
                String message = "";

                for (int i = 0; i <= wList.sizeOfWare() - 1; i++) {
                    if (sentenceCmp.equals(wList.getWare(i).getBezeichnung())) {
                        if (wList.getWare(i).getAnzahl() == 0) {
                            System.out.println(wList.getWare(i).getBezeichnung() + " ist leer");
                            //Bestellen, sobald Warenbestand 0 erreicht                                                                                 
                            orderWare(wList.getWare(i).getBezeichnung());
                            message = "Ware wurde bestellt";
                            sendMessage(message, clientPort);
                            //Nachfüllen
                            refill(i, clientPort);
                            message = "Ware wurde eigelagert";
                            sendMessage(message, clientPort);
                        }

                        //Simulierte Warenabnahme
                        takeWares(i);

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }

                        //Senden an Client 
                        message = Integer.toString(wList.getWare(i).getAnzahl());
                        sendMessage(message, clientPort);

                    } 
                }

            } catch (IOException e) {
                System.out.println("Could not receive datagram.\n" + e.getLocalizedMessage());
            } //Ausgabe
            displayWares();
        }

    }

    private void sendMessage(String message, int clientPort) throws IOException {
        DatagramSocket socket = new DatagramSocket();

        byte[] sendData = new byte[message.length() * 8];
        sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, clientPort);
        socket.send(sendPacket);
//        socket.close();
    }

    public void httpServer() throws IOException {
        DataOutputStream outToClient = null;
        System.out.println("---Zentrale: Webserver Port: " + httpPort);

        while (true) {
            Socket client = httpServerSocket.accept();
            outToClient = new DataOutputStream(client.getOutputStream());

            String CRLF = "\r\n";
            InputStream instream = client.getInputStream();
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(instream));

            //GET /index.html HTTP/1.1
            String requestLine = inFromClient.readLine();

            //Zerlegen der Anfrage in einzelne Elemente und überspringt "GET" --> Filename
            StringTokenizer tokens = new StringTokenizer(requestLine);
            String methode = tokens.nextToken();
            String line = "";

            if ("GET".equals(methode)) {
                out(outToClient, "");

            } else if ("POST".equals(methode)) {
                int contentLength = -1;
                while (true) {
                    line = inFromClient.readLine();
                    final String contentLengthStr = "Content-Length: ";
                    if (line.startsWith(contentLengthStr)) {
                        contentLength = Integer.parseInt(line.substring(contentLengthStr.length()));
                    }
                    if (line.length() == 0) {
                        break;
                    }
                }

                //Post body filtern und umwandeln
                int cL = Integer.valueOf(contentLength);
                char[] buffer = new char[cL];
                String postData = "";
                inFromClient.read(buffer, 0, cL);
                postData = new String(buffer, 0, buffer.length);
                String[] postDataSplited = postData.split("=");

                if ("Invoice".equals(postDataSplited[1])) {
                    String invoice = "<th>" + Double.toString(Store.invoice()) + " EUR</th>";
                    out(outToClient, invoice);
                } else {
                    System.out.println("---Webserver: Bestellte Ware: " + postDataSplited[0]);
                    orderWare(postDataSplited[0]);

                    out(outToClient, "");
                    fill(postDataSplited[0]);
                }
            }

        }
    }

    public void out(DataOutputStream outToClient, String invoice) {
        try {
            outToClient.writeBytes("HTTP/1.1 200 OK\r\n");
            outToClient.writeBytes("Content-Type: text/html\r\n\r\n");
            outToClient.writeBytes(makeFile(invoice));
            outToClient.flush();
            outToClient.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    //Initialisiert den Warenbestand im Kuehlschrank
    public static void initWares() {

        Ware w1 = new Ware();
        w1.setBezeichnung("Apfel");
        w1.setAnzahl(10);
        w1.setStatus("<font color=\"green\">Gelagert</font>");
        wList.addWare(w1);

        Ware w2 = new Ware();
        w2.setBezeichnung("Banane");
        w2.setAnzahl(10);
        w2.setStatus("<font color=\"green\">Gelagert</font>");
        wList.addWare(w2);

        Ware w3 = new Ware();
        w3.setBezeichnung("Birne");
        w3.setAnzahl(10);
        w3.setStatus("<font color=\"green\">Gelagert</font>");
        wList.addWare(w3);

        Ware w4 = new Ware();
        w4.setBezeichnung("Mango");
        w4.setAnzahl(10);
        w4.setStatus("<font color=\"green\">Gelagert</font>");
        wList.addWare(w4);

        Ware w5 = new Ware();
        w5.setBezeichnung("Durian");
        w5.setAnzahl(10);
        w5.setStatus("<font color=\"green\">Gelagert</font>");
        wList.addWare(w5);
    }

    //Entnahme der Ware
    public static synchronized void takeWares(int ware) {
        wList.getWare(ware).entnahme();
        if (wList.getWare(ware).getAnzahl() <= 0) {
            wList.getWare(ware).setAnzahl(0);
            wList.getWare(ware).setStatus("<font color=\"red\">Leer</font>");
        }
        System.out.println(wList.getWare(ware).getBezeichnung() + " entnommen");

    }

    //Anzeigen des Kuehlschrnakinhalts
    public static void displayWares() {
        System.out.println("Fuellbestand: ");
        for (int i = 0; i <= wList.sizeOfWare() - 1; i++) {
            System.out.println(wList.getWare(i).getBezeichnung() + ": " + wList.getWare(i).getAnzahl());
        }

    }

    //Simuliert Nachfüllen der Ware
    public void refill(int ware, int clientPort) {
        try {
            wList.getWare(ware).setAnzahl(MAX);
            wList.getWare(ware).setStatus("<font color=\"green\">Gelagert</font>");
            System.out.println(wList.getWare(ware).getAnzahl() + " ME " + wList.getWare(ware).getBezeichnung() + " hinzugefuegt");
            String message = Integer.toString(wList.getWare(ware).getAnzahl());
            sendMessage(message, clientPort);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public void fill(String name) {
        try {
            for (int i = 0; i <= wList.sizeOfWare() - 1; i++) {
                if (name.equals(wList.getWare(i).getBezeichnung())) {
                    wList.getWare(i).setAnzahl(MAX);
                    wList.getWare(i).setStatus("<font color=\"green\">Gelagert</font>");
                }
            }
            String message = "Ware wurde bestellt und wird eigelagert...";
            System.out.println("---Zentrale: Ware wurde eingelagert");
            sendMessage(message, udpPort);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    //Bestellung und Gesamtrechnung
    public synchronized void orderWare(String name) {
        int amount;
start = System.currentTimeMillis();
        for (int i = 0; i <= wList.sizeOfWare() - 1; i++) {
            if (name.equals(wList.getWare(i).getBezeichnung())) {
                if (wList.getWare(i).getAnzahl() == MAX) {
                    System.out.println("Maximalbestand - Bestellung nicht moeglich!");
                } else {
                    try {
                        amount = MAX - wList.getWare(i).getAnzahl();

                        Store.order(amount, name);
                        wList.getWare(i).setStatus("<font color=\"blue\">Bestellt</font>");
                        System.out.println(amount + " ME " + name + " wurden bestellt");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } long end = System.currentTimeMillis();          
        long diff = end - start;
            System.out.println("+++++++++++++++++++++++++Dauer demand bis order " + diff + "ms");
    }

    public static int parseStringToInt(String value, int beginIndex) {
        int result = 0;
        try {
            String stringValue = value.substring(beginIndex);
            result = Integer.parseInt(stringValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public String makeFile(String invoice) {
        Date date = new Date();
        String html = "<!DOCTYPE html>\n"
                + "<html>\n"
                + "	<head>\n"
                + "		<meta charset='UTF-8'>\n"
                + "			<title>Kuehlschrank</title>\n"
                + "		</head>\n"
                + "		<body><h1 style=color:red;font-family:arial>Kuehlschrank Bestand:</h1>" + date + "\n"
                + "			<form action= \"" + url + "\" method=\"post\">\n"
                + "			<table border=1>\n"
                + "				<thead>\n"
                + "					<tr>\n"
                + "						<th>Bezeichnung</th>\n"
                + "						<th>Anzahl</th>\n"
                + "					</tr>\n"
                + "				</thead>\n"
                + "				<tbody>\n"
                + "					<tr>\n"
                + "						<td>" + wList.getWare(0).getBezeichnung() + "</td>\n"
                + "						<th>" + wList.getWare(0).getAnzahl() + "</th>\n"
                + "                                             <th><input type=\"submit\" name=\"Apfel\" value=\"Order\" /></th>\n"
                + "						<th>" + wList.getWare(0).getStatus() + "</th>\n"
                + "					</tr>\n"
                + "					<tr>\n"
                + "						<td>" + wList.getWare(1).getBezeichnung() + "</td>\n"
                + "						<th>" + wList.getWare(1).getAnzahl() + "</th>\n"
                + "                                             <th><input type=\"submit\" name=\"Banane\" value=\"Order\" /></th>\n"
                + "						<th>" + wList.getWare(1).getStatus() + "</th>\n"
                + "					</tr>\n"
                + "					<tr>\n"
                + "						<td>" + wList.getWare(2).getBezeichnung() + "</td>\n"
                + "						<th>" + wList.getWare(2).getAnzahl() + "</th>\n"
                + "                                             <th><input type=\"submit\" name=\"Birne\" value=\"Order\" /></th>\n"
                + "						<th>" + wList.getWare(2).getStatus() + "</th>\n"
                + "					</tr>\n"
                + "					<tr>\n"
                + "						<td>" + wList.getWare(3).getBezeichnung() + "</td>\n"
                + "						<th>" + wList.getWare(3).getAnzahl() + "</th>\n"
                + "                                             <th><input type=\"submit\" name=\"Mango\" value=\"Order\" /></th>\n"
                + "						<th>" + wList.getWare(3).getStatus() + "</th>\n"
                + "					</tr>\n"
                + "					<tr>\n"
                + "						<td>" + wList.getWare(4).getBezeichnung() + "</td>\n"
                + "						<th>" + wList.getWare(4).getAnzahl() + "</th>\n"
                + "                                             <th><input type=\"submit\" name=\"Durian\" value=\"Order\" /></th>\n"
                + "						<th>" + wList.getWare(4).getStatus() + "</th>\n"
                + "					</tr>\n"
                + "					<tr>\n"
                + "						<td></td>\n"
                + "						<td></td>\n"
                + "                                             <th><input type=\"submit\" name=\"Rechnung\" value=\"Invoice\" /></th>\n"
                + invoice
                + "					</tr>\n"
                + "				</tbody>\n"
                + "			</table>\n"
                + "			</form>\n"
                + "		</body>\n"
                + "	</html>";

        System.out.println("---Webserver: HTML erstellt");

        return html;
    }

    public static void thriftServer(Order.Processor<StoreHandler> processor) {
        try {
            TServerTransport serverTransport = new TServerSocket(tPort);
            TServer server = new TSimpleServer(new TServer.Args(serverTransport).processor(processor));
            System.out.println("---Zentrale: Verbindung zu Store Port: " + tPort);
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

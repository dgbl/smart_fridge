package fridge;

import java.io.*;
import java.net.*;

/**
 *
 * @author DNS
 */
class Sensor {

    public static void main(String args[]) throws Exception {

        boolean sensorWhile = true;
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("IP Adresse der Zentrale angeben");
        String serverHostName = inFromUser.readLine().trim();

        int serverPort = 9999;
        String receivedFromServer;
        String checkRefill = "Bestand wird gemessen";

        
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName(serverHostName);

        System.out.println("---Sensor: IPAdress: " + IPAddress + " | ServerPort: " + serverPort + "\n");

        while (sensorWhile) {
            //Eingabe
            System.out.println("Ware f. Sensor eingeben: ");
            String sentence = inFromUser.readLine().trim();
            //Eingabe pruefen
            if (!(sentence.equals("Apfel") || sentence.equals("Birne") || sentence.equals("Banane") || sentence.equals("Mango") || sentence.equals("Durian"))) {
                System.out.println("Ware nicht im Kuehlschrank!");
            } else {
                while (true) {
                    System.out.println("---Sensor: " + checkRefill);
                    checkRefill = "Bestand wird gemessen";
                    byte[] sendData = new byte[sentence.length() * 8];
                    byte[] receiveData = new byte[sentence.length() * 8];

                    //Senden an Zentrale
                    sendData = sentence.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
                    clientSocket.send(sendPacket);
                    //Empfangen
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    clientSocket.receive(receivePacket);
                    receivedFromServer = new String(receivePacket.getData(), 0, receivePacket.getLength());

                    //Ausgabe des Fuellbestands
                    System.out.println("Fuellbestand " + receivedFromServer);
                    if (receivedFromServer.equals("0")) {
                        //entnahmeWhile = false;
                        checkRefill = "Ware wurde hinzugefügt";
                    }
                }
            }
        }
        clientSocket.close();

    }
}

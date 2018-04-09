package store;

import java.io.IOException;
import thrift.Article;
import thrift.Order;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.eclipse.paho.client.mqttv3.MqttException;
import mqtt.*;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import producer.Serializer;
import static store.StoreStock.aList;

/**
 *
 * @author DNS
 */
public class Store implements MqttCallback {

    public static final int PORT = 7777;
    public static final String HOST = "localhost";

    StoreStock stock = new StoreStock();
    public static ArrayList<Article> buyList = new ArrayList<Article>();
    public static ArrayList<Article> offerList = new ArrayList<Article>();
    Serializer serializer = new Serializer();

    public Store() {

        initStock();
        subscibe("order");
        subscibe("offer");

    }

    public void initStock() {
        Article a1 = new Article();
        a1.name = "Apfel";
        a1.amount = 40;
        a1.price = 1;
        aList.add(a1);

        Article a2 = new Article();
        a2.name = "Banane";
        a2.amount = 40;
        a2.price = 2;
        aList.add(a2);

        Article a3 = new Article();
        a3.name = "Birne";
        a3.amount = 40;
        a3.price = 3;
        aList.add(a3);

        Article a4 = new Article();
        a4.name = "Mango";
        a4.amount = 40;
        a4.price = 4;
        aList.add(a4);

        Article a5 = new Article();
        a5.name = "Durian";
        a5.amount = 40;
        a5.price = 5;
        aList.add(a5);

    }

    //Führt die Bestellung aus und liefert Menge und Preis des bestellten Artikels
    public static void order(int amount, String name) {
        System.out.println("---Store: Bestellung eingegangen");
        System.out.println("----------------------------------------------------------");
        Article article = new Article();
        article.setName(name);
        article.setAmount(amount);
        for (int i = 0; i < StoreStock.aList.size(); i++) {
            if (StoreStock.aList.get(i).getName().equals(article.name)) {

                article.setPrice(StoreStock.aList.get(i).getPrice());
                System.out.println("---Store: Preis: " + StoreStock.aList.get(i).getPrice());
            }
        }

        try {
            TTransport transport;
            transport = new TSocket(HOST, PORT);
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Order.Client client = new Order.Client(protocol);

            System.out.println("Gesamtbetrag: " + client.calcPrice(article, amount) + " EUR");
            buyList.add(article);
            invoice();
            buy(article.getName(), article.getAmount());
            transport.close();
        } catch (TException x) {
            x.printStackTrace();
        }

    }

    public static double invoice() {

        double totalOrder = 0;
        double total = 0;
        System.out.println("Bestellungen:    " + buyList.size());
        for (int i = 0; i <= buyList.size() - 1; i++) {
            totalOrder = (buyList.get(i).getAmount() * buyList.get(i).getPrice());
            System.out.println(buyList.get(i).getName() + " " + buyList.get(i).getAmount() + " Stk:  " + buyList.get(i).getAmount() * buyList.get(i).getPrice() + " EUR");
            total += totalOrder;
        }
        System.out.println("Rechnungsbetrag: " + total + " EUR");
        System.out.println("----------------------------------------------------------");

        return total;
    }

    public static void buy(String name, int amount) {
        System.out.println("---Store: Artikel gekauft");
        for (int i = 0; i < StoreStock.aList.size(); i++) {
            System.out.println("---Store: Artikel " + StoreStock.aList.get(i).getName() + " veruegbar: " + StoreStock.aList.get(i).getAmount() + " zu " + StoreStock.aList.get(i).getPrice() + " EUR");
            if (StoreStock.aList.get(i).getName().equals(name)) {
                System.out.println("---Store: Artikel " + StoreStock.aList.get(i).getName() + " veruegbar: " + StoreStock.aList.get(i).getAmount() + " zu " + StoreStock.aList.get(i).getPrice() + " EUR");
                StoreStock.aList.get(i).setAmount(StoreStock.aList.get(i).getAmount() - amount);

                if (StoreStock.aList.get(i).getAmount() <= 0) {
                    System.out.println("---Store: Artikel nicht mehr veruegbar");
                    publish("demand", StoreStock.aList.get(i));
                    System.out.println("---Store: Nachfrage an Produzenten");
                    StoreStock.aList.get(i).setAmount(50);
                }
            }

        }
    }

    public static void publish(String topic, Article article) {
        PubParameters pubParameters = new PubParameters();

        // Get the CLI parameters.
        pubParameters = pubParameters.getInstance();

        // Create the broker string from command line arguments.
        String broker
                = pubParameters.getBrokerProtocol() + "://"
                + pubParameters.getBrokerAddress() + ":"
                + pubParameters.getBrokerPort();

        pubParameters.setTopic(topic);

        // Create some MQTT connection options.
        MqttConnectOptions mqttConnectOpts = new MqttConnectOptions();
        mqttConnectOpts.setCleanSession(true);

        try {

            MqttClient client = new MqttClient(broker, MqttClient.generateClientId());

            // Connect to the MQTT broker using the connection options.
            client.connect(mqttConnectOpts);
            System.out.println("Connected to MQTT broker: " + client.getServerURI());

            pubParameters.setMessage(article);

            // Create the mqttMessage and set a quality-of-service parameter.
            MqttMessage mqttMessage = new MqttMessage(Serializer.serialize(article));
            mqttMessage.setQos(2);

            // Publish the mqttMessage.
            client.publish(pubParameters.getTopic(), mqttMessage);
            System.out.println("Published topic: " + pubParameters.getTopic());
            System.out.println("Published article: " + pubParameters.getMessage().getName());

            // Disconnect from the MQTT broker.
//            client.disconnect();
//            System.out.println("Disconnected from MQTT broker.");
        } catch (MqttException e) {
            System.out.println("An error occurred: " + e.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(Store.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void subscibe(String topic) {
        SubParameters subParameters = new SubParameters();

        // Get the CLI parameters.
        subParameters = SubParameters.getInstance();

        // Create the broker string from command line arguments.
        String broker
                = subParameters.getBrokerProtocol() + "://"
                + subParameters.getBrokerAddress() + ":"
                + subParameters.getBrokerPort();

        subParameters.setTopic(topic);

        try {
            MqttClient client = new MqttClient(broker, MqttClient.generateClientId());
//            client.setCallback(new MqttHandler());
            client.setCallback(this);

            // Connect to the MQTT broker.
            client.connect();
            System.out.println("Connected to MQTT broker: " + client.getServerURI());

            // Subscribe to a topic.
            client.subscribe(subParameters.getTopic());
            System.out.println("Subscribed to topic: " + client.getTopic(subParameters.getTopic()));

//            client.disconnect();
        } catch (MqttException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }

    }

    @Override
    public void connectionLost(Throwable thrwbl) {
        System.out.println("Connection to MQTT broker lost!");
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {

        try {
            Article messageArticle = (Article) serializer.deserialize(mqttMessage.getPayload());
//            String message = new String(mqttMessage.getPayload());
//            System.out.println("Message: " + message);

            System.out.println("Article received: " + messageArticle.getName());
            System.out.println("Topic received: " + topic);

            if (topic.equals("order")) {
                System.out.println("---Store: Subcribe Bestellung");
                for (int i = 0; i < StoreStock.aList.size(); i++) {
                    if (StoreStock.aList.get(i).getName().equals(messageArticle.getName())) {
                        StoreStock.aList.get(i).setPrice(messageArticle.getPrice());
                        System.out.println("Neuer Preis: " + StoreStock.aList.get(i).getPrice());
                    }
                }
            }
            if (topic.equals("offer")) {
                System.out.println("---Strore: Subscribe Angebot");
                for (int i = 0; i < StoreStock.aList.size(); i++) {
                    if (StoreStock.aList.get(i).equals(messageArticle.getName())) {
                        offerList.add(messageArticle);
                        for (int j = 0; j < offerList.size(); j++) {
                            if (offerList.get(j).getName().equals(messageArticle.getName())) {
                                if (offerList.get(j).getPrice() <= messageArticle.getPrice()) {
                                    System.out.println("Gutes Angebot: " + messageArticle.getPrice());
                                    offerList.get(j).setPrice(messageArticle.getPrice());
                                    System.out.println("---Store: Publish Nachfrage");
                                    publish("demand", messageArticle);
                                } else {
                                    System.out.println("Schlechtes Angebot: " + messageArticle.getPrice());
                                }
                            }
                        }

                    }
                }

            } else {
                System.out.println("Topic: " + topic);
            }
        } catch (IOException ex) {
            Logger.getLogger(Store.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Store.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {
        try {
            System.out.println("Delivery completed: " + imdt.getMessage());
        } catch (MqttException e) {
            System.out.println("Failed to get delivery token message: " + e.getMessage());
        }
    }

//    public static void main(String[] args) {
//        Store s = new Store();
//        s.subscibe("order");
//        s.subscibe("offer");
//    }
}

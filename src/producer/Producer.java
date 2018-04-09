package producer;

import java.io.IOException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import thrift.Article;

public class Producer implements MqttCallback {

    private Timer timer = new Timer();

    Serializer serializer = new Serializer();

    public Producer(String name, int price1, int price2, int price3, int price4, int price5) {
        System.out.println("Produzent " + name);
        System.out.println("-------------------------------------");
        ProducerStock ps = new ProducerStock();
        ps.initStock(price1, price2, price3, price4, price5);
        ps.displayOffer();
        timer.scheduleAtFixedRate(timerTask, 0, 10000);
    }

    public void publish(String topic, Article article) {
        mqtt.PubParameters pubParameters = new mqtt.PubParameters();

        //CLI Parameter
        pubParameters = pubParameters.getInstance();
        String broker
                = pubParameters.getBrokerProtocol() + "://"
                + pubParameters.getBrokerAddress() + ":"
                + pubParameters.getBrokerPort();

        pubParameters.setTopic(topic);
        MqttConnectOptions mqttConnectOpts = new MqttConnectOptions();
        mqttConnectOpts.setCleanSession(true);

        try {

            MqttClient client = new MqttClient(broker, MqttClient.generateClientId());

            // Connect MQTT broker 
            client.connect(mqttConnectOpts);
            System.out.println("Connected to MQTT broker: " + client.getServerURI());

            pubParameters.setMessage(article);

            // Create mqttMessage
            MqttMessage mqttMessage = new MqttMessage(Serializer.serialize(article));
            mqttMessage.setQos(2);

            // Publish mqttMessage
            client.publish(pubParameters.getTopic(), mqttMessage);
            System.out.println("Published topic: " + pubParameters.getTopic());
            System.out.println("Published article: " + pubParameters.getMessage().getName());

                   } catch (MqttException e) {
            System.out.println("An error occurred: " + e.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(Producer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void subscibe(String topic) {
        mqtt.SubParameters subParameters = new mqtt.SubParameters();

        
        subParameters = mqtt.SubParameters.getInstance();
        String broker
                = subParameters.getBrokerProtocol() + "://"
                + subParameters.getBrokerAddress() + ":"
                + subParameters.getBrokerPort();

        subParameters.setTopic(topic);
        try {
            MqttClient client = new MqttClient(broker, MqttClient.generateClientId());
            client.setCallback(this);

            
            client.connect();
            System.out.println("Connected to MQTT broker: " + client.getServerURI());


            client.subscribe(subParameters.getTopic());
            System.out.println("Subscribed to topic: " + client.getTopic(subParameters.getTopic()));

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

            System.out.println("Article received: " + messageArticle.getName());
            System.out.println("Topic received: " + topic);
            
            if (topic.equals("demand")) {
                System.out.println("----------------------------------------");
                System.out.println("Nachfrage erhalten");
                for (int i = 0; i < producer.ProducerStock.pList.size(); i++) {
                    if (producer.ProducerStock.pList.get(i).getName().equals(messageArticle.getName())) {
                        System.out.println("Producer Preis: " + producer.ProducerStock.pList.get(i).getPrice());
                        System.out.println("----------------------------------------");
                        messageArticle.setPrice(producer.ProducerStock.pList.get(i).getPrice());
                        publish("order", messageArticle);
                    }
                }

            } else {
                System.out.println("Topic: " + topic);
            }
            
 
        } catch (IOException ex) {
            Logger.getLogger(Producer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Producer.class.getName()).log(Level.SEVERE, null, ex);
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

    //Erstellt zufälliges Angebot
    private TimerTask timerTask = new TimerTask() {

        @Override
        public void run() {
            Article ProducedArticle = new Article();
            String[] randomName = new String[5];
            randomName[0] = "Apfel";
            randomName[1] = "Banane";
            randomName[2] = "Birne";
            randomName[3] = "Mango";
            randomName[4] = "Durian";

            int randomPrice = ThreadLocalRandom.current().nextInt(1, 10);
            int randomArticleName = ThreadLocalRandom.current().nextInt(0, 4);
            ProducedArticle.setName(randomName[randomArticleName]);
            ProducedArticle.setAmount(0);
            ProducedArticle.setPrice(randomPrice);

            System.out.println("Angebot: " + ProducedArticle.getName() + " fuer " + ProducedArticle.getPrice() + " EUR");
            publish("offer", ProducedArticle);
        }
    };

    public static void main(String[] args) {
//        Producer p1 = new Producer("A", 4, 4, 5, 1, 2);
        Producer p2 = new Producer("C", 75, 23, 22, 23, 24);

//        p1.subscibe("demand");
        p2.subscibe("demand");
//        p2.publish("offer", new Article("Durian", 0, 2));

    }
}

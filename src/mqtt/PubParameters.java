package mqtt;
import java.util.List;
import thrift.Article;

public class PubParameters {

    private static PubParameters instance;
   
    private String brokerAddress = "127.0.0.1";
    private String brokerPort = "1883";
    private String brokerProtocol = "tcp";
    private String topic = "demand";
    private String message;
    
    private Article article;

    public static PubParameters getInstance() {
        if (instance == null)
            instance = new PubParameters();
        return instance;
    }

    public String getBrokerAddress() {
        return this.brokerAddress;
    }

    public void setBrokerAddress(String brokerAddress) {
        this.brokerAddress = brokerAddress;
    }

    public String getBrokerPort() {
        return this.brokerPort;
    }

    public void setBrokerPort(String brokerPort) {
        this.brokerPort = brokerPort;
    }

    public String getBrokerProtocol() {
        return this.brokerProtocol;
    }

    public void setBrokerProtocol(String brokerProtocol) {
        this.brokerProtocol = brokerProtocol;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMessageString() {
        return this.message;
    }
    
  
    public Article getMessage() {
        return this.article;
    }

    public void setMessage(Article article) {
        this.article = article;
    }

        public void setMessage(String message) {
        this.message = message;
    }
    
    public void setMessage(List<String> args) {
        this.message = "";
        for (String arg: args) this.message += arg + " ";
        this.message = this.message.trim();
    }

    public PubParameters() {}

}
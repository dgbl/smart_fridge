package mqtt;

public class SubParameters {

    private static SubParameters instance;
    private String brokerAddress = "iot.eclipse.org";
    private String brokerAddress = "127.0.0.1";
    private String brokerPort = "1883";
    private String brokerProtocol = "tcp";
    private String topic = "demand";

    public static SubParameters getInstance() {
        if (instance == null)
            instance = new SubParameters();
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
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public SubParameters() {}
}
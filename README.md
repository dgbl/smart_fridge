# smart_fridge

Thrift code gen: thrift -r --gen java order.thrift

Start the broker: cmd navigate to mosquitto

MQTT Test:

Start the command line subscriber:

    mosquitto_sub -v -t 'test/topic'

Publish test message with the command line publisher:

    mosquitto_pub -t 'test/topic' -m 'helloWorld'

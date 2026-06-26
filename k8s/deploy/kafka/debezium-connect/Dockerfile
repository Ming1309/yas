FROM quay.io/strimzi/kafka:0.47.0-kafka-3.9.1

USER root

RUN mkdir -p /opt/kafka/plugins/debezium-postgresql && \
    curl -L https://repo1.maven.org/maven2/io/debezium/debezium-connector-postgres/2.7.3.Final/debezium-connector-postgres-2.7.3.Final-plugin.tar.gz \
    | tar xz -C /opt/kafka/plugins/debezium-postgresql --strip-components=1

USER 1001

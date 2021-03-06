/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.camel.component.kafka;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;

import kafka.message.MessageAndMetadata;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.component.kafka.KafkaComponentUtil.constructExchange;

/**
 * Kafka Consumer Thread
 */
@SuppressWarnings("unchecked")
public class KafkaConsumerTask implements Runnable{

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerTask.class);

    /**
     * Camel Kafka endpoint
     */
    private final KafkaEndpoint endpoint;

    /**
     * Camel Processor
     */
    private final Processor processor;

    /**
     * Camel Kafka Configuration
     */
    private final KafkaConfiguration configuration;

    /**
     * Camel Kafka Consumer
     */
    private final KafkaConsumer consumer;

    /**
     * Consumer Iterator
     */
    private final ConsumerIterator consumerIterator;

    /**
     * Default constructor.
     *
     * NOTE: when used as consumer thread
     */
    public KafkaConsumerTask(final KafkaStream stream,
                             final KafkaEndpoint endpoint,
                             final KafkaConsumer consumer,
                             final Processor processor,
                             final KafkaConfiguration configuration) {

        this.consumerIterator = stream.iterator();
        this.endpoint = endpoint;
        this.consumer = consumer;
        this.processor = processor;
        this.configuration = configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
       
            if (LOGGER.isDebugEnabled()) {

                LOGGER.debug("Camel Kafka Consumer started.");
            }

            while (consumerIterator.hasNext()) {

                MessageAndMetadata<byte[], byte[]> incomingData = consumerIterator.next();

                if (incomingData != null) {

                    if (LOGGER.isDebugEnabled()) {

                        LOGGER.debug("Kafka Consumer Message received : " + incomingData);
                    }

                    final Exchange exchange = constructExchange(endpoint, incomingData, configuration);

                    try {

                        this.processor.process(exchange);
                    } catch (Exception e) {

                        LOGGER.error("Error processing the message:", e);
                    } finally {

                        if (exchange.getException() != null) {

                            consumer.getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
                        }
                    }
                }
            }
    }
}

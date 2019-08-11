package com.lakwarus.sample.pojo;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.io.JsonEOFException;

@SpringBootApplication
public class SpringbootCamelRestdslApplication {

    // Order management is done using an in-memory map.
    Map<String, Order> objectmap;

    public SpringbootCamelRestdslApplication() {
        objectmap = new HashMap<String, Order>();
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringbootCamelRestdslApplication.class, args);
    }

    @Component
    class OrderRoute extends RouteBuilder {

        @Override
        public void configure() throws Exception {

        	
            restConfiguration().component("servlet").bindingMode(RestBindingMode.json);

            onException(Exception.class).handled(true).process(new Processor() {

                public void process(Exchange exchange) throws Exception {
                    Exception ex = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);

                    if (ex instanceof JsonEOFException) {
                        Status status = new Status();
                        status.setOrderId("null");
                        status.setStatus("Malformed JSON received");

                        // Create response message.
                        exchange.getOut().setBody(status, Status.class);
                    } else {
                        Status status = new Status();
                        status.setOrderId("null");
                        status.setStatus(" Error occurred while processing the request !!!");

                        // Create response message.
                        exchange.getOut().setBody(status, Status.class);
                    }

                }
            });

            // Resource that handles the HTTP POST requests that are directed to the path
            // '/order' to create a new Order.
            rest("/ordermgt").consumes("application/json").post("/order").type(Order.class).to("direct:addOrder");

            from("direct:addOrder").doTry().process(new Processor() {

                @Override
                public void process(Exchange exchange) throws Exception {

                    Order order = exchange.getIn().getBody(Order.class);
                    String orderId = order.getId();
                    if (orderId == null) {
                        throw new OrderValidationException();
                    }
                    objectmap.put(orderId, order);

                    Status status = new Status();
                    status.setOrderId(orderId);
                    status.setStatus("Order Created!");

                    // Create response message.
                    exchange.getOut().setBody(status, Status.class);

                    // Set 201 Created status code in the response message.
                    exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
                    // Set 'Location' header in the response message.
                    // This can be used by the client to locate the newly added order.
                    exchange.getOut().setHeader("Location", "http://localhost:8080/ordermgt/order/" + orderId);
                }
            }).doCatch(OrderValidationException.class).process(new Processor() {

                public void process(Exchange exchange) throws Exception {

                    Status status = new Status();
                    status.setOrderId("null");
                    status.setStatus("Order Id is Null !!");

                    // Create response message.
                    exchange.getOut().setBody(status, Status.class);
                    exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);

                }
            }).log("AddOrder error : Invalid JSON recevied!").doCatch(Exception.class).process(new Processor() {

                public void process(Exchange exchange) throws Exception {
                    if (exchange.getOut().getBody(Order.class) == null) {
                        Status status = new Status();
                        status.setOrderId("null");
                        status.setStatus("JSON payload is empty!");

                        // Create response message.
                        exchange.getOut().setBody(status, Status.class);
                        exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
                    }

                }
            }).log("AddOrder error : JSON payload is empty!");

            // Resource that handles the HTTP GET requests that are directed to a specific
            // order using path '/order/<orderId>'.
            rest("/ordermgt").get("/order/{orderId}").to("direct:getOrder");

            from("direct:getOrder").doTry().process(new Processor() {

                @Override
                public void process(Exchange exchange) {

                    // Find the requested order from the map and retrieve it in JSON format.
                    String orderId = exchange.getIn().getHeader("orderId", String.class);
                    Order order = objectmap.get(orderId);

                    if (order == null) {

                        Status status = new Status();
                        status.setOrderId(orderId);
                        status.setStatus("Get order error : " + orderId + " cannot be found!");

                        exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
                    } else {

                        exchange.getOut().setBody(order, Order.class);
                        exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
                    }

                }
            }).doCatch(Exception.class).process(new Processor() {

                public void process(Exchange exchange) throws Exception {

                    Status status = new Status();
                    status.setOrderId("null");
                    status.setStatus("Internal server error!");

                    exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);

                }
            }).log("Get order error : error while processing getOrder");

            // Resource that handles the HTTP PUT requests that are directed to the path
            // '/order/<orderId>' to update an existing Order.
            rest("/ordermgt").consumes("application/json").put("/order/{orderId}").type(Order.class)
                    .to("direct:putOrder");

            from("direct:putOrder").doTry().process(new Processor() {

                @Override
                public void process(Exchange exchange) {

                    Order newOrder = exchange.getIn().getBody(Order.class);
                    // Find the requested order from the map and retrieve it in JSON format.
                    String orderId = exchange.getIn().getHeader("orderId", String.class);
                    Order oldOrder = objectmap.get(orderId);
                    if (oldOrder != null) {

                        // Updating existing order with the attributes of the updated order.
                        objectmap.replace(orderId, newOrder);
                        exchange.getOut().setBody(newOrder, Order.class);
                        exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
                    } else {

                        Status status = new Status();
                        status.setOrderId(orderId);
                        status.setStatus("Update Order error : orderId cannot be found!");
                        exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
                    }

                }
            }).doCatch(Exception.class).process(new Processor() {

                public void process(Exchange exchange) throws Exception {

                    Status status = new Status();
                    status.setOrderId("null");
                    status.setStatus("Invalid JSON recevied!");

                    exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);

                }
            }).log("UpdateOrder error : Invalid JSON recevied!");

            // Resource that handles the HTTP DELETE requests, which are directed to the
            // path
            // '/order/<orderId>' to delete an existing Order.
            rest("/ordermgt").delete("/order/{orderId}").to("direct:deleteOrder");

            from("direct:deleteOrder").doTry().process(new Processor() {

                @Override
                public void process(Exchange exchange) {

                    // Find the requested order from the map and retrieve it in JSON format.
                    String orderId = exchange.getIn().getHeader("orderId", String.class);
                    Order oldOrder = objectmap.get(orderId);
                    if (oldOrder != null) {

                        // Updating existing order with the attributes of the updated order.
                        objectmap.remove(orderId);
                        Status status = new Status();
                        status.setOrderId(orderId);
                        status.setStatus("Order deleted!");

                        // Create response message.
                        exchange.getOut().setBody(status, Status.class);
                    } else {

                        Status status = new Status();
                        status.setOrderId(orderId);
                        status.setStatus("Delete Order error : orderId  cannot be found!");

                        exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
                    }

                }
            }).doCatch(Exception.class).process(new Processor() {

                public void process(Exchange exchange) throws Exception {

                    Status status = new Status();
                    status.setOrderId("null");
                    status.setStatus("Internal server error!");

                    exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);

                }
            }).log("Delete order error : error while processing deleteOrder");

        }
    }

}
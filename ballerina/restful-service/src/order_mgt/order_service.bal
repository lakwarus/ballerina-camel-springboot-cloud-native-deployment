import ballerina/http;
import ballerina/log;
import ballerinax/kubernetes;

@kubernetes:Service {
    serviceType:"NodePort",
    name:"restful-service"
}
listener http:Listener httpListener = new(8080);

// Order management is done using an in-memory map.
// Add some sample orders to 'ordersMap' at startup.
map<json> ordersMap = {};

@kubernetes:Deployment {
    image:"restful-service:v1.0",
    name:"restful-service"
}
// RESTful service.
@http:ServiceConfig { basePath: "/ordermgt" }
service orderMgt on httpListener {

    // Resource that handles the HTTP POST requests that are directed to the path
    // '/order' to create a new Order.
    @http:ResourceConfig {
        methods: ["POST"],
        path: "/order"
    }
    resource function addOrder(http:Caller caller, http:Request req) {
        http:Response response = new;
        var orderReq = req.getJsonPayload();
        if (orderReq is json) {
            
            json|error orderIdJson = orderReq.id;
            if (orderIdJson is error) {

                // Create response message.
                json payload = { status: "OrderId is Empty!", orderId: "null" };
                response.statusCode = 400;
                response.setJsonPayload(payload);
            } else {

                string orderId = orderIdJson.toString();

                // Find the duplicate orders
                json existingOrder = ordersMap[orderId];

                if (existingOrder == null) {
                    ordersMap[orderId] = orderReq;

                    // Create response message.
                    json payload = { status: "Order Created.", orderId: orderId };
                    response.setJsonPayload(payload);

                    // Set 201 Created status code in the response message.
                    response.statusCode = 201;
                    // Set 'Location' header in the response message.
                    // This can be used by the client to locate the newly added order.
                    response.setHeader("Location", "http://localhost:8080/ordermgt/order/" +
                            orderId);
                } else {
                    response.statusCode = 500;
                    json payload = { status: "Duplicate Order", orderId: orderId };
                    response.setJsonPayload(payload);
                }
            }
        } else {
            response.statusCode = 400;
            json payload = { status: "Invalid JSON received!", orderId: "null" };
            response.setJsonPayload(payload);
        }
        // Send response to the client.
        var result = caller->respond(response);
        if (result is error) {
            log:printError("Error sending response", err = result);
        }
    }

    // Resource that handles the HTTP GET requests that are directed to a specific
    // order using path '/order/<orderId>'.
    @http:ResourceConfig {
        methods: ["GET"],
        path: "/order/{orderId}"
    }
    resource function getOrder(http:Caller caller, http:Request req, string orderId) {

        // Find the requested order from the map and retrieve it in JSON format.
        json payload = ordersMap[orderId];
        http:Response response = new;
        if (payload == null) {
            payload = { status: "Order cannot be found!", orderId: orderId };
        }

        // Set the JSON payload in the outgoing response message.
        response.setJsonPayload(payload);

        // Send response to the client.
        var result = caller->respond(response);
        if (result is error) {
            log:printError("Error sending response", err = result);
        }
    }

    // Resource that handles the HTTP PUT requests that are directed to the path
    // '/order/<orderId>' to update an existing Order.
    @http:ResourceConfig {
        methods: ["PUT"],
        path: "/order/{orderId}"
    }
    resource function updateOrder(http:Caller caller, http:Request req, string orderId) {
        var updatedOrder = req.getJsonPayload();
        http:Response response = new;
        if (updatedOrder is json) {
            // Find the order that needs to be updated and retrieve it in JSON format.
            json existingOrder = ordersMap[orderId];

            // Updating existing order
            if (existingOrder != null) {
                ordersMap[orderId] = updatedOrder;

                json payload = { status: "Order updated", orderId: orderId };
                response.setJsonPayload(payload);

            } else {
                json payload = { status: "Order cannot be found!", orderId: orderId };
                response.setJsonPayload(payload);
            }
        } else {
            response.statusCode = 400;
            json payload = { status: "Invalid JSON received!", orderId: "null" };
            response.setJsonPayload(payload);
        }
        var result = caller->respond(response);
        if (result is error) {
            log:printError("Error sending response", err = result);
        }
    }

    // Resource that handles the HTTP DELETE requests, which are directed to the path
    // '/order/<orderId>' to delete an existing Order.
    @http:ResourceConfig {
        methods: ["DELETE"],
        path: "/order/{orderId}"
    }
    resource function cancelOrder(http:Caller caller, http:Request req, string orderId) {
        http:Response response = new;
        
        // Find the order that needs to be updated and retrieve it in JSON format.
        json existingOrder = ordersMap[orderId];

        // Updating existing order
        if (existingOrder != null) {
        
            // Remove the requested order from the map.
            _ = ordersMap.remove(orderId);

            json payload = { status: "Order removed!", orderId: orderId };
            response.setJsonPayload(payload);

        }else {
            json payload = { status: "Order cannot be found!", orderId: orderId };
            response.setJsonPayload(payload);
        }        
        
        // Send response to the client.
        var result = caller->respond(response);
        if (result is error) {
            log:printError("Error sending response", err = result);
        }
    }
}

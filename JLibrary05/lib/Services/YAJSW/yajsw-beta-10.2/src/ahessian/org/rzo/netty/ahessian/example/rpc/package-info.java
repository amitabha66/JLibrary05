/**
 * Hessian RPC Example.
 *
 * The example is divided into client, server and service.
 * Client implementation is found in the package example.rpc.client
 * Server implementation is found in the package example.rpc.server
 * Service implementation is found in the package example.rpc.service
 * 
 * HelloWorldService implements the helloworld service. Which sends back the received string.
 * The client sends some rpc requests to the service, waits for all responses and displays the performance.
 * 
 * The example is configured to use sessions, so that the client can disconnect and reconnect to receive the
 * remaining responses.
 */
package org.rzo.netty.ahessian.example.rpc;
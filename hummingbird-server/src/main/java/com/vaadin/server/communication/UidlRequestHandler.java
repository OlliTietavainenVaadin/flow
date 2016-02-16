/*
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.server.communication;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.server.ServletHelper;
import com.vaadin.server.SessionExpiredHandler;
import com.vaadin.server.SynchronizedRequestHandler;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.communication.ServerRpcHandler.InvalidUIDLSecurityKeyException;
import com.vaadin.shared.JsonConstants;
import com.vaadin.ui.UI;

import elemental.json.JsonException;
import elemental.json.JsonObject;

/**
 * Processes a UIDL request from the client.
 *
 * Uses {@link ServerRpcHandler} to execute client-to-server RPC invocations and
 * {@link UidlWriter} to write state changes and client RPC calls back to the
 * client.
 *
 * @author Vaadin Ltd
 * @since 7.1
 */
public class UidlRequestHandler extends SynchronizedRequestHandler
        implements SessionExpiredHandler {

    public static final String UIDL_PATH = "UIDL/";

    private ServerRpcHandler rpcHandler = new ServerRpcHandler();

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return ServletHelper.isUIDLRequest(request);
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        UI uI = session.getService().findUI(request);
        if (uI == null) {
            // This should not happen but it will if the UI has been closed. We
            // really don't want to see it in the server logs though
            commitJsonResponse(response,
                    getUINotFoundErrorJSON(session.getService(), request));
            return true;
        }

        StringWriter stringWriter = new StringWriter();

        try {
            rpcHandler.handleRpc(uI, request.getReader(), request);

            writeUidl(uI, stringWriter);
        } catch (JsonException e) {
            getLogger().log(Level.SEVERE, "Error writing JSON to response", e);
            // Refresh on client side
            writeRefresh(response);
            return true;
        } catch (InvalidUIDLSecurityKeyException e) {
            getLogger().log(Level.WARNING,
                    "Invalid security key received from {0}",
                    request.getRemoteHost());
            // Refresh on client side
            writeRefresh(response);
            return true;
        } finally {
            stringWriter.close();
        }

        commitJsonResponse(response, stringWriter.toString());
        return true;
    }

    private void writeRefresh(VaadinResponse response) throws IOException {
        String json = VaadinService.createCriticalNotificationJSON(null, null,
                null, null);
        commitJsonResponse(response, json);
    }

    private static void writeUidl(UI ui, Writer writer) throws IOException {
        JsonObject uidl = new UidlWriter().createUidl(ui, false);

        // some dirt to prevent cross site scripting
        String responseString = "for(;;);[" + uidl.toJson() + "]";
        writer.write(responseString);
    }

    private static final Logger getLogger() {
        return Logger.getLogger(UidlRequestHandler.class.getName());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.vaadin.server.SessionExpiredHandler#handleSessionExpired(com.vaadin
     * .server.VaadinRequest, com.vaadin.server.VaadinResponse)
     */
    @Override
    public boolean handleSessionExpired(VaadinRequest request,
            VaadinResponse response) throws IOException {
        if (!ServletHelper.isUIDLRequest(request)) {
            return false;
        }
        VaadinService service = request.getService();
        SystemMessages systemMessages = service.getSystemMessages(
                ServletHelper.findLocale(null, request), request);

        service.writeStringResponse(response, JsonConstants.JSON_CONTENT_TYPE,
                VaadinService.createCriticalNotificationJSON(
                        systemMessages.getSessionExpiredCaption(),
                        systemMessages.getSessionExpiredMessage(), null,
                        systemMessages.getSessionExpiredURL()));

        return true;
    }

    /**
     * Returns the JSON which should be returned to the client when a request
     * for a non-existent UI arrives.
     *
     * @param service
     *            The VaadinService
     * @param vaadinRequest
     *            The request which triggered this, or null if not available
     * @since 7.1
     * @return A JSON string
     */
    static String getUINotFoundErrorJSON(VaadinService service,
            VaadinRequest vaadinRequest) {
        SystemMessages ci = service.getSystemMessages(vaadinRequest.getLocale(),
                vaadinRequest);
        // Session Expired is not really the correct message as the
        // session exists but the requested UI does not.
        // Using Communication Error for now.
        return VaadinService.createCriticalNotificationJSON(
                ci.getCommunicationErrorCaption(),
                ci.getCommunicationErrorMessage(), null,
                ci.getCommunicationErrorURL());
    }

    /**
     * Commit the JSON response. We can't write immediately to the output stream
     * as we want to write only a critical notification if something goes wrong
     * during the response handling.
     *
     * @param response
     *            The response to write to
     * @param json
     *            The JSON to write
     * @throws IOException
     *             If there was an exception while writing to the output
     */
    public static void commitJsonResponse(VaadinResponse response, String json)
            throws IOException {
        response.setContentType(JsonConstants.JSON_CONTENT_TYPE);

        // Ensure that the browser does not cache UIDL responses.
        // iOS 6 Safari requires this (#9732)
        response.setHeader("Cache-Control", "no-cache");

        byte[] b = json.getBytes("UTF-8");
        response.setContentLength(b.length);

        OutputStream outputStream = response.getOutputStream();
        outputStream.write(b);
        // NOTE GateIn requires the buffers to be flushed to work
        outputStream.flush();
    }
}

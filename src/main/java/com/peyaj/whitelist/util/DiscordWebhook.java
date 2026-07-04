package com.peyaj.whitelist.util;

import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscordWebhook {

    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * Sends an asynchronous POST request containing the json payload to the specified webhook URL.
     */
    public static void sendWebhook(Logger logger, String webhookUrl, JsonObject payload) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty() || webhookUrl.contains("YOUR_DISCORD_WEBHOOK_URL_HERE")) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl.trim()))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "PeyajWhitelist")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            logger.warning("Discord Webhook responded with status: " + response.statusCode() + " - Body: " + response.body());
                        }
                    })
                    .exceptionally(ex -> {
                        logger.log(Level.WARNING, "Failed to deliver Discord webhook asynchronously", ex);
                        return null;
                    });
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Error preparing Discord webhook request", t);
        }
    }
}

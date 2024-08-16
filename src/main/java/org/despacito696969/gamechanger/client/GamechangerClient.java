package org.despacito696969.gamechanger.client;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.despacito696969.gamechanger.Gamechanger;

public class GamechangerClient implements ClientModInitializer {
    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
            Gamechanger.CONFIG_SYNC_PACKET_ID,
            (client, handler, buf, responseSender) -> {
                var str = buf.readUtf();
                try {
                    var json = JsonParser.parseString(str);
                    Gamechanger.resetManagers();
                    Gamechanger.loadFromJson(json);
                }
                catch (JsonSyntaxException expection) {
                    Gamechanger.LOGGER.error("Received incorrect json from server!");
                    Gamechanger.LOGGER.error(str);
                }
            }
        );
    }
}

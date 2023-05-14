package me.thatlukinhasguy.autovpnblocker.Database.Utils;

import me.thatlukinhasguy.autovpnblocker.Database.ConfigProvider;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class REST {
    public void downloadDatabase(ConfigProvider config) throws Exception {
        URL url = new URL("https://raw.githubusercontent.com/ThatLukinhasGuy/IP-Database/main/db.sqlite");

        try (BufferedInputStream inputStream = new BufferedInputStream(url.openStream()); FileOutputStream fileOutputStream = new FileOutputStream(config.getDataFolder() + "/db.sqlite")) {
            byte[] dataBuffer = new byte[1024];

            int bytesRead;

            while ((bytesRead = inputStream.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            // Todo - Logging/Disable plugin.
        }
    }

    public String checkUpdate() throws IOException {
        URL url = new URL("https://api.github.com/repos/ThatLukinhasGuy/IP-Database");

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String inputLine;

        StringBuffer res = new StringBuffer();

        while ((inputLine = input.readLine()) != null) {
            res.append(inputLine);
        }

        input.close();
        connection.disconnect();

        JSONObject jsonRes = new JSONObject(res.toString());

        return jsonRes.getString("pushed_at");
    }
}

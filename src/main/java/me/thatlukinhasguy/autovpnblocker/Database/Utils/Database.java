package me.thatlukinhasguy.autovpnblocker.Database.Utils;

import inet.ipaddr.IPAddressString;
import me.thatlukinhasguy.autovpnblocker.Database.ConfigProvider;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.sql.*;
import java.util.Objects;
import java.util.stream.Stream;

public class Database {
    private final ConfigProvider config;

    private Connection connection;

    public Database(ConfigProvider config) throws SQLException {
        this.config = config;

        this.makeConnection();
    }

    public JSONObject checkIp(String address) throws SQLException {
        PreparedStatement preparedStatement = this.connection.prepareStatement("SELECT * FROM IPv4 WHERE Start <= ? AND End >= ?;");

        BigInteger addressInt = new IPAddressString(address).getAddress().getValue();

        long x = Long.parseLong(String.valueOf(addressInt));

        preparedStatement.setLong(1, x);
        preparedStatement.setLong(2, x);

        ResultSet resultSet = preparedStatement.executeQuery();

        JSONArray results = new JSONArray();

        while (resultSet.next()) {
            JSONObject object = new JSONObject();

            object.put("start", resultSet.getInt("Start"));
            object.put("end", resultSet.getInt("End"));
            object.put("asn", resultSet.getString("ASN"));
            object.put("country", resultSet.getString("Country"));
            object.put("city", resultSet.getString("City"));
            object.put("region", resultSet.getString("Region"));
            object.put("longitude", resultSet.getString("Longitude"));
            object.put("latitude", resultSet.getString("Latitude"));
            object.put("timezone", resultSet.getString("TimeZone"));
            object.put("postal", resultSet.getString("Postal"));
            object.put("isp", resultSet.getString("ISP"));

            String types = resultSet.getString("Types");

            object.put("types", new JSONArray(Stream.of(types.split(", ")).mapToInt(Integer::parseInt).toArray()));

            results.put(object);
        }

        preparedStatement.close();
        resultSet.close();

        if (results.length() > 1) {
            for (Object result : results) {
                JSONObject obj = new JSONObject(result.toString());

                if (Objects.equals(obj.getBigInteger("start"), address) && Objects.equals(obj.getBigInteger("end"), address)) return obj;
            }
        } else {
            return results.getJSONObject(0);
        }

        return null;
    }

    public void close() throws SQLException {
        this.connection.close();

        this.connection = null;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void makeConnection() throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.config.getDataFolder() + "/db.sqlite");
    }
}

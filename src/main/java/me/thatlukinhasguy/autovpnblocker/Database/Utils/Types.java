package me.thatlukinhasguy.autovpnblocker.Database.Utils;

public enum Types {
    RESIDENTIAL(1, "Residential"),
    BUSINESS(2, "Business"),
    HOSTING(3, "Hosting"),
    VPN(4, "VPN"),
    DDOS_MITIGATION(5, "DDoS Mitigation"),
    TOR_RELAY(6, "Tor Relay"),
    CLOUD_GAMING(7, "Cloud Gaming"),
    MOBILE_CARRIER(8, "Mobile Carrier");

    private final int id;

    private final String name;

    Types(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public static Types getById(int id) {
        for (Types type : values()) {
            if (type.getId() == id) return type;
        }

        return null;
    }
}
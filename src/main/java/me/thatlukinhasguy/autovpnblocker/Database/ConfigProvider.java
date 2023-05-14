package me.thatlukinhasguy.autovpnblocker.Database;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public abstract class ConfigProvider {
    protected File dataFolder = null;
    protected File configFile = null;

    protected HashMap<String, BlockProvider> blocks;

    protected BlacklistProvider countryBlacklist;
    protected BlacklistProvider ispBlacklist;

    protected List<String> whitelist;

    public ConfigProvider() {
        this.dataFolder = dataFolder;
        this.configFile = configFile;
        blocks = new HashMap<>();
    }


    public File getDataFolder() {
        return this.dataFolder;
    }

    public HashMap<String, BlockProvider> getBlocks() {
        return this.blocks;
    }

    public BlacklistProvider getCountryBlacklist() {
        return this.countryBlacklist;
    }

    public BlacklistProvider getIspBlacklist() {
        return this.ispBlacklist;
    }

}
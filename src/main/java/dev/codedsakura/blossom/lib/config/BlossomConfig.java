package dev.codedsakura.blossom.lib.config;

public abstract class BlossomConfig {
    protected Integer version;

    protected abstract int getLatestVersion();

    /**
     * @return whether updating was successful, and no backup+reset needed
     */
    public boolean update() {
        return false;
    }
}

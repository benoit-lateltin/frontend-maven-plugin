package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public interface VoltaExecutorConfig {

    File getVoltaPath();

    File getWorkingDirectory();

    Platform getPlatform();
}

final class InstallVoltaExecutorConfig implements VoltaExecutorConfig {

    private static final String VOLTA_DEFAULT = VoltaInstaller.INSTALL_PATH + "/dist/bin/volta";

    private final InstallConfig installConfig;

    public InstallVoltaExecutorConfig(InstallConfig installConfig) {
        this.installConfig = installConfig;
    }

    @Override
    public File getVoltaPath() {
        return new File(installConfig.getInstallDirectory() + VOLTA_DEFAULT);
    }

    @Override
    public File getWorkingDirectory() {
        return installConfig.getWorkingDirectory();
    }

    @Override
    public Platform getPlatform() {
        return installConfig.getPlatform();
    }
}
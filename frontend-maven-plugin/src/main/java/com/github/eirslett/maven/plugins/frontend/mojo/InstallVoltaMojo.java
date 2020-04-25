package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.*;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.SettingsDecrypter;

@Mojo(name = "install-volta", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class InstallVoltaMojo extends AbstractFrontendMojo {

    /**
     * Where to download Volta binary from. Defaults to https://github.com/volta-cli/volta/releases
     */
    @Parameter(property = "voltaDownloadRoot", required = false,
        defaultValue = VoltaInstaller.DEFAULT_VOLTA_DOWNLOAD_ROOT)
    private String voltaDownloadRoot;

    /**
     * The version of Volta to install. IMPORTANT! Most Volta names start with 'v', for example 'v0.7.2'.
     */
    @Parameter(property = "voltaVersion", required = true)
    private String voltaVersion;

    /**
     * Server Id for download username and password
     */
    @Parameter(property = "serverId", defaultValue = "")
    private String serverId;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.installvolta", alias = "skip.installvolta", defaultValue = "${skip.installvolta}")
    private boolean skip;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws InstallationException {
        ProxyConfig proxyConfig = MojoUtils.getProxyConfig(this.session, this.decrypter);
        Server server = MojoUtils.decryptServer(this.serverId, this.session, this.decrypter);
        if (null != server) {
            factory.getVoltaInstaller(proxyConfig).setVoltaDownloadRoot(this.voltaDownloadRoot)
                .setVoltaVersion(this.voltaVersion).setPassword(server.getPassword())
                .setUserName(server.getUsername()).install();
        } else {
            factory.getVoltaInstaller(proxyConfig).setVoltaDownloadRoot(this.voltaDownloadRoot)
                    .setVoltaVersion(this.voltaVersion).install();
        }
    }

}

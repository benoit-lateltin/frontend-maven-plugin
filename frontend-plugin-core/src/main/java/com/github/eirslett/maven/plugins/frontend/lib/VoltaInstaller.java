package com.github.eirslett.maven.plugins.frontend.lib;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

public class VoltaInstaller {

    public static final String INSTALL_PATH = "/volta";

    public static final String DEFAULT_VOLTA_DOWNLOAD_ROOT = "https://github.com/volta-cli/volta/releases";

    private static final Object LOCK = new Object();

    private String voltaVersion, voltaDownloadRoot, userName, password;

    private final Logger logger;

    private final InstallConfig config;

    private final ArchiveExtractor archiveExtractor;

    private final FileDownloader fileDownloader;

    VoltaInstaller(InstallConfig config, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
    }

    public VoltaInstaller setVoltaVersion(String voltaVersion) {
        this.voltaVersion = voltaVersion;
        return this;
    }

    public VoltaInstaller setVoltaDownloadRoot(String voltaDownloadRoot) {
        this.voltaDownloadRoot = voltaDownloadRoot;
        return this;
    }

    public VoltaInstaller setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public VoltaInstaller setPassword(String password) {
        this.password = password;
        return this;
    }

    public void install() throws InstallationException {
        // use static lock object for a synchronized block
        synchronized (LOCK) {
            if (voltaDownloadRoot == null || voltaDownloadRoot.isEmpty()) {
                voltaDownloadRoot = DEFAULT_VOLTA_DOWNLOAD_ROOT;
            }
            if (!voltaIsAlreadyInstalled()) {
                if (!voltaVersion.startsWith("v")) {
                    throw new InstallationException("Volta version has to start with prefix 'v'.");
                }
                installVolta();
            }
        }
    }

    private boolean voltaIsAlreadyInstalled() {
        try {
            VoltaExecutorConfig executorConfig = new InstallVoltaExecutorConfig(config);
            File nodeFile = executorConfig.getVoltaPath();
            if (nodeFile.exists()) {
                String version = new VoltaExecutor(executorConfig, Arrays.asList("--version"), null).executeAndGetResult(logger).trim();

                if (version.equals(voltaVersion.replaceFirst("^v", ""))) {
                    logger.info("Volta {} is already installed.", version);
                    return true;
                } else {
                    logger.info("Volta {} was installed, but we need version {}", version, voltaVersion);
                    return false;
                }
            } else {
                return false;
            }
        } catch (ProcessExecutionException e) {
            return false;
        }
    }

    private void installVolta() throws InstallationException {
        try {
            logger.info("Installing Volta version {}", voltaVersion);
            String downloadUrl = voltaDownloadRoot + voltaVersion;
            String extension = "tar.gz";
            // todo add platform support
            String fileending = "/volta-" + voltaVersion + "." + extension;

            downloadUrl += fileending;

            logger.info("Will download Volta here: {}", downloadUrl);

            CacheDescriptor cacheDescriptor = new CacheDescriptor("volta", voltaVersion, extension);

            File archive = config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, userName, password);

            File installDirectory = getInstallDirectory();

            // We need to delete the existing yarn directory first so we clean out any old files, and
            // so we can rename the package directory below.
            try {
                if (installDirectory.isDirectory()) {
                    FileUtils.deleteDirectory(installDirectory);
                }
            } catch (IOException e) {
                logger.warn("Failed to delete existing Yarn installation.");
            }

            try {
                extractFile(archive, installDirectory);
            } catch (ArchiveExtractionException e) {
                if (e.getCause() instanceof EOFException) {
                    // The downloading was probably interrupted and archive file is incomplete:
                    // delete it to retry from scratch
                    logger.error("The archive file {} is corrupted and will be deleted. "
                            + "Please try the build again.", archive.getPath());
                    archive.delete();
                    if (installDirectory.exists()) {
                        FileUtils.deleteDirectory(installDirectory);
                    }
                }

                throw e;
            }

            ensureCorrectVoltaRootDirectory(installDirectory, voltaVersion);

            logger.info("Installed Yarn locally.");
        } catch (DownloadException e) {
            throw new InstallationException("Could not download Yarn", e);
        } catch (ArchiveExtractionException | IOException e) {
            throw new InstallationException("Could not extract the Yarn archive", e);
        }
    }

    private File getInstallDirectory() {
        File installDirectory = new File(config.getInstallDirectory(), INSTALL_PATH);
        if (!installDirectory.exists()) {
            logger.debug("Creating install directory {}", installDirectory);
            installDirectory.mkdirs();
        }
        return installDirectory;
    }

    private void extractFile(File archive, File destinationDirectory) throws ArchiveExtractionException {
        logger.info("Unpacking {} into {}", archive, destinationDirectory);
        archiveExtractor.extract(archive.getPath(), destinationDirectory.getPath());
    }

    private void ensureCorrectVoltaRootDirectory(File installDirectory, String voltaVersion) throws IOException {
        // here maybe we should check the structure of the archive content
        // - volta bin
        // - volta-shim bin
        // - volta-migrate bin

        // yarn check was :

//        File voltaRootDirectory = new File(installDirectory, YARN_ROOT_DIRECTORY);
//        if (!yarnRootDirectory.exists()) {
//            logger.debug("Yarn root directory not found, checking for yarn-{}", yarnVersion);
//            // Handle renaming Yarn 1.X root to YARN_ROOT_DIRECTORY
//            File yarnOneXDirectory = new File(installDirectory, "yarn-" + yarnVersion);
//            if (yarnOneXDirectory.isDirectory()) {
//                if (!yarnOneXDirectory.renameTo(yarnRootDirectory)) {
//                    throw new IOException("Could not rename versioned yarn root directory to " + YARN_ROOT_DIRECTORY);
//                }
//            } else {
//                throw new FileNotFoundException("Could not find yarn distribution directory during extract");
//            }
//        }
    }

    private void downloadFileIfMissing(String downloadUrl, File destination, String userName, String password)
            throws DownloadException {
        if (!destination.exists()) {
            downloadFile(downloadUrl, destination, userName, password);
        }
    }

    private void downloadFile(String downloadUrl, File destination, String userName, String password)
            throws DownloadException {
        logger.info("Downloading {} to {}", downloadUrl, destination);
        fileDownloader.download(downloadUrl, destination.getPath(), userName, password);
    }
}

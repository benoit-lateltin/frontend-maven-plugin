package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class VoltaExecutor {

    private final ProcessExecutor executor;

    public VoltaExecutor(VoltaExecutorConfig config, List<String> arguments,
                         Map<String, String> additionalEnvironment) {
        final String yarn = config.getVoltaPath().getAbsolutePath();
        List<String> localPaths = new ArrayList<>();
        localPaths.add(config.getVoltaPath().getParent());
        executor = new ProcessExecutor(config.getWorkingDirectory(), localPaths,
            Utils.prepend(yarn, arguments), config.getPlatform(), additionalEnvironment);
    }

    public String executeAndGetResult(final Logger logger) throws ProcessExecutionException {
        return executor.executeAndGetResult(logger);
    }

    public int executeAndRedirectOutput(final Logger logger) throws ProcessExecutionException {
        return executor.executeAndRedirectOutput(logger);
    }
}

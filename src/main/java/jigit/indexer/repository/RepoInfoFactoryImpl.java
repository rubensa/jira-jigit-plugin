package jigit.indexer.repository;

import jigit.settings.JigitRepo;
import jigit.settings.JigitSettingsManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;

public final class RepoInfoFactoryImpl implements RepoInfoFactory {
    @NotNull
    private final JigitSettingsManager settingsManager;

    public RepoInfoFactoryImpl(@NotNull JigitSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    @Override @NotNull
    public Collection<RepoInfo> build(@NotNull JigitRepo repo) throws IOException {
        return repo.getServiceType().repositories(repo, settingsManager);
    }
}

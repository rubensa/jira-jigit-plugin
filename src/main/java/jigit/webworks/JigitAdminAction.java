package jigit.webworks;

import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;
import jigit.ao.CommitManager;
import jigit.common.JigitDateFormatter;
import jigit.common.UrlActions;
import jigit.indexer.repository.RepoType;
import jigit.indexer.repository.ServiceType;
import jigit.settings.JigitRepo;
import jigit.settings.JigitSettingsManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
//all methods are used in velocity view
public final class JigitAdminAction extends JiraWebActionSupport {
    @NotNull
    private final ApplicationProperties applicationProperties;
    @NotNull
    private final JigitSettingsManager settingsManager;
    @NotNull
    private final CommitManager commitManager;
    @NotNull
    private final JigitDateFormatter dateFormatter;

    public JigitAdminAction(@NotNull ApplicationProperties applicationProperties,
                            @NotNull JigitSettingsManager settingsManager,
                            @NotNull CommitManager commitManager,
                            @NotNull JigitDateFormatter dateFormatter) {
        this.applicationProperties = applicationProperties;
        this.settingsManager = settingsManager;
        this.commitManager = commitManager;
        this.dateFormatter = dateFormatter;
    }

    public int getCommitCount(@NotNull String repoName, @NotNull String branchName) {
        return commitManager.getCommitCount(repoName, branchName);
    }

    public int getCommitCount(@NotNull String repoGroupName) {
        return commitManager.getCommitCount(repoGroupName);
    }

    @NotNull
    public Map<String, JigitRepo> getJigitRepos() {
        return Collections.unmodifiableMap(new TreeMap<>(settingsManager.getJigitRepos()));
    }

    @NotNull
    public String getBaseUrl() {
        return applicationProperties.getBaseUrl();
    }

    public boolean hasAdminPermission() {
        return getPermissionManager().hasPermission(Permissions.ADMINISTER, getLoggedInUser());
    }

    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    @NotNull
    public String getSleepToFormatted(long date) {
        return dateFormatter.format(new Date(date));
    }

    public int getUnits(int units) {
        return (int) TimeUnit.MILLISECONDS.toSeconds(units);
    }

    public @NotNull Collection<RepoType> repoTypes() {
        return RepoType.values;
    }

    public @NotNull Collection<ServiceType> serviceTypes() {
        return ServiceType.values;
    }

    public @NotNull RepoType singleRepoType() {
        return RepoType.SingleRepository;
    }

    public @NotNull UrlActions urlActions() {
        return UrlActions.instance;
    }
}

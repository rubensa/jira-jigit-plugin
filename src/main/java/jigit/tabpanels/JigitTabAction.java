package jigit.tabpanels;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import jigit.ao.CommitManager;
import jigit.common.CommitActionHelper;
import jigit.common.CommitDateHelper;
import jigit.common.JigitDateFormatter;
import jigit.common.UrlActions;
import jigit.entities.Commit;
import jigit.indexer.repository.GroupRepoName;
import jigit.settings.JigitSettingsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class JigitTabAction extends AbstractIssueAction {
    @NotNull
    private final JigitDateFormatter dateFormatter;
    @NotNull
    private final JigitSettingsManager settingsManager;
    @NotNull
    private final CommitManager commitManager;
    @NotNull
    private final Issue issue;

    public JigitTabAction(@NotNull IssueTabPanelModuleDescriptor descriptor,
                          @NotNull CommitManager commitManager,
                          @NotNull JigitSettingsManager settingsManager,
                          @NotNull JigitDateFormatter dateFormatter,
                          @NotNull Issue issue) {
        super(descriptor);
        this.commitManager = commitManager;
        this.settingsManager = settingsManager;
        this.issue = issue;
        this.dateFormatter = dateFormatter;
    }

    @SuppressWarnings({"unchecked", "InstantiationOfUtilityClass"})
    @Override
    protected void populateVelocityParams(@SuppressWarnings("rawtypes") @NotNull Map map) {
        map.put("dateFormatter", dateFormatter);
        final List<Commit> commits = commitManager.getCommits(issue);
        Collections.sort(commits, new Comparator<Commit>() {
            @Override
            public int compare(@NotNull Commit commit1, @NotNull Commit commit2) {
                return commit2.getCreatedAt().compareTo(commit1.getCreatedAt());
            }
        });
        map.put("commits", commits);
        map.put("repos", Collections.unmodifiableMap(settingsManager.getJigitRepos()));
        map.put("commitActionHelper", new CommitActionHelper());
        map.put("commitDateHelper", new CommitDateHelper());
        map.put("groupRepoNaming", GroupRepoName.Rule);
        map.put("urlActions", UrlActions.instance);
    }

    @Nullable
    @Override
    public Date getTimePerformed() {
        return null;
    }
}

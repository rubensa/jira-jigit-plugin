package jigit.tabpanels;

import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.user.ApplicationUser;
import jigit.ao.CommitManager;
import jigit.entities.Commit;
import jigit.settings.JigitRepo;
import jigit.settings.JigitSettingsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class JigitTabPanel extends AbstractIssueTabPanel {
    private static final @NotNull
    Comparator<Commit> FROM_THE_FIRST_TO_THE_LAST = new Comparator<Commit>() {
        @Override
        public int compare(@NotNull Commit commit1, @NotNull Commit commit2) {
            return commit1.getCreatedAt().compareTo(commit2.getCreatedAt());
        }
    };

    @NotNull
    private final JigitSettingsManager settingsManager;
    @NotNull
    private final CommitManager commitManager;
    @NotNull
    private final DateTimeFormatter dateTimeFormatter;

    public JigitTabPanel(@NotNull JigitSettingsManager jigitSettingsManager,
                         @NotNull CommitManager commitManager,
                         @NotNull DateTimeFormatter dateTimeFormatter) {
        this.settingsManager = jigitSettingsManager;
        this.commitManager = commitManager;
        this.dateTimeFormatter = dateTimeFormatter;
    }

    @Override
    @NotNull
    public List<IssueAction> getActions(@NotNull Issue issue, @Nullable ApplicationUser user) {
        final Map<String, JigitRepo> jigitRepos = Collections.unmodifiableMap(settingsManager.getJigitRepos());
        final List<Commit> commits = commitManager.getCommits(issue);
        if (commits.isEmpty()) {
            return Collections.<IssueAction>singletonList(
                    new GenericMessageAction(descriptor.getI18nBean().getText("jigit-tab-panel.no.commits.to.display")));
        }

        Collections.sort(commits, FROM_THE_FIRST_TO_THE_LAST);

        final ArrayList<IssueAction> issueActions = new ArrayList<>(commits.size());
        for (Commit commit : commits) {
            issueActions.add(new JigitTabAction(descriptor, dateTimeFormatter.forLoggedInUser(), commit, jigitRepos));
        }

        return issueActions;
    }

    @Override
    public boolean showPanel(@NotNull Issue issue, @Nullable ApplicationUser user) {
        return true;
    }
}

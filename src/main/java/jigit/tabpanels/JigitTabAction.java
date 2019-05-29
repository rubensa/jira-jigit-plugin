package jigit.tabpanels;

import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import jigit.common.CommitActionHelper;
import jigit.common.CommitDateHelper;
import jigit.common.UrlActions;
import jigit.entities.Commit;
import jigit.indexer.repository.GroupRepoName;
import jigit.settings.JigitRepo;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Map;

public final class JigitTabAction extends AbstractIssueAction {
    @NotNull
    private final DateTimeFormatter dateTimeFormatter;
    @NotNull
    private final Commit commit;
    @NotNull
    private final Map<String, JigitRepo> jigitRepos;

    public JigitTabAction(@NotNull IssueTabPanelModuleDescriptor descriptor,
                          @NotNull DateTimeFormatter dateTimeFormatter,
                          @NotNull Commit commit,
                          @NotNull Map<String, JigitRepo> jigitRepos) {
        super(descriptor);
        this.dateTimeFormatter = dateTimeFormatter;
        this.commit = commit;
        this.jigitRepos = jigitRepos;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void populateVelocityParams(@SuppressWarnings("rawtypes") @NotNull Map map) {
        map.put("iso8601Formatter", dateTimeFormatter.withStyle(DateTimeStyle.ISO_8601_DATE_TIME));
        map.put("prettyFormatter", dateTimeFormatter.withStyle(DateTimeStyle.COMPLETE));
        map.put("commit", commit);
        map.put("repos", jigitRepos);
        map.put("commitActionHelper", CommitActionHelper.Instance);
        map.put("commitDateHelper", new CommitDateHelper());
        map.put("groupRepoNaming", GroupRepoName.Rule);
        map.put("urlActions", UrlActions.instance);
    }

    @NotNull
    @Override
    public Date getTimePerformed() {
        return commit.getCreatedAt();
    }
}

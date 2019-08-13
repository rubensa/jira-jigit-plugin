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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

public final class JigitTabAction extends AbstractIssueAction {
    @NotNull
    private static final Logger log = LoggerFactory.getLogger(JigitTabAction.class);
    @NotNull
    private final DateTimeFormatter dateTimeFormatter;
    @NotNull
    private final Commit commit;
    @NotNull
    private final Map<String, JigitRepo> jigitRepos;
    @NotNull
    private Date localCommitDate;

    public JigitTabAction(@NotNull IssueTabPanelModuleDescriptor descriptor,
                          @NotNull DateTimeFormatter dateTimeFormatter,
                          @NotNull Commit commit,
                          @NotNull Map<String, JigitRepo> jigitRepos) {
        super(descriptor);
        this.dateTimeFormatter = dateTimeFormatter;
        this.commit = commit;
        this.jigitRepos = jigitRepos;
        final Date commitDate = commit.getCreatedAt();
        try {
            localCommitDate = CommitDateHelper.Instance.toLocal(commitDate);
        } catch (ParseException e) {
            log.error("Couldn't parse date " + commitDate + " to local format", e);
            localCommitDate = commitDate;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void populateVelocityParams(@SuppressWarnings("rawtypes") @NotNull Map map) {
        map.put("iso8601Formatter", dateTimeFormatter.withStyle(DateTimeStyle.ISO_8601_DATE_TIME));
        map.put("prettyFormatter", dateTimeFormatter.withStyle(DateTimeStyle.COMPLETE));
        map.put("commit", commit);
        map.put("repos", jigitRepos);
        map.put("commitActionHelper", CommitActionHelper.Instance);
        map.put("commitDate", localCommitDate);
        map.put("groupRepoNaming", GroupRepoName.Rule);
        map.put("urlActions", UrlActions.instance);
    }

    @NotNull
    @Override
    public Date getTimePerformed() {
        return localCommitDate;
    }
}

package jigit.ao;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import jigit.common.CommitDateHelper;
import jigit.entities.Commit;
import jigit.entities.CommitDiff;
import jigit.entities.CommitIssue;
import jigit.indexer.api.CommitAdapter;
import jigit.indexer.api.CommitFileAdapter;
import net.java.ao.DBParam;
import net.java.ao.Query;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.*;

public final class CommitManagerImpl implements CommitManager {
    private static final int DELETE_BY = 500;
    @NotNull
    private static final String EMPTY_COMMIT_MESSAGE_PLACEHOLDER = "<empty message>";
    @NotNull
    private static final String EMPTY_AUTHOR_PLACEHOLDER = "<empty author>";

    @NotNull
    private final ActiveObjects ao;
    @NotNull
    private final ChangeHistoryManager changeHistoryManager;

    public CommitManagerImpl(@NotNull ActiveObjects ao,
                             @NotNull ChangeHistoryManager changeHistoryManager) {
        this.ao = ao;
        this.changeHistoryManager = changeHistoryManager;
    }

    @Override
    public boolean isExists(@NotNull String repoName, @NotNull String commitSha1) {
        return ao.count(Commit.class, "REPO_NAME = ? AND COMMIT_SHA1 = ?", repoName, commitSha1) > 0;
    }

    @Nullable
    private Commit get(@NotNull String repoName, @NotNull String commitSha1) {
        final Commit[] commits = ao.find(Commit.class, "REPO_NAME = ? AND COMMIT_SHA1 = ?", repoName, commitSha1);
        if (commits.length == 0) {
            return null;
        }
        return commits[0];
    }

    @Override
    public int getCommitCount(@NotNull String repoName, @NotNull String branchName) {
        return ao.count(Commit.class, "REPO_NAME = ? AND BRANCH = ?", repoName, branchName);
    }

    @Override
    public int getCommitCount(@NotNull String repoGroupName) {
        return ao.count(Commit.class, "REPO_GROUP = ?", repoGroupName);
    }

    @SuppressWarnings("MethodWithTooManyParameters")
    @Override
    @NotNull
    public Commit create(@NotNull String commitSha1, @NotNull String title, @NotNull String author,
                         @NotNull Date createdAt, @Nullable String repoGroupName, @NotNull String repoName, @NotNull String branchName,
                         boolean firstCommit) {
        return ao.create(Commit.class,
                new DBParam("COMMIT_SHA1", commitSha1),
                new DBParam("TITLE", StringUtils.isBlank(title) ? EMPTY_COMMIT_MESSAGE_PLACEHOLDER : title),
                new DBParam("AUTHOR", StringUtils.isBlank(author) ? EMPTY_AUTHOR_PLACEHOLDER : author),
                new DBParam("CREATED_AT", createdAt),
                new DBParam("FIRST_COMMIT", firstCommit),
                new DBParam("REPO_GROUP", repoGroupName),
                new DBParam("REPO_NAME", repoName),
                new DBParam("BRANCH", branchName));
    }

    @Override
    @Nullable
    public Commit getLastIndexed(@NotNull String repoName, @NotNull String branch) {
        final Query lastIndexedQuery = Query.
                select().
                where("REPO_NAME = ? AND BRANCH = ?", repoName, branch).
                order("CREATED_AT DESC").
                limit(1);
        final Commit[] commits = ao.find(Commit.class, lastIndexedQuery);
        if (commits.length > 0) {
            return commits[0];
        }

        return null;
    }

    @Override
    @NotNull
    public List<Commit> getCommits(@NotNull Issue issue) {
        final Collection<String> issueKeys = new ArrayList<>();
        //TODO revise deprecated
        issueKeys.addAll(changeHistoryManager.getPreviousIssueKeys(issue.getId()));
        issueKeys.add(issue.getKey());

        return getCommits(issueKeys);
    }

    @Override
    @NotNull
    public List<Commit> getCommits(@NotNull Collection<String> issueKeys) {
        final String issueKeysPlaceholder = Joiner
                .on(", ")
                .join(Iterables.transform(issueKeys, Functions.constant("?")));

        final Commit[] commits = ao.find(Commit.class, Query.
                select().
                alias(Commit.class, "commits").alias(CommitIssue.class, "commit_issue").
                join(CommitIssue.class, "commits.ID = commit_issue.COMMIT_ID").
                where("commit_issue.ISSUE_KEY IN (" + issueKeysPlaceholder + ")", issueKeys.toArray())
        );

        //can't use select().distinct() in the query above, because commit.TITLE field has CLOB data type in Oracle,
        //and it doesn't allow distinct clause. So, there are two ways: use two queries or exclude duplicate by hands.
        return excludeDuplicates(commits);
    }

    @NotNull
    private List<Commit> excludeDuplicates(@NotNull Commit[] commits) {
        final Map<Integer, Commit> result = new HashMap<>(commits.length);
        for (Commit commit : commits) {
            result.put(commit.getID(), commit);
        }
        return new ArrayList<>(result.values());
    }

    @Override
    @Transactional
    public void removeCommits(@NotNull String repoName, @NotNull String branch) {
        final Commit[] commits =
                ao.find(Commit.class, Query.select().where("REPO_NAME = ? AND BRANCH = ?", repoName, branch));
        int counter = 0;
        final Collection<Integer> ids = new ArrayList<>(DELETE_BY);
        for (Commit commit : commits) {
            counter++;
            ids.add(commit.getID());
            if (counter % DELETE_BY == 0) {
                deleteDependent(ids);
                ids.clear();
            }
        }
        if (!ids.isEmpty()) {
            deleteDependent(ids);
        }

        ao.deleteWithSQL(Commit.class, "REPO_NAME = ? AND BRANCH = ?", repoName, branch);
    }

    private void deleteDependent(@NotNull Collection<Integer> ids) {
        final String idsPlaceholder = Joiner
                .on(", ")
                .join(Iterables.transform(ids, Functions.constant("?")));
        final Object[] fk = ids.toArray();
        ao.deleteWithSQL(CommitDiff.class, "COMMIT_ID IN (" + idsPlaceholder + ")", fk);
        ao.deleteWithSQL(CommitIssue.class, "COMMIT_ID IN (" + idsPlaceholder + ")", fk);
    }

    @Override
    @NotNull
    @Transactional
    public Commit persist(@NotNull CommitAdapter commitAdapter,
                          @Nullable String repoGroupName,
                          @NotNull String repoName,
                          @NotNull String branchName,
                          @NotNull Collection<String> issueKeys,
                          @NotNull Collection<CommitFileAdapter> commitFileAdapters) throws ParseException {
        //TODO check ao.executeInTransaction https://bitbucket.org/acourtis/rdbms-plugin-examples
        final boolean firstCommit = commitAdapter.getParentSha1s().isEmpty();
        final Date createdAt = CommitDateHelper.Instance.toUTC(commitAdapter.getCreatedAt());
        final Commit commit = create(commitAdapter.getCommitSha1(), commitAdapter.getTitle(), commitAdapter.getAuthorName(),
                createdAt, repoGroupName, repoName, branchName, firstCommit);

        if (issueKeys.isEmpty()) {
            return commit;
        }

        final int commitId = commit.getID();
        createCommitIssues(commitId, issueKeys);
        createCommitDiffs(commitId, commitFileAdapters);

        return commit;
    }

    @Override
    @Transactional
    public void persistDependent(@NotNull CommitAdapter commitAdapter,
                                 @NotNull String repoName,
                                 @NotNull String branchName,
                                 @NotNull Collection<String> issueKeys,
                                 @NotNull Collection<CommitFileAdapter> commitFileAdapters) {
        if (issueKeys.isEmpty()) {
            return;
        }
        final Commit commit = get(repoName, commitAdapter.getCommitSha1());
        if (commit == null) {
            return;
        }

        ao.delete(commit.getCommitDiffs());
        ao.delete(commit.getCommitIssues());

        final int commitId = commit.getID();
        createCommitIssues(commitId, issueKeys);
        createCommitDiffs(commitId, commitFileAdapters);
    }

    private void createCommitIssues(int commitId, @NotNull Collection<String> issueKeys) {
        for (String issueKey : issueKeys) {
            createCommitIssue(commitId, issueKey);
        }
    }

    private void createCommitIssue(int commitId, @NotNull String issueKey) {
        ao.create(CommitIssue.class,
                new DBParam("COMMIT_ID", commitId),
                new DBParam("ISSUE_KEY", issueKey));
    }

    private void createCommitDiffs(int commitId, @NotNull Collection<CommitFileAdapter> commitFileAdapters) {
        for (CommitFileAdapter commitFileAdapter : commitFileAdapters) {
            if (commitFileAdapter.getNewPath().isEmpty() &&
                    (commitFileAdapter.getOldPath() == null || commitFileAdapter.getOldPath().isEmpty())) {
                continue;
            }
            createCommitDiff(commitId, commitFileAdapter);
        }
    }

    private void createCommitDiff(int commitId, @NotNull CommitFileAdapter commitFileAdapter) {
        final String newPath = commitFileAdapter.getNewPath();
        final String oldPath = commitFileAdapter.getOldPath();
        ao.create(CommitDiff.class,
                new DBParam("COMMIT_ID", commitId),
                new DBParam("NEW_PATH", newPath),
                new DBParam("OLD_PATH", newPath.equals(oldPath) ? null : oldPath),
                new DBParam("ACTION_ID", commitFileAdapter.getCommitAction().getId()));
    }
}

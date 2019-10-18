package jigit.ao;

import jigit.DBTester;
import jigit.client.github.dto.GitHubAuthor;
import jigit.client.github.dto.GitHubCommit;
import jigit.entities.Commit;
import jigit.entities.CommitDiff;
import jigit.entities.CommitIssue;
import jigit.indexer.api.CommitFileAdapter;
import jigit.indexer.api.github.GithubCommitAdapter;
import jigit.indexer.api.github.GithubCommitFileAdapter;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;


public final class CommitManagerTest extends DBTester {
    @Test
    public void readCommits() throws ParseException {
        final String issueKey1 = "KEY-11";
        final String issueKey2 = "KEY-12";
        final String issueKey3 = "KEY-14";
        createCommitStuff("master", Arrays.asList(issueKey1, issueKey2));
        createCommitStuff("master", Arrays.asList(issueKey1, issueKey3));

        final List<Commit> commits = getCommitManager().getCommits(singletonList(issueKey1));
        assertEquals(2, commits.size());
        for (Commit commit : commits) {
            assertEquals(1, commit.getCommitDiffs().length);
            assertEquals(2, commit.getCommitIssues().length);
        }

        final List<Commit> commits2 = getCommitManager().getCommits(singletonList(issueKey2));
        assertEquals(1, commits2.size());
        final Commit commit2 = commits.get(0);
        assertEquals(1, commit2.getCommitDiffs().length);
        assertEquals(2, commit2.getCommitIssues().length);

        final List<Commit> commits3 = getCommitManager().getCommits(singletonList("KEY-1"));
        assertEquals(0, commits3.size());

        final List<Commit> commits4 = getCommitManager().getCommits(Arrays.asList(issueKey1, issueKey2));
        assertEquals(2, commits4.size());
        for (Commit commit : commits) {
            assertEquals(1, commit.getCommitDiffs().length);
            assertEquals(2, commit.getCommitIssues().length);
        }
    }

    @Test
    public void emptyCommitMessage() throws ParseException {
        final String issueKey = "KEY-1";
        createCommitStuff(UUID.randomUUID().toString(), "master", singletonList(issueKey), "", "me");
        final List<Commit> commits = getCommitManager().getCommits(singletonList(issueKey));
        assertEquals(1, commits.size());
    }

    @Test
    public void aCommitWithAnEmptyAuthorIsSaved() throws ParseException {
        final String issueKey = "KEY-1";
        createCommitStuff(UUID.randomUUID().toString(), "master", singletonList(issueKey), "", "");
        final List<Commit> commits = getCommitManager().getCommits(singletonList(issueKey));
        assertEquals(1, commits.size());
    }

    @Test
    public void clearRepo() throws ParseException, SQLException {
        final String master = "master";
        final String issueKey1 = "KEY-1";
        createCommitStuff(master, singletonList(issueKey1));
        final String branch = "feature_1";
        final String issueKey2 = "KEY-1";
        createCommitStuff(branch, singletonList(issueKey2));
        assertEquals(1, getCommitManager().getCommitCount(REPO_NAME, master));
        assertEquals(1, getCommitManager().getCommitCount(REPO_NAME, branch));

        getCommitManager().removeCommits(REPO_NAME, master);
        assertEquals(1, getEntityManager().count(Commit.class));
        assertEquals(1, getEntityManager().count(CommitIssue.class));
        assertEquals(1, getEntityManager().count(CommitDiff.class));

        getCommitManager().removeCommits(REPO_NAME, branch);
        assertEquals(0, getEntityManager().count(Commit.class));
        assertEquals(0, getEntityManager().count(CommitIssue.class));
        assertEquals(0, getEntityManager().count(CommitDiff.class));
    }

    @Test
    public void emptyCommitDiffIsIgnored() throws ParseException, SQLException {
        final GitHubCommit.CommitInfo commitInfo = new GitHubCommit.CommitInfo(
                new GitHubAuthor("me", "2016/01/02 12:00:00 UTC"), "whatever");
        final GitHubCommit gitHubCommit = new GitHubCommit(UUID.randomUUID().toString(), commitInfo,
                Collections.<GitHubCommit.ParentCommit>emptyList(), Collections.<GitHubCommit.File>emptyList());
        final GitHubCommit.File gitHubFile = new GitHubCommit.File("", "", "");
        final CommitFileAdapter githubCommitFileAdapter = new GithubCommitFileAdapter(gitHubFile);
        getCommitManager().persist(new GithubCommitAdapter(gitHubCommit), GROUP_NAME, REPO_NAME, "master",
                singletonList("KEY-1"), singletonList(githubCommitFileAdapter));

        assertEquals(1, getEntityManager().count(Commit.class));
        assertEquals(1, getEntityManager().count(CommitIssue.class));
        assertEquals(0, getEntityManager().count(CommitDiff.class));
    }

    private void createCommitStuff(@NotNull String branch, @NotNull List<String> issueKeys) throws ParseException {
        final String sha1 = UUID.randomUUID().toString();
        createCommitStuff(sha1, branch, issueKeys, "Commit message for sha=" + sha1, "me");
    }

    private void createCommitStuff(@NotNull String sha1,
                                   @NotNull String branch,
                                   @NotNull List<String> issueKeys,
                                   @NotNull String message,
                                   @NotNull String author) throws ParseException {
        final GitHubCommit.CommitInfo commitInfo = new GitHubCommit.CommitInfo(
                new GitHubAuthor(author, "2016/01/02 12:00:00 UTC"), message);
        final GitHubCommit gitHubCommit = new GitHubCommit(sha1, commitInfo,
                Collections.<GitHubCommit.ParentCommit>emptyList(), Collections.<GitHubCommit.File>emptyList());
        final GitHubCommit.File gitHubFile = new GitHubCommit.File("changed", "MyClass.java", "MyClass.java");
        final CommitFileAdapter githubCommitFileAdapter = new GithubCommitFileAdapter(gitHubFile);
        getCommitManager().persist(new GithubCommitAdapter(gitHubCommit), GROUP_NAME, REPO_NAME, branch, issueKeys,
                singletonList(githubCommitFileAdapter));
    }

}
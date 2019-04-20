package jigit.indexer;

import com.google.common.collect.Sets;
import jigit.DBTester;
import jigit.ao.CommitManager;
import jigit.ao.QueueItemManager;
import jigit.ao.QueueItemManagerImpl;
import jigit.entities.Commit;
import jigit.entities.CommitDiff;
import jigit.entities.CommitIssue;
import jigit.entities.QueueItem;
import jigit.indexer.api.APIAdaptedStub;
import jigit.indexer.api.CommitAdapter;
import jigit.indexer.api.CommitFileAdapter;
import jigit.indexer.api.TestRepoInfoFactory;
import jigit.indexer.repository.RepoInfo;
import jigit.indexer.repository.RepoType;
import jigit.indexer.repository.ServiceType;
import jigit.settings.JigitRepo;
import jigit.settings.JigitSettingsManager;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("ClassWithTooManyFields")
public final class JigitIndexerTest extends DBTester {
    @NotNull
    private static final TreeSet<String> BRANCHES = Sets.newTreeSet(Arrays.asList(APIAdaptedStub.BRANCH1, APIAdaptedStub.BRANCH2));
    @NotNull
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    @NotNull
    private static final JigitRepo SINGLE_JIGIT_REPO = new JigitRepo(REPO_NAME, "url", ServiceType.GitHub, "token",
            RepoType.SingleRepository, "repoId",
            APIAdaptedStub.MASTER, true, 1, (int) TimeUnit.SECONDS.toMillis(1), 2, false, BRANCHES);
    @NotNull
    private static final JigitRepo GROUP_JIGIT_REPO = new JigitRepo(GROUP_NAME, "url", ServiceType.GitLab, "token",
            RepoType.GroupOfRepositories, GROUP_NAME,
            APIAdaptedStub.MASTER, true, 1, (int) TimeUnit.SECONDS.toMillis(1), 2, false, BRANCHES);
    @NotNull
    @Rule
    public final Timeout timeoutRule = new Timeout((int) TimeUnit.SECONDS.toMillis(30));
    @NotNull
    private final JigitSettingsManager jigitSettingsManager = mock(JigitSettingsManager.class);
    @NotNull
    private final IssueKeysExtractor issueKeysExtractor = new TestIssueKeysExtractor();
    @SuppressWarnings("NullableProblems")
    @NotNull
    private JigitIndexer jigitIndexer;
    @SuppressWarnings("NullableProblems")
    @NotNull
    private RepoDataCleaner repoDataCleaner;
    @SuppressWarnings("NullableProblems")
    @NotNull
    private RepoInfo singleRepoInfo;
    @SuppressWarnings("NullableProblems")
    @NotNull
    private RepoInfo groupRepoInfo;
    @SuppressWarnings("NullableProblems")
    @NotNull
    private IndexingWorkerFactory indexingWorkerFactory;

    @AfterClass
    public static void tearDownAll() {
        executorService.shutdown();
    }

    @Override
    @Before
    public void setUp() throws IOException {
        super.setUp();
        final QueueItemManager queueItemManager = new QueueItemManagerImpl(getActiveObjects());
        final CommitManager commitManager = getCommitManager();
        final TestRepoInfoFactory repoInfoFactory = new TestRepoInfoFactory();
        singleRepoInfo = repoInfoFactory.build(SINGLE_JIGIT_REPO).iterator().next();
        groupRepoInfo = repoInfoFactory.build(GROUP_JIGIT_REPO).iterator().next();
        repoDataCleaner = new RepoDataCleaner(commitManager, queueItemManager);
        indexingWorkerFactory = spy(new IndexingWorkerFactoryImpl(repoInfoFactory,
                queueItemManager, new PersistStrategyFactoryImpl(commitManager), commitManager,
                repoDataCleaner, issueKeysExtractor));
        jigitIndexer = new JigitIndexer(jigitSettingsManager, indexingWorkerFactory);
        DisabledRepos.instance.markEnabled(singleRepoInfo.getRepoName());
        DisabledRepos.instance.markEnabled(groupRepoInfo.getRepoName());
    }

    @Test
    public void singleRepoIndexed() {
        indexRepo(Collections.singleton(SINGLE_JIGIT_REPO));
        allCommitsAreIndexed(singleRepoInfo);
    }

    @Test
    public void groupRepoIndexed() {
        indexRepo(Collections.singleton(GROUP_JIGIT_REPO));
        allCommitsAreIndexed(groupRepoInfo);
    }

    @Test
    public void groupRepoIndexedEvenIfSingleRepoProducesException() throws IOException {
        doThrow(new IllegalArgumentException()).when(indexingWorkerFactory).build(eq(SINGLE_JIGIT_REPO));
        indexRepo(Arrays.asList(SINGLE_JIGIT_REPO, GROUP_JIGIT_REPO));
        allCommitsAreIndexed(groupRepoInfo);
    }

    @Test
    public void repoWithWrongBranchNameIndexed() {
        final TreeSet<String> branches = new TreeSet<>(BRANCHES);
        branches.add("a-branch");
        final JigitRepo jigitRepoWithWrongBrachName = new JigitRepo(SINGLE_JIGIT_REPO.getRepoName(),
                SINGLE_JIGIT_REPO.getServerUrl(), ServiceType.GitHub, SINGLE_JIGIT_REPO.getToken(),
                SINGLE_JIGIT_REPO.getRepoType(), SINGLE_JIGIT_REPO.getRepositoryId(),
                SINGLE_JIGIT_REPO.getDefaultBranch(), SINGLE_JIGIT_REPO.isEnabled(), SINGLE_JIGIT_REPO.getRequestTimeout(),
                SINGLE_JIGIT_REPO.getSleepTimeout(), SINGLE_JIGIT_REPO.getSleepRequests(),
                SINGLE_JIGIT_REPO.isIndexAllBranches(), branches);
        indexRepo(Collections.singleton(jigitRepoWithWrongBrachName));
        allCommitsAreIndexed(singleRepoInfo);
    }

    @Test
    public void repoIndexingStopsAfterDisabling() throws SQLException, ExecutionException, InterruptedException {
        final Future<Boolean> futureResult = executorService.submit(new Callable<Boolean>() {
            @NotNull
            @Override
            public Boolean call() {
                try {
                    Thread.sleep(singleRepoInfo.getSleepTimeout());
                } catch (InterruptedException ignored) {
                    fail();
                    return Boolean.FALSE;
                }
                DisabledRepos.instance.markDisabled(singleRepoInfo.getRepoName());
                return Boolean.TRUE;
            }
        });
        indexRepo(Collections.singleton(SINGLE_JIGIT_REPO));
        assertTrue(futureResult.get());
        assertTrue(DisabledRepos.instance.disabled(singleRepoInfo.getRepoName()));
        final int commitsNumber = getEntityManager().count(Commit.class);
        assertTrue("Actual number is " + commitsNumber, commitsNumber < 5);
        assertTrue("Actual number is " + commitsNumber, commitsNumber >= 1);

        final List<Commit> commits4 =
                getCommitManager().getCommits(singletonList(APIAdaptedStub.commit4.getCommitSha1()));
        final Commit commit = assertCommits(singleRepoInfo, commits4, APIAdaptedStub.MASTER);
        assertCommit(commit, APIAdaptedStub.commit4);
    }


    @Test
    public void noRepoDataAfterCleaningSingleRepository() throws SQLException, ExecutionException, InterruptedException {
        noRepoDataAfterCleaning(SINGLE_JIGIT_REPO, singleRepoInfo);
    }

    @Test
    public void noRepoDataAfterCleaningGroupOfRepositories() throws SQLException, ExecutionException, InterruptedException {
        noRepoDataAfterCleaning(GROUP_JIGIT_REPO, groupRepoInfo);
    }

    private void noRepoDataAfterCleaning(final @NotNull JigitRepo jigitRepo,
                                         final @NotNull RepoInfo repoInfo) throws SQLException, ExecutionException, InterruptedException {
        indexRepo(Collections.singleton(jigitRepo));
        final Future<Boolean> futureResult = executorService.submit(new Callable<Boolean>() {
            @NotNull
            @Override
            public Boolean call() {
                try {
                    Thread.sleep(repoInfo.getSleepTimeout());
                    final int commitsNumber = getEntityManager().count(Commit.class);
                    assertTrue("Actual number is " + commitsNumber, commitsNumber >= 1);
                    repoDataCleaner.clearRepoData(Collections.singletonList(repoInfo));
                } catch (InterruptedException | SQLException ignored) {
                    fail();
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
        });
        assertTrue(futureResult.get());
        assertFalse(DisabledRepos.instance.disabled(repoInfo.getRepoName()));
        assertEquals(0, getEntityManager().count(Commit.class));
        assertEquals(0, getEntityManager().count(QueueItem.class));
    }

    private void allCommitsAreIndexed(@NotNull RepoInfo repoInfo) {
        final List<Commit> commits1 =
                getCommitManager().getCommits(singletonList(APIAdaptedStub.commit1.getCommitSha1()));
        final Commit commit = assertCommits(repoInfo, commits1, APIAdaptedStub.MASTER);
        assertCommit(commit, APIAdaptedStub.commit1);

        final List<Commit> commits2 =
                getCommitManager().getCommits(singletonList(APIAdaptedStub.commit2.getCommitSha1()));
        final Commit commit2 = assertCommits(repoInfo, commits2, APIAdaptedStub.MASTER);
        assertCommit(commit2, APIAdaptedStub.commit2);

        final List<Commit> commits3 =
                getCommitManager().getCommits(singletonList(APIAdaptedStub.commit3.getCommitSha1()));
        final Commit commit3 = assertCommits(repoInfo, commits3, APIAdaptedStub.MASTER);
        assertCommit(commit3, APIAdaptedStub.commit3);

        final List<Commit> commits4 =
                getCommitManager().getCommits(singletonList(APIAdaptedStub.commit4.getCommitSha1()));
        final Commit commit4 = assertCommits(repoInfo, commits4, APIAdaptedStub.MASTER);
        assertCommit(commit4, APIAdaptedStub.commit4);

        final List<Commit> commits5 =
                getCommitManager().getCommits(singletonList(APIAdaptedStub.commit5.getCommitSha1()));
        final Commit commit5 = assertCommits(repoInfo, commits5, APIAdaptedStub.BRANCH2);
        assertCommit(commit5, APIAdaptedStub.commit5);
    }

    @NotNull
    private Commit assertCommits(@NotNull RepoInfo repoInfo, @NotNull List<Commit> commits, @NotNull String branchName) {
        assertEquals(1, commits.size());
        final Commit commit = commits.get(0);
        assertEquals(repoInfo.getRepoName(), commit.getRepoName());
        assertEquals(branchName, commit.getBranch());
        if (repoInfo.getRepoGroup() == null) {
            assertNull(commit.getRepoGroup());
        } else {
            assertEquals(repoInfo.getRepoGroup(), commit.getRepoGroup());
        }
        return commit;
    }

    private void assertCommit(@NotNull Commit commit, @NotNull CommitAdapter commitAdapter) {
        assertEquals(commit.getAuthor(), commitAdapter.getAuthorName());
        assertEquals(commit.getCommitSha1(), commitAdapter.getCommitSha1());
        assertEquals(commit.getTitle(), commitAdapter.getTitle());
        assertIssueKeys(commit, commitAdapter);
        try {
            assertDiffs(commit, commitAdapter);
        } catch (IOException ignored) {
            fail();
        }
    }

    private void assertDiffs(@NotNull Commit commit, @NotNull CommitAdapter commitAdapter) throws IOException {
        final CommitDiff[] storedDiffs = commit.getCommitDiffs();
        final Collection<CommitFileAdapter> commitDiffs = commitAdapter.getCommitDiffs();
        assertEquals(commitDiffs.size(), storedDiffs.length);
        assertEquals(1, commitDiffs.size());
        final CommitFileAdapter diff = commitDiffs.iterator().next();
        assertEquals(diff.getCommitAction().getId(), storedDiffs[0].getActionId().intValue());
        assertEquals(diff.getNewPath(), storedDiffs[0].getNewPath());
        assertEquals(diff.getOldPath(), storedDiffs[0].getOldPath());
    }

    private void assertIssueKeys(@NotNull Commit commit, @NotNull CommitAdapter commitAdapter) {
        final List<String> issueKeys = issueKeysExtractor.extract(commitAdapter.getTitle());
        final CommitIssue[] commitIssues = commit.getCommitIssues();
        assertEquals(issueKeys.size(), commitIssues.length);
        for (CommitIssue commitIssue : commitIssues) {
            assertTrue(issueKeys.contains(commitIssue.getIssueKey()));
        }
    }

    private void indexRepo(@NotNull Collection<JigitRepo> repos) {
        final HashMap<String, JigitRepo> settings = new HashMap<>(repos.size());
        for (JigitRepo repo : repos) {
            settings.put(repo.getRepoName(), repo);
        }
        when(jigitSettingsManager.getJigitRepos()).thenReturn(settings);
        jigitIndexer.execute();
    }
}

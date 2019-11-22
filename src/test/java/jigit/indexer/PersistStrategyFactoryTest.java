package jigit.indexer;

import jigit.DBTester;
import jigit.entities.CommitDiff;
import jigit.indexer.api.APIAdaptedStub;
import jigit.indexer.api.CommitAdapter;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.Matchers.containsInAnyOrder;

public final class PersistStrategyFactoryTest extends DBTester {
    @NotNull
    private static final CommitAdapter COMMIT = APIAdaptedStub.commit5;
    @NotNull
    private static final String BRANCH_NAME = "feature-branch";
    @NotNull
    private static final Collection<String> ISSUES = Collections.singleton("KEY-1");

    @SuppressWarnings("NullableProblems")
    @NotNull
    private PersistStrategyFactory persistStrategyFactory;

    @Before
    public void setUpTest() {
        persistStrategyFactory = new PersistStrategyFactoryImpl(getCommitManager());
    }

    @Test
    public void persistenceOfAlreadyIndexedCommitFetchedForTheFirstTimeReturnsNoParentCommits() throws IOException, ParseException {
        //given
        getCommitManager().persist(COMMIT, GROUP_NAME, REPO_NAME, BRANCH_NAME, Collections.<String>emptyList(),
                COMMIT.getCommitDiffs());
        Assert.assertTrue(getCommitManager().isExists(REPO_NAME, COMMIT.getCommitSha1()));
        Assert.assertEquals(0, getActiveObjects().find(CommitDiff.class).length);

        //when
        final Collection<String> persistenceResult = persistStrategyFactory
                .getStrategy(REPO_NAME, COMMIT.getCommitSha1(), true)
                .persist(GROUP_NAME, REPO_NAME, BRANCH_NAME, COMMIT, ISSUES, COMMIT.getCommitDiffs());

        //then
        Assert.assertTrue(persistenceResult.isEmpty());
        Assert.assertEquals(0, getActiveObjects().find(CommitDiff.class).length);
    }

    @Test
    public void persistenceOfAlreadyIndexedCommitFetchedForTheSecondTimeSavesDataAndReturnsParentCommits() throws IOException, ParseException {
        //given
        getCommitManager().persist(COMMIT, GROUP_NAME, REPO_NAME, BRANCH_NAME, ISSUES, COMMIT.getCommitDiffs());
        Assert.assertTrue(getCommitManager().isExists(REPO_NAME, COMMIT.getCommitSha1()));

        //when
        final Collection<String> persistenceResult = persistStrategyFactory
                .getStrategy(REPO_NAME, COMMIT.getCommitSha1(), false)
                .persist(GROUP_NAME, REPO_NAME, BRANCH_NAME, COMMIT, ISSUES, COMMIT.getCommitDiffs());

        //then
        Assert.assertThat(persistenceResult, containsInAnyOrder(COMMIT.getParentSha1s().toArray()));
        Assert.assertEquals(COMMIT.getCommitDiffs().size(), getActiveObjects().find(CommitDiff.class).length);
    }

    @Test
    public void persistenceOfNotIndexedCommitSavesDataAndReturnsParentCommits() throws IOException, ParseException {
        //given
        Assert.assertFalse(getCommitManager().isExists(REPO_NAME, COMMIT.getCommitSha1()));

        //when
        final Collection<String> persistenceResult = persistStrategyFactory
                .getStrategy(REPO_NAME, COMMIT.getCommitSha1(), true)
                .persist(GROUP_NAME, REPO_NAME, BRANCH_NAME, COMMIT, ISSUES, COMMIT.getCommitDiffs());

        //then
        Assert.assertThat(persistenceResult, containsInAnyOrder(COMMIT.getParentSha1s().toArray()));
        Assert.assertTrue(getCommitManager().isExists(REPO_NAME, COMMIT.getCommitSha1()));
        Assert.assertEquals(COMMIT.getCommitDiffs().size(), getActiveObjects().find(CommitDiff.class).length);
    }
}

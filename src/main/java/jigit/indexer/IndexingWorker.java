package jigit.indexer;

import jigit.ao.QueueItemManager;
import jigit.indexer.api.APIAdapter;
import jigit.indexer.api.CommitAdapter;
import jigit.indexer.api.CommitFileAdapter;
import jigit.indexer.api.LimitExceededException;
import jigit.indexer.branch.BranchIndexingMode;
import jigit.indexer.repository.RepoInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Callable;

final class IndexingWorker implements Callable<RepoInfo> {
    @NotNull
    private static final Logger log = LoggerFactory.getLogger(JigitIndexer.class);
    @NotNull
    private final RepoInfo repoInfo;
    @NotNull
    private final DeletingForcePushHandler deletingForcePushHandler;
    @NotNull
    private final QueueItemManager queueItemManager;
    @NotNull
    private final PersistStrategyFactory persistStrategyFactory;
    @NotNull
    private final IssueKeysExtractor issueKeysExtractor;

    IndexingWorker(@NotNull RepoInfo repoInfo,
                   @NotNull DeletingForcePushHandler deletingForcePushHandler,
                   @NotNull QueueItemManager queueItemManager,
                   @NotNull PersistStrategyFactory persistStrategyFactory,
                   @NotNull IssueKeysExtractor issueKeysExtractor) {
        this.repoInfo = repoInfo;
        this.deletingForcePushHandler = deletingForcePushHandler;
        this.queueItemManager = queueItemManager;
        this.persistStrategyFactory = persistStrategyFactory;
        this.issueKeysExtractor = issueKeysExtractor;
    }

    @NotNull
    @Override
    public RepoInfo call() {
        try {
            final Map<String, BranchIndexingMode> branchFPHandlers = getBranchIndexingModes();
            for (Map.Entry<String, BranchIndexingMode> entry : branchFPHandlers.entrySet()) {
                handleBranchMode(entry.getValue(), entry.getKey());
            }
        } catch (LimitExceededException ignored) {
            log.info("Repository request limit exceeded for " + repoInfo.getRepoName()
                    + ". Next indexing will start after " + new Date(repoInfo.getSleepTo()));
        } catch (Throwable t) {
            log.error("Couldn't index repo: " + repoInfo.getRepoName(), t);
        }

        return repoInfo;
    }

    private void handleBranchMode(@NotNull BranchIndexingMode indexingMode,
                                  @NotNull String branch) throws IOException, InterruptedException, ParseException {
        try {
            indexingMode.getForcePushHandler().handle(branch);
            indexRepoBranch(branch);
        } catch (Throwable t) {
            if (indexingMode.isStopIndexingOnException()) {
                throw t;
            } else {
                log.error("Got an error while indexing repository " + repoInfo.getRepoFullName()
                        + " and branch " + branch, t);
            }
        }
    }

    @NotNull
    private Map<String, BranchIndexingMode> getBranchIndexingModes() {
        final Map<String, BranchIndexingMode> branchHandlers = new LinkedHashMap<>();
        branchHandlers.put(repoInfo.getDefaultBranch(), new BranchIndexingMode(true, ForcePushHandler.DO_NOTHING));
        for (String branch : repoInfo.branches()) {
            branchHandlers.put(branch, new BranchIndexingMode(false, deletingForcePushHandler));
        }
        return branchHandlers;
    }

    private void indexRepoBranch(@NotNull String branch) throws IOException, InterruptedException, ParseException {
        final String repositoryId = repoInfo.getRepoFullName();
        try {
            final String startCommitSha1 = repoInfo.getApiAdapter().getHeadCommitSha1(branch);
            if (startCommitSha1 == null) {
                return;
            }

            log.debug("Indexing of repository " + repositoryId + " and branch " + branch +
                    " started from head commit " + startCommitSha1 + '.');
            indexFromCommit(branch, startCommitSha1);
            log.debug("Indexing of repository " + repositoryId + " and branch " + branch + " finished.");
        } catch (SocketTimeoutException e) {
            log.warn("SocketTimeoutException occurred when trying to get head commit from "
                    + repositoryId + " branch " + branch, e);
        }
    }

    private void indexFromCommit(@NotNull String branch, @NotNull String startCommitSha1) throws IOException, InterruptedException, ParseException {
        final String repositoryId = repoInfo.getRepoFullName();
        final String repoName = repoInfo.getRepoName();
        final CommitQueue commitQueue = new CommitQueue(queueItemManager, repoName, branch);
        final int skipCount = commitQueue.size();
        commitQueue.add(startCommitSha1);

        int counter = 0;
        while (!(commitQueue.isEmpty() || DisabledRepos.instance.disabled(repoName))) {
            final APIAdapter apiAdapter = repoInfo.getApiAdapter();
            if (isTimeToSleep(apiAdapter.getRequestsQuantity())) {
                Thread.sleep(repoInfo.getSleepTimeout());
            }
            counter++;

            final String commitSha1 = commitQueue.peek();
            if (commitSha1 == null) {
                continue;
            }
            log.debug("Commit " + commitSha1 + " fetched.");

            final CommitAdapter commitAdapter;
            try {
                commitAdapter = apiAdapter.getCommit(commitSha1);
            } catch (SocketTimeoutException e) {
                log.warn("SocketTimeoutException occurred when trying to get commit " + commitSha1 +
                        " from " + repositoryId + " branch " + branch, e);
                return;
            }

            Collection<CommitFileAdapter> commitDiffs = Collections.emptyList();
            final Set<String> issueKeys =
                    new HashSet<>(issueKeysExtractor.extract(commitAdapter.getTitle()));

            if (!issueKeys.isEmpty()) {
                try {
                    commitDiffs = commitAdapter.getCommitDiffs();
                } catch (SocketTimeoutException e) {
                    log.warn("SocketTimeoutException occurred when trying to get diff " + commitSha1 +
                            " from " + repositoryId + " branch " + branch, e);
                }
            }

            final Collection<String> nextCommits = persistStrategyFactory.
                    getStrategy(repoName, commitSha1, counter > skipCount).
                    persist(repoInfo.getRepoGroup(), repoName, branch, commitAdapter, issueKeys, commitDiffs);
            commitQueue.remove();
            commitQueue.addAll(nextCommits);
        }
        if (DisabledRepos.instance.disabled(repoName)) {
            throw new InterruptedException("Indexing was interrupted from the outside.");
        }
    }

    private boolean isTimeToSleep(long requestsQuantity) {
        return requestsQuantity > 0 && repoInfo.getSleepRequests() > 0 && requestsQuantity % repoInfo.getSleepRequests() == 0;
    }
}

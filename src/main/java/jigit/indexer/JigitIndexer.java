package jigit.indexer;

import jigit.indexer.repository.RepoInfo;
import jigit.settings.JigitRepo;
import jigit.settings.JigitSettingsManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class JigitIndexer {
    @NotNull
    private static final Logger log = Logger.getLogger(JigitIndexer.class);
    private static final int THREAD_POOL_SIZE = 2;
    @NotNull
    private final JigitSettingsManager settingsManager;
    @NotNull
    private final IndexingWorkerFactory indexingWorkerFactory;

    public JigitIndexer(@NotNull JigitSettingsManager settingsManager,
                        @NotNull IndexingWorkerFactory indexingWorkerFactory) {
        this.settingsManager = settingsManager;
        this.indexingWorkerFactory = indexingWorkerFactory;
    }

    public void execute() {
        final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE, new JigitThreadFactory());
        final CompletionService<RepoInfo> completionService = new ExecutorCompletionService<>(executorService);
        final int tasksCount = tasks(completionService).size();
        try {
            for (int i = 0; i < tasksCount; i++) {
                final Future<RepoInfo> projectCompleted = completionService.take();
                projectCompleted.get();
            }
        } catch (InterruptedException e) {
            log.error("Internal error", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Internal error", e.getCause());
        } finally {
            executorService.shutdown();
        }
    }

    @NotNull
    private Collection<Future<RepoInfo>> tasks(@NotNull CompletionService<RepoInfo> completionService) {
        final Collection<Future<RepoInfo>> tasks = new ArrayList<>();
        final Map<String, JigitRepo> jigitRepos = settingsManager.getJigitRepos();
        for (JigitRepo repo : jigitRepos.values()) {
            if (!repo.isNeedToIndex()) {
                continue;
            }
            try {
                for (IndexingWorker indexingWorker : indexingWorkerFactory.build(repo)) {
                    tasks.add(completionService.submit(indexingWorker));
                }
            } catch (Throwable t) {
                log.error("Couldn't index repo: " + repo.getRepoName(), t);
            }
        }
        return tasks;
    }

    private static final class JigitThreadFactory implements ThreadFactory {
        @NotNull
        private final AtomicInteger counter = new AtomicInteger(0);

        @NotNull
        @Override
        public Thread newThread(@NotNull Runnable r) {
            return new Thread(r, "jigit-indexer-" + counter.incrementAndGet());
        }
    }
}

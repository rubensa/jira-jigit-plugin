package jigit.indexer;

import jigit.indexer.repository.RepoInfo;
import jigit.settings.JigitRepo;
import jigit.settings.JigitSettingsManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class JigitIndexer {
    @NotNull
    private static final Logger LOG = Logger.getLogger(JigitIndexer.class);
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
        final Map<String, JigitRepo> jigitRepos = settingsManager.getJigitRepos();
        int futureTasks = 0;

        try {
            for (JigitRepo repo : jigitRepos.values()) {
                if (!repo.isNeedToIndex()) {
                    continue;
                }
                try {
                    for (IndexingWorker indexingWorker : indexingWorkerFactory.build(repo)) {
                        completionService.submit(indexingWorker);
                        futureTasks++;
                    }
                } catch(FileNotFoundException e){
                    LOG.error("JigitIndexer::execute: FileNotFoundException: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LOG.error("JigitIndexer::execute", e);
        }

        try {
            for (int i = 0; i < futureTasks; i++) {
                final Future<RepoInfo> projectCompleted = completionService.take();
                projectCompleted.get();
            }
        } catch (InterruptedException e) {
            LOG.error("JigitIndexer::execute - InterruptedException", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOG.error("JigitIndexer::execute - ExecutionException. Cause: ", e.getCause());
        } finally {
            executorService.shutdown();
        }
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

package pl.laskowski.marcin.solving.bruteforce;

import pl.laskowski.marcin.model.Field;
import pl.laskowski.marcin.model.Region;
import pl.laskowski.marcin.model.Sudoku;
import pl.laskowski.marcin.model.SudokuHintGrid;
import pl.laskowski.marcin.solving.ConcurrentSudokuSolver;
import pl.laskowski.marcin.solving.SudokuSolver;
import pl.laskowski.marcin.type.SudokuVariant;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BruteForceConcurrentSolver2 implements ConcurrentSudokuSolver {

    private final SudokuSolver coreAlgorithm;
    private final ThreadPoolExecutor executors;
    private int sampleSize;

    public BruteForceConcurrentSolver2(SudokuVariant sudokuVariant, int threads, int sampleSize) {
        if (threads < 1) throw new IllegalArgumentException("There must be at least one thread");
        this.coreAlgorithm = new BruteForceSolver(sudokuVariant);
        this.executors = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
        this.sampleSize = sampleSize;
    }

    @Override
    public SudokuVariant getSudokuVariant() {
        return coreAlgorithm.getSudokuVariant();
    }

    @Override
    public Sudoku solve(Sudoku sudoku) {
        try {
            BlockingQueue<Sudoku> queue = new LinkedBlockingQueue<>();
            queue.add(sudoku);
            insertSamples(queue);
            return solveInParallel(queue);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (UnexpectedSuccessException e) {
            return e.getSolution();
        }
    }

    private void insertSamples(BlockingQueue<Sudoku> queue) throws InterruptedException {
        while (queue.size() < sampleSize) {
            Sudoku sample = queue.take();
            List<Sudoku> dividedSample = divide(sample);
            queue.addAll(dividedSample);
        }
    }

    private List<Sudoku> divide(Sudoku sample) {
        Field field = sample.getFirstEmptyField();
        if (field != null) {
            return IntStream.range(1, getSudokuVariant().regionSize())
                    .mapToObj(newValue -> {
                        Sudoku copy = sample.copy();
                        copy.at(field.position()).set(newValue);
                        return copy;
                    })
                    .collect(Collectors.toList());
        } else {
            throw new UnexpectedSuccessException(sample);
        }
    }

    private Sudoku solveInParallel(BlockingQueue<Sudoku> queue) throws InterruptedException, ExecutionException {
        ExecutorCompletionService<Sudoku> service = new ExecutorCompletionService<>(executors);
        List<Future<Sudoku>> futures = IntStream.range(0, executors.getMaximumPoolSize())
                .mapToObj(i -> solveTask(queue))
                .map(service::submit)
                .collect(Collectors.toList());
        try {
            return service.take().get();
        } finally {
            for (Future f : futures) f.cancel(true);
        }
    }

    private Callable<Sudoku> solveTask(BlockingQueue<Sudoku> queue) {
        return () -> {
            while (!Thread.interrupted()) {
                Sudoku sudoku = queue.take();
                if (!isValid(sudoku)) continue;
                Sudoku result = coreAlgorithm.solve(sudoku);
                if (result.isSolved()) return result;
            }
            return null;
        };
    }

    @Override
    public void shutdown() {
        try {
            this.executors.shutdownNow();
            this.executors.awaitTermination(1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //ignore
        }
    }

    private boolean isValid(Sudoku s) {
        for (Region r: getSudokuVariant().divideIntoRegions(s)) {
            if (!r.isValid()) return false;
        }
        return true;
    }

}

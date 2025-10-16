/*
 * Java 21 - Efficient Folder Generator Utility
 * (Virtual Threads + CompletableFuture + Single-line Progress + ETA + CLI Parameters + Elapsed Time + Unique Filenames)
 *
 * Generates multiple folders each with a unique file. Supports command-line arguments,
 * a `--help` flag to display usage, shows progress, ETA, total elapsed time, and ensures unique filenames.
 */

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class FolderGeneratorV1 {

    private static final byte[] FILE_PATTERN = "This is a small generated file.\n".getBytes(StandardCharsets.UTF_8);

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || Arrays.stream(args).anyMatch(a -> a.equals("--help"))) {
            printUsage();
            return;
        }

        // Defaults
        String baseDirStr = "output";
        int count = 100;
        String prefix = "folder-";
        String fileNameBase = "file";
        int minSizeKB = 50;
        int maxSizeKB = 500;
        boolean overwrite = false;

        // Parse CLI arguments
        for (String arg : args) {
            if (arg.startsWith("--baseDir=")) baseDirStr = arg.substring("--baseDir=".length());
            else if (arg.startsWith("--count=")) count = Integer.parseInt(arg.substring("--count=".length()));
            else if (arg.startsWith("--prefix=")) prefix = arg.substring("--prefix=".length());
            else if (arg.startsWith("--fileName=")) fileNameBase = arg.substring("--fileName=".length());
            else if (arg.startsWith("--min-size=")) minSizeKB = parseSize(arg.substring("--min-size=".length()));
            else if (arg.startsWith("--max-size=")) maxSizeKB = parseSize(arg.substring("--max-size=".length()));
            else if (arg.equals("--overwrite")) overwrite = true;
        }

        if (minSizeKB > maxSizeKB) { int tmp = minSizeKB; minSizeKB = maxSizeKB; maxSizeKB = tmp; }

        Path baseDir = Path.of(baseDirStr);
        Files.createDirectories(baseDir);

        AtomicInteger completed = new AtomicInteger(0);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Instant start = Instant.now();

        List<CompletableFuture<Void>> allFutures = new ArrayList<>();
        final int totalCount = count;
        for (int i = 1; i <= count; i++) {
            final int index = i;
            final String folderPrefix = prefix;
            final String fNameBase = fileNameBase;
            final int minSize = minSizeKB;
            final int maxSize = maxSizeKB;
            final boolean overwriteFile = overwrite;

            allFutures.add(CompletableFuture.runAsync(() -> {
                try {
                    // Generate unique filename by appending folder index
                    String uniqueFileName = fNameBase + "_" + index + ".txt";
                    createFolderAndFile(baseDir, folderPrefix, uniqueFileName, randomSize(minSize, maxSize), overwriteFile, index);

                    int done = completed.incrementAndGet();
                    double percent = (done * 100.0) / totalCount;
                    Duration elapsed = Duration.between(start, Instant.now());
                    long remainingMillis = (long) ((elapsed.toMillis() / (double) done) * (totalCount - done));
                    String etaStr = formatDuration(Duration.ofMillis(remainingMillis));
                    String elapsedStr = formatDuration(elapsed);
                    System.out.printf("\rProgress: %.1f%% [%d/%d] ETA: %s | Elapsed: %s", percent, done, totalCount, etaStr, elapsedStr);
                    System.out.flush();
                } catch (IOException e) {
                    System.err.printf("\n[ERROR] File %d: %s%n", index, e.getMessage());
                }
            }, executor));
        }

        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
        executor.close();

        Duration totalDuration = Duration.between(start, Instant.now());
        System.out.println();
        System.out.printf("\n✅ Generation complete. Base directory: %s, total files: %d, total elapsed time: %s%n",
                baseDir.toAbsolutePath(), count, formatDuration(totalDuration));
    }

    private static void createFolderAndFile(Path baseDir, String prefix, String fileName, int fileSizeKB, boolean overwrite, int index) throws IOException {
        Path folder = baseDir.resolve(prefix + index);
        Files.createDirectories(folder);
        Path file = folder.resolve(fileName);
        if (!Files.exists(file) || overwrite) {
            try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                writePattern(out, fileSizeKB * 1024);
            }
        }
    }

    private static void writePattern(BufferedOutputStream out, int bytesToWrite) throws IOException {
        int written = 0;
        while (written < bytesToWrite) {
            int toWrite = Math.min(bytesToWrite - written, FILE_PATTERN.length);
            out.write(FILE_PATTERN, 0, toWrite);
            written += toWrite;
        }
    }

    private static int randomSize(int minKB, int maxKB) {
        return minKB + new Random().nextInt(maxKB - minKB + 1);
    }

    private static String formatDuration(Duration d) {
        long seconds = d.toSeconds();
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        long sec = seconds % 60;
        return String.format("%dm %ds", minutes, sec);
    }

    private static int parseSize(String s) {
        s = s.toUpperCase(Locale.ROOT);
        if (s.endsWith("MB")) return Integer.parseInt(s.replace("MB", "")) * 1024;
        else if (s.endsWith("KB")) return Integer.parseInt(s.replace("KB", ""));
        else return Integer.parseInt(s);
    }

    private static void printUsage() {
        System.out.println("""
FolderGenerator - Generate multiple folders with unique files

Usage:
  java FolderGenerator [options]

Options:
  --baseDir=<folder>    Output folder (default: output)
  --count=<number>      Number of folders to generate (default: 100)
  --prefix=<text>       Prefix for folder names (default: folder-)
  --fileName=<name>     Base name of file in each folder (default: file)
  --min-size=<size>     Minimum file size KB or MB (default: 50KB)
  --max-size=<size>     Maximum file size KB or MB (default: 500KB)
  --overwrite           Overwrite existing files (default: false)
  --help                Show this help message

Example:
  java FolderGenerator --baseDir=generated --count=200 --prefix=myfolder- --fileName=data --min-size=50KB --max-size=200KB --overwrite
""");
    }
}

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FileGeneratorV6 {

    private static final Random random = new Random();
    private static final char[] CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    public static void main(String[] args) throws Exception {

        if (args.length == 0 || Arrays.stream(args).anyMatch(a -> a.equals("--help"))) {
            printUsage();
            return;
        }

        // Defaults
        String outputFolder = "output";
        int totalFiles = 100;
        Set<String> formats = new LinkedHashSet<>(List.of("txt","csv","xlsx","docx","pdf"));
        int minSizeKB = 50;
        int maxSizeKB = 500;

        // Parse CLI arguments
        for (String arg : args) {
            if (arg.startsWith("--output=")) {
                outputFolder = arg.substring("--output=".length());
            } else if (arg.startsWith("--total=")) {
                totalFiles = Integer.parseInt(arg.substring("--total=".length()));
            } else if (arg.startsWith("--formats=")) {
                String[] f = arg.substring("--formats=".length()).split(",");
                formats = new LinkedHashSet<>(Arrays.asList(f));
            } else if (arg.startsWith("--min-size=")) {
                minSizeKB = parseSize(arg.substring("--min-size=".length()));
            } else if (arg.startsWith("--max-size=")) {
                maxSizeKB = parseSize(arg.substring("--max-size=".length()));
            }
        }

        if (minSizeKB > maxSizeKB) {
            int tmp = minSizeKB; minSizeKB = maxSizeKB; maxSizeKB = tmp;
        }

        Path folder = Paths.get(outputFolder);
        Files.createDirectories(folder);

        AtomicInteger completed = new AtomicInteger(0);
        // Executor for virtual threads
        Executor executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());

        long start = System.currentTimeMillis();

        // ---------------- Distribute totalFiles among formats ----------------
        int numFormats = formats.size();
        int baseFilesPerFormat = totalFiles / numFormats;
        int remainder = totalFiles % numFormats;
        int formatIndex = 0;

        List<CompletableFuture<Void>> allFutures = new ArrayList<>();

        for (String fmt : formats) {
            final String format = fmt;
            int filesForThisFormat = baseFilesPerFormat + (formatIndex < remainder ? 1 : 0);
            formatIndex++;

            for (int i = 1; i <= filesForThisFormat; i++) {
                final int index = i;           // for lambda
                final int minSize = minSizeKB;
                final int maxSize = maxSizeKB;

                allFutures.add(runAsync(() ->
                                createFile(folder, index, format, randomSize(minSize, maxSize)),
                        completed, totalFiles, executor));
            }
        }

        // Wait for all tasks to complete
        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();

        long end = System.currentTimeMillis();

        // ---------------- Summary ----------------
        long duration = end - start;
        long millis  = duration % 1000;
        long seconds = (duration / 1000) % 60;
        long minutes = (duration / (1000 * 60)) % 60;
        long hours   = (duration / (1000 * 60 * 60));

        System.out.printf(
                "\nGenerated %d files in %dh %dm %ds %dms at %s%n",
                totalFiles, hours, minutes, seconds, millis, folder
        );
    }


    private static void printUsage() {
        System.out.println("""
        FileGenerator - Generate random files in multiple formats.

        Usage:
          java FileGenerator [options]

        Options:
          --output=<folder>       Output folder where files will be saved (default: output)
          --total=<number>        Total number of files per format to generate (default: 100)
          --formats=<list>        Comma-separated list of formats to generate.
                                  Supported: txt,csv,xlsx,docx,pdf
                                  Default: txt,csv,xlsx,docx,pdf
          --min-size=<size>       Minimum size of each file in KB or MB (default: 50KB)
          --max-size=<size>       Maximum size of each file in KB or MB (default: 500KB)
          --help                  Show this help message and exit

        Example:
          java FileGenerator --output=generatedFiles --total=500 --formats=txt,csv,pdf --min-size=50KB --max-size=200KB
        """);
    }


    // ---------------- Helper Methods ----------------
    private static int parseSize(String size) {
        size = size.toUpperCase().trim();
        if (size.endsWith("KB")) return Integer.parseInt(size.replace("KB","").trim());
        if (size.endsWith("MB")) return Integer.parseInt(size.replace("MB","").trim()) * 1024;
        return Integer.parseInt(size); // default KB
    }

    private static int randomSize(int minKB, int maxKB) {
        return (minKB + random.nextInt(maxKB - minKB + 1)) * 1024;
    }

    private static Path filePath(Path folder, int index, String ext) {
        return folder.resolve("file_" + index + "." + ext);
    }

    private static String randomString(int length) {
        var rnd = ThreadLocalRandom.current();
        var sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(CHARS[rnd.nextInt(CHARS.length)]);
        return sb.toString();
    }

    private static CompletableFuture<Void> runAsync(Task task, AtomicInteger completed, int totalTasks, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } catch (Exception e) {
                System.err.println("\nError generating file: ");
                e.printStackTrace(System.err);
            } finally {
                int done = completed.incrementAndGet();
                printProgressBar(done, totalTasks);
            }
        }, executor);
    }

    @FunctionalInterface
    interface Task { void run() throws Exception; }

    private static void printProgressBar(int done, int totalTasks) {
        int width = 50;
        int filled = (done * width) / totalTasks;
        String bar = "=".repeat(filled) + " ".repeat(width - filled);
        System.out.print("\r[" + bar + "] " + (100 * done / totalTasks) + "% (" + done + "/" + totalTasks + ")");
    }

    private static void createFile(Path folder, int index, String fmt, int size) throws IOException {
        switch(fmt.toLowerCase()) {
            case "txt" -> createTextFile(filePath(folder, index, "txt"), size);
            case "csv" -> createCsvFile(filePath(folder, index, "csv"), size);
            case "xlsx" -> createXlsxFile(filePath(folder, index, "xlsx"), size);
            case "docx" -> createDocxFile(filePath(folder, index, "docx"), size);
            case "pdf" -> createPdfFile(filePath(folder, index, "pdf"), size);
            default -> throw new IllegalArgumentException("Unsupported format: " + fmt);
        }
    }

    // ---------------- File Generators ----------------
    public static void createTextFile(Path path, int targetSize) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            int written = 0;
            while (written < targetSize) {
                String line = randomString(100) + "\n";
                writer.write(line);
                written += line.getBytes().length;
            }
        }
    }

    public static void createCsvFile(Path path, int targetSize) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            int written = 0;
            while (written < targetSize) {
                String line = random.nextInt(1000) + "," + randomString(10) + "," + random.nextDouble() + "\n";
                writer.write(line);
                written += line.getBytes().length;
            }
        }
    }

    public static void createXlsxFile(Path path, int targetSize) throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            Sheet sheet = wb.createSheet("Sheet1");
            int rowNum = 0;
            int maxRows = Math.max(1, targetSize / 1000);
            while (rowNum < maxRows) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < 10; i++) row.createCell(i).setCellValue(randomString(20));
            }
            try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
                wb.write(fos);
            }
        }
    }

    public static void createDocxFile(Path path, int targetSize) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(path.toFile())) {

            int written = 0;
            while (written < targetSize) {
                XWPFParagraph p = doc.createParagraph();
                XWPFRun run = p.createRun();
                String text = randomString(200);
                run.setText(text);
                written += text.getBytes().length;
            }
            doc.write(fos);
        }
    }

    public static void createPdfFile(Path path, int targetSize) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            float y = 700;
            int written = 0;
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            while (written < targetSize) {
                // Create new page if needed
                if (y < 50) {
                    cs.close();                  // close old stream
                    page = new PDPage(PDRectangle.LETTER);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page); // open new stream
                    y = 700;
                }

                String text = randomString(200);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, y);
                cs.showText(text);
                cs.endText();

                y -= 15;
                written += text.getBytes().length;
            }

            cs.close();
            doc.save(path.toFile());
        }
    }

}

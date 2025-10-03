import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FileGeneratorV5 {

    private static final Random random = new Random();

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: FileGenerator.exe <outputFolder> <totalFiles>");
            return;
        }

        String folderPath = args[0];
        int totalFiles = Integer.parseInt(args[1]);
        Files.createDirectories(Paths.get(folderPath));

        int formats = 5;
        int perFormat = totalFiles / formats;

        // Java 21 virtual threads for lightweight concurrency
        ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
        AtomicInteger completed = new AtomicInteger(0);
        int totalTasks = perFormat * formats;

        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 1; i <= perFormat; i++) {
            int index = i;
            tasks.add(() -> runWithProgress(() -> createTextFile(folderPath + "/file_" + index + ".txt", randomSize()), completed, totalTasks));
            tasks.add(() -> runWithProgress(() -> createCsvFile(folderPath + "/file_" + index + ".csv", randomSize()), completed, totalTasks));
            tasks.add(() -> runWithProgress(() -> createXlsxFile(folderPath + "/file_" + index + ".xlsx", randomSize()), completed, totalTasks));
            tasks.add(() -> runWithProgress(() -> createDocxFile(folderPath + "/file_" + index + ".docx", randomSize()), completed, totalTasks));
            tasks.add(() -> runWithProgress(() -> createPdfFile(folderPath + "/file_" + index + ".pdf", randomSize()), completed, totalTasks));
        }

        long start = System.currentTimeMillis();
        executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        long end = System.currentTimeMillis();

        if (completed.get() < totalTasks) {
            completed.set(totalTasks);
            printProgressBar(completed.get(), totalTasks);
        }

        long duration = end - start;
        long millis  = duration % 1000;
        long seconds = (duration / 1000) % 60;
        long minutes = (duration / (1000 * 60)) % 60;
        long hours   = (duration / (1000 * 60 * 60));

        System.out.printf(
                "\nGenerated %d files in %dh %dm %ds %dms at %s%n",
                totalFiles, hours, minutes, seconds, millis, folderPath
        );
    }

    // ---------------- Run task with progress ----------------
    private static Void runWithProgress(Task task, AtomicInteger completed, int totalTasks) {
        try {
            task.run();
        } catch (Exception e) {
            System.err.println("\nError generating file: " + e.getMessage());
        } finally {
            int done = completed.incrementAndGet();
            printProgressBar(done, totalTasks);
        }
        return null;
    }

    @FunctionalInterface
    interface Task { void run() throws Exception; }

    // ---------------- Progress Bar ----------------
    private static void printProgressBar(int done, int totalTasks) {
        int width = 50;
        int filled = (done * width) / totalTasks;
        String bar = "=".repeat(filled) + " ".repeat(width - filled);
        System.out.print("\r[" + bar + "] " + (100 * done / totalTasks) + "% (" + done + "/" + totalTasks + ")");
    }

    // ---------------- Utilities ----------------
    private static int randomSize() {
        return (50 + random.nextInt(450)) * 1024; // 50KB to 500KB
    }

    private static String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    // ---------------- File Generators ----------------
    public static void createTextFile(String path, int targetSize) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path))) {
            int written = 0;
            while (written < targetSize) {
                String line = randomString(100) + "\n";
                writer.write(line);
                written += line.getBytes().length;
            }
        }
    }

    public static void createCsvFile(String path, int targetSize) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path))) {
            int written = 0;
            while (written < targetSize) {
                String line = random.nextInt(1000) + "," + randomString(10) + "," + random.nextDouble() + "\n";
                writer.write(line);
                written += line.getBytes().length;
            }
        }
    }

    public static void createXlsxFile(String path, int targetSize) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            int rowNum = 0;
            int maxRows = Math.max(1, targetSize / 1000); // Roughly adjust number of rows based on target size
            while (rowNum < maxRows) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < 10; i++) row.createCell(i).setCellValue(randomString(20));
            }
            try (FileOutputStream fos = new FileOutputStream(path)) {
                wb.write(fos);
            }
        }
    }

    public static void createDocxFile(String path, int targetSize) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(path)) {

            int written = 0;
            while (written < targetSize) {
                XWPFParagraph p = doc.createParagraph();
                XWPFRun run = p.createRun();
                String text = randomString(200);
                run.setText(text);
                written += text.getBytes().length;

                // flush periodically to reduce memory
                if (written % (1024 * 1024) < 200) {
                    doc.write(fos);
                    fos.flush();
                }
            }
            doc.write(fos);
        }
    }

    // ---------------- PDFBox version ----------------
    public static void createPdfFile(String path, int targetSize) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            int written = 0;

            while (written < targetSize) {
                String text = randomString(200);

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, 700); // simple fixed position
                cs.showText(text);
                cs.endText();

                written += text.getBytes().length;

                // Flush every ~1MB to reduce memory usage
                if (written % (1024 * 1024) < 200) {
                    cs.close();
                    cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);
                }
            }

            cs.close();
            doc.save(path);
        }
    }
}

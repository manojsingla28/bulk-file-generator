import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FileGeneratorV4 {

    private static final Random random = new Random();

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java FileGenerator <outputFolder> <totalFiles>");
            return;
        }

        String folderPath = args[0];
        int totalFiles = Integer.parseInt(args[1]);
        Files.createDirectories(Paths.get(folderPath));

        int formats = 5; // TXT, CSV, XLSX, DOCX, PDF
        int perFormat = totalFiles / formats;

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
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

        // Ensure progress bar ends at 100%
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
                "\n✅ Generated %d files in %02d:%02d:%02d.%03d (hh:mm:ss.SSS) at %s%n",
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
        String bar = repeat('=', filled) + repeat(' ', width - filled);
        System.out.print("\r[" + bar + "] " + (100 * done / totalTasks) + "% (" + done + "/" + totalTasks + ")");
    }

    private static String repeat(char c, int times) {
        char[] arr = new char[times];
        Arrays.fill(arr, c);
        return new String(arr);
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
            StringBuilder sb = new StringBuilder();
            while (sb.length() < targetSize) sb.append(randomString(100)).append("\n");
            writer.write(sb.toString());
        }
    }

    public static void createCsvFile(String path, int targetSize) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path))) {
            StringBuilder sb = new StringBuilder();
            while (sb.length() < targetSize)
                sb.append(random.nextInt(1000)).append(",")
                        .append(randomString(10)).append(",")
                        .append(random.nextDouble()).append("\n");
            writer.write(sb.toString());
        }
    }

    public static void createXlsxFile(String path, int targetSize) throws IOException {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Sheet1");

        int rowNum = 0;
        int maxRows = 5000; // or any reasonable number
        while (rowNum < maxRows) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < 10; i++) row.createCell(i).setCellValue(randomString(20));
        }

        try (FileOutputStream fos = new FileOutputStream(path)) {
            wb.write(fos);
        }
        wb.close();
    }

    public static void createDocxFile(String path, int targetSize) throws IOException {
        XWPFDocument doc = new XWPFDocument();
        int size = 0;
        while (size < targetSize) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun run = p.createRun();
            String text = randomString(200);
            run.setText(text);
            size += text.length();
        }
        try (FileOutputStream fos = new FileOutputStream(path)) {
            doc.write(fos);
        }
        doc.close();
    }

    public static void createPdfFile(String path, int targetSize) throws IOException {
        PdfWriter writer = new PdfWriter(path);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        int size = 0;
        while (size < targetSize) {
            String text = randomString(200);
            document.add(new Paragraph(text));
            size += text.length();
        }

        document.close();
        pdf.close();
    }
}

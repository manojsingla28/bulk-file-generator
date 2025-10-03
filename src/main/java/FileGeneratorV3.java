import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FileGeneratorV3 {

    private static final Random random = new Random();

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Please provide outputFolder and totalFiles");
            return;
        }

        String folderPath = args[0];
        int totalFiles = Integer.parseInt(args[1]);
        Files.createDirectories(Paths.get(folderPath));

        int formats = 5; // TXT, CSV, XLS, DOCX, PDF
        int perFormat = totalFiles / formats;

        ExecutorService executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 2
        );
        List<Callable<Void>> tasks = new ArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);
        int totalTasks = perFormat * formats;

        for (int i = 1; i <= perFormat; i++) {
            int index = i;
            tasks.add(() -> { createTextFile(folderPath + "/file_" + index + ".txt", randomSize()); progress(completed, totalTasks); return null; });
            tasks.add(() -> { createCsvFile(folderPath + "/file_" + index + ".csv", randomSize()); progress(completed, totalTasks); return null; });
            tasks.add(() -> { createXlsFile(folderPath + "/file_" + index + ".xls", randomSize()); progress(completed, totalTasks); return null; });
            tasks.add(() -> { createDocxFile(folderPath + "/file_" + index + ".docx", randomSize()); progress(completed, totalTasks); return null; });
            tasks.add(() -> { createPdfFile(folderPath + "/file_" + index + ".pdf", randomSize()); progress(completed, totalTasks); return null; });
        }

        long start = System.currentTimeMillis();
        executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        long end = System.currentTimeMillis();

        // format duration with ms
        long duration = end - start;

        long millis  = duration % 1000;
        long seconds = (duration / 1000) % 60;
        long minutes = (duration / (1000 * 60)) % 60;
        long hours   = (duration / (1000 * 60 * 60));

        System.out.printf(
                "\n✅ Generated %d files in %02d:%02d:%02d:%03d at %s%n",
                totalFiles, hours, minutes, seconds, millis, folderPath
        );
    }

    private static void progress(AtomicInteger completed, int total) {
        int done = completed.incrementAndGet();
        if (done % 5 == 0 || done == total) { // update every 5 files
            System.out.print("\rProgress: " + (100 * done / total) + "% (" + done + "/" + total + ")");
        }
    }

    // Random size between 50 KB and 500 KB
    private static int randomSize() {
        return (50 + random.nextInt(450)) * 1024;
    }

    // ---------------- TEXT ----------------
    public static void createTextFile(String path, int targetSize) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path))) {
            StringBuilder sb = new StringBuilder();
            while (sb.length() < targetSize) {
                sb.append(randomString(100)).append("\n");
            }
            writer.write(sb.toString());
        }
    }

    // ---------------- CSV ----------------
    public static void createCsvFile(String path, int targetSize) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path))) {
            StringBuilder sb = new StringBuilder();
            while (sb.length() < targetSize) {
                sb.append(random.nextInt(1000)).append(",")
                        .append(randomString(10)).append(",")
                        .append(random.nextDouble()).append("\n");
            }
            writer.write(sb.toString());
        }
    }

    // ---------------- XLS ----------------
    public static void createXlsFile(String path, int targetSize) throws IOException {
        Workbook wb = new HSSFWorkbook();
        Sheet sheet = wb.createSheet("Sheet1");

        int rowNum = 0;
        while (sheet.toString().length() < targetSize) { // approx check
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < 10; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(randomString(20));
            }
        }
        try (FileOutputStream fos = new FileOutputStream(path)) {
            wb.write(fos);
        }
        wb.close();
    }

    // ---------------- DOCX ----------------
    public static void createDocxFile(String path, int targetSize) throws Exception {
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

    // ---------------- PDF ----------------
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

    // ---------------- Utils ----------------
    private static String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
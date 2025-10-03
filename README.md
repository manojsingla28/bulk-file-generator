# 🗂️ Bulk File Generator V8

A high-performance Java utility for generating **large volumes of files** (TXT, CSV, XLSX, DOCX, PDF) with **random content**.  
Built with **Java 21 Virtual Threads** for massive concurrency and optimized for speed.

---

## 🚀 Features

- 🔸 Generates **Text**, **CSV**, **Excel (XLSX)**, **Word (DOCX)**, and **PDF** files
- 🔸 Uses **Java 21 Virtual Threads** for lightweight, parallel execution
- 🔸 Supports **configurable file sizes** (50 KB – 500 KB by default)
- 🔸 Live **progress bar** during generation
- 🔸 Distributes files **evenly across formats** when generating multiple types
- 🔸 Efficient random content generation
- 🔸 Safe multi-page **PDF generation** without memory issues

---

## 🛠️ Tech Stack

- **Java 21**
- [Apache POI](https://poi.apache.org/) – for DOCX and XLSX generation
- [Apache PDFBox](https://pdfbox.apache.org/) – for PDF generation
- Java NIO – for fast file writing

---

## 📦 Installation

1. Clone the repository:

```bash
git clone https://github.com/<your-username>/bulk-file-generator.git
cd bulk-file-generator
```

2. Build with Maven (or your preferred build tool):

```bash
mvn clean package
```

💡 You’ll need **Java 21+** installed.

---

## 🧪 Usage

Run the utility from the command line:

```bash
java -cp target/bulk-file-generator-1.0.jar FileGeneratorV8 [options]
```

### Available options:

| Option | Description | Default |
|--------|-------------|---------|
| `--output=<folder>` | Output folder where files will be saved | `output` |
| `--total=<number>` | Total number of files to generate (across all formats) | `100` |
| `--formats=<list>` | Comma-separated list of formats to generate. Supported: `txt,csv,xlsx,docx,pdf` | `txt,csv,xlsx,docx,pdf` |
| `--min-size=<size>` | Minimum file size per file in KB or MB | `50KB` |
| `--max-size=<size>` | Maximum file size per file in KB or MB | `500KB` |
| `--help` | Display help message | - |

### Example

```bash
java -cp target/bulk-file-generator-1.0.jar FileGeneratorV8 --output=generatedFiles --total=500 --formats=txt,csv,pdf --min-size=50KB --max-size=200KB
```

**Behavior**:

- Generates **500 files total**, evenly distributed among `txt`, `csv`, and `pdf`.
- Uses **virtual threads** to run all file generation in parallel.
- Shows a **live progress bar** during execution.

### 📊 File Distribution Diagram

For example, `--total=500` and `--formats=txt,csv,pdf` (3 formats):

```
Total files: 500
-----------------
TXT  -> 167 files
CSV  -> 167 files
PDF  -> 166 files
```

Files are divided evenly, remainder goes to the first formats.

---

## ⚡ Performance Tips

- Use **SSD storage** for faster I/O.
- Increase the JVM heap size if generating very large files: `-Xmx4G`
- Adjust the `--min-size` and `--max-size` to control file sizes.
- For extremely large Excel files, `SXSSFWorkbook` streaming ensures low memory usage.
- Batching per format reduces context switching and speeds up generation.

---

## 📁 Repository Structure

```
.
├── src/
│   └── main/java/FileGeneratorV8.java
├── pom.xml
├── README.md
└── LICENSE
```

---

## ⭐ Contribute

This is a personal utility repo, but PRs and suggestions are welcome.  
If you find this useful, give it a ⭐ on GitHub!

---

## 👤 Author

**Manoj Kumar**  
💼 Senior Java Developer  
🔗 [GitHub Profile](https://github.com/manojsingla28)
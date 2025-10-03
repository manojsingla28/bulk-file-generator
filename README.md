# 🗂️ Bulk File Generator

A high-performance Java utility for generating **large volumes of files** (TXT, CSV, XLSX, DOCX, PDF) with **random content**.  
Built with **Java 21 Virtual Threads** for massive concurrency and optimized for speed.

---

## 🚀 Features

- 🔸 Generates **Text**, **CSV**, **Excel (XLSX)**, **Word (DOCX)**, and **PDF** files
- 🔸 Uses **Java 21 Virtual Threads** for lightweight, parallel execution
- 🔸 Supports **configurable file sizes** (50 KB – 500 KB by default)
- 🔸 Live **progress bar** during generation
- 🔸 Efficient random content generation

---

## 🛠️ Tech Stack

- **Java 21**
- [Apache POI](https://poi.apache.org/) – for DOCX and XLSX generation
- [Apache PDFBox](https://pdfbox.apache.org/) – for PDF generation
- Java NIO – for fast file writing

---

## 📦 Installation

1. Clone the repository:

```
git clone https://github.com/<your-username>/bulk-file-generator.git
cd bulk-file-generator
```
2. Build with Maven (or your preferred build tool):

```
mvn clean package
```

💡 You’ll need Java 21+ installed.

## 🧪 Usage

You can run the utility from the command line after building:
```
java -cp target/bulk-file-generator-1.0.jar FileGeneratorV5 <output-folder> <total-files>
```
# Example:
```
java -cp target/bulk-file-generator-1.0.jar FileGeneratorV5 ./output 1000
```

This will generate:
* 200 TXT files
* 200 CSV files
* 200 XLSX files
* 200 DOCX files
* 200 PDF files

All in parallel, with progress updates.

## ⚡ Performance Tips

- Use SSD storage for faster I/O.
- Increase the JVM heap size if generating very large files.
- Adjust the randomSize() method to control file sizes.
- For extremely large Excel files, consider using SXSSFWorkbook (streaming).

## 📁 Repository Structure
```
.
├── src/
│   └── main/java/FileGeneratorV5.java
├── pom.xml
├── README.md
└── LICENSE
```
## ⭐ Contribute

This is a personal utility repo, but PRs and suggestions are welcome.
If you find this useful, give it a ⭐ on GitHub!

## 👤 Author

Manoj Kumar
💼 Senior Java Developer
🔗 GitHub Profile

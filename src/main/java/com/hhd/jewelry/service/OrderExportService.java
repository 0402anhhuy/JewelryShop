package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.Order;
import com.hhd.jewelry.repository.OrderRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrderExportService {

    private final OrderRepository orderRepo;
    private final ResourceLoader resourceLoader;
    // ✅ Xuất Excel
    public ByteArrayInputStream exportToExcel() throws Exception {
        List<Order> orders = orderRepo.findAll();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh sách đơn hàng");

            // ✅ Header style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont(); // 🟢 dùng full path tránh conflict
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // ✅ Tạo hàng tiêu đề
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Mã đơn", "Khách hàng", "Tổng tiền (₫)", "Trạng thái", "Ngày tạo"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // ✅ Style nội dung
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            int rowIdx = 1;
            for (Order o : orders) {
                Row row = sheet.createRow(rowIdx++);

                Cell c0 = row.createCell(0);
                c0.setCellValue("ORD-" + o.getId());
                c0.setCellStyle(dataStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(o.getUser() != null ? o.getUser().getFullName() : "Khách ẩn danh");
                c1.setCellStyle(dataStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(nf.format(o.getTotalAmount()));
                c2.setCellStyle(dataStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(o.getStatus() != null ? o.getStatus().name() : "N/A");
                c3.setCellStyle(dataStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(o.getCreatedAt().format(dtf));
                c4.setCellStyle(dataStyle);
            }

            for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // ✅ Xuất PDF
    public ByteArrayInputStream exportToPdf() throws Exception {
        List<Order> orders = orderRepo.findAll();

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // Font Unicode tiếng Việt
        String fontClasspath = "classpath:fonts/DejaVuSans.ttf";
        Resource resource = resourceLoader.getResource(fontClasspath);

        // Đọc font thành mảng byte[]
        InputStream inputStream = resource.getInputStream();
        byte[] fontBytes = inputStream.readAllBytes();
        inputStream.close();

        // Tạo BaseFont từ mảng byte
        BaseFont unicodeFont = BaseFont.createFont("DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, fontBytes, null);
        Font fontNormal = new Font(unicodeFont, 11);
        Font fontHeader = new Font(unicodeFont, 12, Font.BOLD);
        Font fontTitle = new Font(unicodeFont, 16, Font.BOLD, Color.BLUE);

        // Tiêu đề
        Paragraph title = new Paragraph("Danh sách đơn hàng", fontTitle);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15);
        document.add(title);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setWidths(new int[]{2, 4, 3, 3, 4});

        // Header
        String[] headers = {"Mã đơn", "Khách hàng", "Tổng tiền (₫)", "Trạng thái", "Ngày tạo"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHeader));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Order o : orders) {
            table.addCell(new Phrase("ORD-" + o.getId(), fontNormal));
            table.addCell(new Phrase(o.getUser() != null ? o.getUser().getFullName() : "Khách ẩn danh", fontNormal));
            table.addCell(new Phrase(nf.format(o.getTotalAmount()), fontNormal));
            table.addCell(new Phrase(o.getStatus() != null ? o.getStatus().name() : "N/A", fontNormal));
            table.addCell(new Phrase(o.getCreatedAt().format(dtf), fontNormal));
        }

        document.add(table);
        document.close();

        return new ByteArrayInputStream(out.toByteArray());
    }
}

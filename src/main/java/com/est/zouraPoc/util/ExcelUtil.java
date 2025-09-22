package com.est.zouraPoc.util;

import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
public class ExcelUtil {
    @Value("${zuora.export.path}")
    private String path;
	 public void createExcel() {
		 Workbook workbook = new XSSFWorkbook();

	     
	     Sheet sheet = workbook.createSheet("Sheet1");

	     
	     Row row = sheet.createRow(0);

	     
	     Cell cell1 = row.createCell(0);
	     cell1.setCellValue("Name");

	     Cell cell2 = row.createCell(1);
	     cell2.setCellValue("Age");

	    
	     row = sheet.createRow(1);

	     
	     cell1 = row.createCell(0);
	     cell1.setCellValue("John");

	     cell2 = row.createCell(1);
	     cell2.setCellValue(25);

	     
	     row = sheet.createRow(2);

	     
	     cell1 = row.createCell(0);
	     cell1.setCellValue("Alice");

	     cell2 = row.createCell(1);
	     cell2.setCellValue(30);

	     
	     try (FileOutputStream fileOut = new FileOutputStream(path+".xlsx")) {
	         workbook.write(fileOut);
	     } catch (IOException e) {
	         e.printStackTrace();
	     }

	     System.out.println("Excel file created successfully!");
	 }
	 
}

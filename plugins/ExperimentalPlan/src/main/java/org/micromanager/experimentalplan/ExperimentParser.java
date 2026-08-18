package org.micromanager.experimentalplan;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class ExperimentParser {

    public List<String> parse(File excelFile) throws IOException {

        System.out.println("PARSER STARTED");
        System.out.println("FILE: " + excelFile.getAbsolutePath());

        List<String> experimentIds = new ArrayList<>();

        try (FileInputStream input = new FileInputStream(excelFile);
            Workbook workbook = WorkbookFactory.create(input)) {

            System.out.println("WORKBOOK OPENED");
            System.out.println("NUMBER OF SHEETS: "
                + workbook.getNumberOfSheets());

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                System.out.println(
                    "SHEET " + i + ": "
                    + workbook.getSheetName(i)
                );
            }

            Sheet sheet =
                workbook.getSheet("Experiment Dashboard");

            if (sheet == null) {
                throw new IOException(
                    "Could not find sheet: Experiment Dashboard"
                );
            }

            System.out.println("SHEET FOUND");

            DataFormatter formatter = new DataFormatter();

            for (int rowNumber = 1;
                rowNumber <= sheet.getLastRowNum();
                rowNumber++) {

                Row row = sheet.getRow(rowNumber);

                if (row == null) {
                    continue;
                }

                String experimentId =
                    formatter.formatCellValue(
                        row.getCell(0)
                    ).trim();

                System.out.println(
                    "ROW " + rowNumber
                    + " = [" + experimentId + "]"
                );

                if (!experimentId.isEmpty()) {
                    experimentIds.add(experimentId);
                }
            }

            System.out.println(
                "FOUND IDS: " + experimentIds
            );
        }

        return experimentIds;
    }
}
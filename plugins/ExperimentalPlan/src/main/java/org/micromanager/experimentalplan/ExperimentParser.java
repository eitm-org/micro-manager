package org.micromanager.experimentalplan;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class ExperimentParser {

    // reads the experimental plans excel file and returns a list of experiment IDs that are marked as "Planned"
    public List<Experiment> parse(File excelFile) throws IOException {

        List<Experiment> experiments = new ArrayList<>();

        // open excel and create an Apache POI Workbook
        try (FileInputStream input = new FileInputStream(excelFile);
            Workbook workbook = WorkbookFactory.create(input)) {

            // sheet with all experiments
            Sheet sheet = workbook.getSheet("Experiment Dashboard");

            if (sheet == null) {
                throw new IOException( "Could not find sheet: Experiment Dashboard" );
            }

            DataFormatter formatter = new DataFormatter();

            // experiment ID is column A
            int experimentIdColumn = 0;

            // status is column B
            int statusColumn = 1;

            int purposeColumn = 3;

            for (int rowNumber = 1; rowNumber <= sheet.getLastRowNum(); rowNumber++) {

                Row row = sheet.getRow(rowNumber);

                if (row == null) {
                    continue;
                }

                String experimentId = formatter.formatCellValue( row.getCell(experimentIdColumn) ).trim();

                String status = formatter.formatCellValue( row.getCell(statusColumn) ).trim();

                String purpose = formatter.formatCellValue(row.getCell(purposeColumn)).trim();

                if (!experimentId.isEmpty() && status.equalsIgnoreCase("Planned")) {
                    experiments.add(new Experiment(experimentId, purpose));
                }
            }

        }

        return experiments;
    }
}
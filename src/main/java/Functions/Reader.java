package Functions;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Reader {
    //Gets the appropriate worksheet
    public static void readDataToObjects() throws IOException {
        //Getting the file
        String filePath = "src/main/resources/e-Delphyn LIS 21.1.0 US Statistics from Jira.xlsx";
        Workbook workbook = WorkbookFactory.create(new File(filePath));
        Sheet sheet = workbook.getSheetAt(0); // Assuming the data is in the first sheet (index 0)

        //Get the data from the file
        getDataFromFile(sheet);
    }

    //Gets the data from the file
    private static void getDataFromFile(Sheet sheet) {
        int numRows = sheet.getLastRowNum() + 1;
        Map<String, List<Row>> rawDataMapByIssueType = new HashMap<>();
        Map<String, Map<String, Long>> issueTypeStatusTimes = new HashMap<>();
        int numInternalIssues = 0;
        HashSet<String> internals = new HashSet<>();

        //Get the rows by types
        for (int i = 1; i < numRows; i++) {
            Row row = sheet.getRow(i);
            String issueType = row.getCell(0).getStringCellValue();

            // save the data row to a map, issuetype key, row items
            if (rawDataMapByIssueType.containsKey(issueType)){
                rawDataMapByIssueType.get(issueType).add(row);
            }
            else {
                rawDataMapByIssueType.put(issueType, new ArrayList<>());
                rawDataMapByIssueType.get(issueType).add(row);
            }

            //Getting number of internals
            if (issueType.equals("Internal")) {
                internals.add(row.getCell(1).getStringCellValue());
            }
        }
        for (String internal : internals){
            numInternalIssues++;
        }

        // iterate through each issuetype in the map and calculate the statusTimes map values
        for (Map.Entry<String, List<Row>> entry : rawDataMapByIssueType.entrySet()) {
            Map<String, Long> statusTimes = new HashMap<>();

            for (Row row : entry.getValue()) {
                //calculate time spent
                String issueType = row.getCell(0).getStringCellValue();
                String key = row.getCell(1).getStringCellValue();
                String status = row.getCell(2).getStringCellValue();
                Date createdDate = row.getCell(3).getDateCellValue();
                Date transitionDate = row.getCell(4).getDateCellValue();
                String fromStatus;
                String toStatus;
                try {
                    fromStatus = row.getCell(5).getStringCellValue();
                    toStatus = row.getCell(6).getStringCellValue();
                } catch (NullPointerException ex) {
                    fromStatus = "New";
                    toStatus = "";
                }

                long timeSpent = new Date().getTime();
                if (status.equals("New")) {
                    timeSpent = new Date().getTime() - createdDate.getTime();
                } else if (fromStatus.equals("New")) {
                    timeSpent = transitionDate.getTime() - createdDate.getTime();
                }
                else {
                    for (int j = 1; j < entry.getValue().size(); j++) {
                        try {
                            //if found, first check which date is bigger!
                            if (entry.getValue().get(j).getCell(6).getStringCellValue().equals(fromStatus) &&
                                    entry.getValue().get(j).getCell(1).getStringCellValue().equals(key)) {
                                if (transitionDate.after(entry.getValue().get(j).getCell(4).getDateCellValue())){
                                    timeSpent = transitionDate.getTime() - entry.getValue().get(j).getCell(4).getDateCellValue().getTime();
                                } else {
                                    timeSpent = entry.getValue().get(j).getCell(4).getDateCellValue().getTime() - transitionDate.getTime();
                                }
                                break;
                            }
                        } catch (NullPointerException ex) {
                            System.out.println("Nullpointer at calculating difference: " + entry.getValue().get(j).getCell(1));
                        }
                    }
                }

                if (statusTimes.containsKey(fromStatus)){
                    timeSpent = statusTimes.get(fromStatus) + timeSpent;
                }
                statusTimes.put(fromStatus, timeSpent);
                issueTypeStatusTimes.put(issueType, statusTimes);
            }
        }
        writeDataToCsv(issueTypeStatusTimes, rawDataMapByIssueType, numInternalIssues);
    }

    //Calculates the difference and writes data to .csv file
    private static void writeDataToCsv(Map<String, Map<String, Long>> issueTypeStatusTimes,
                                       Map<String, List<Row>> rawData, int numOfInternals) {
        StringBuilder csvContent = new StringBuilder();

    // Append the CSV header
        csvContent.append("Issue Type,Status,Average Time (ms)\n");

    // Iterate over the issue types and their respective status times
        for (Map.Entry<String, Map<String, Long>> entry : issueTypeStatusTimes.entrySet()) {
            String issueType = entry.getKey();
            Map<String, Long> statusTimes = entry.getValue();
            double averageSumTime = 0;

            // Calculate the average time for each status
            for (Map.Entry<String, Long> statusEntry : statusTimes.entrySet()) {
                //exclude completed moved back to anything
                if (statusEntry.getKey().equals("Completed")) {
                    continue;
                }

                String status = statusEntry.getKey();
                Long totalTime = statusEntry.getValue();

                int counter = 0;
                for (Map.Entry<String, List<Row>> rawEntry : rawData.entrySet()){
                    if (rawEntry.getKey().equals(issueType)){
                        for (Row row : rawEntry.getValue()){
                            if (row.getCell(5) != null) {
                                if (status.equals(row.getCell(5).getStringCellValue())) {
                                    counter++;
                                }
                            }
                        }
                    }
                }
                // Calculate the average time
                double averageTime = (double) totalTime / counter / 1000;
                String formattedAverageTime = formatElapsedTime((long) averageTime);
                averageSumTime = averageSumTime + averageTime;
                // Append the data to the CSV content
                csvContent.append(issueType).append(",");
                csvContent.append(status).append(",");
                csvContent.append(formattedAverageTime).append("\n");
            }
            String formattedAverageTimeSum = formatElapsedTime((long) averageSumTime);
            csvContent.append(entry.getKey() + " sum: " + formattedAverageTimeSum).append("\n");
            csvContent.append("\n");
        }

        csvContent.append("Number of internals: " + numOfInternals);
        //export the csv
        String csvFilePath = "output.csv";
        try (FileWriter fileWriter = new FileWriter(csvFilePath, true)) {
            fileWriter.write(csvContent.toString());

            System.out.println("Data exported successfully to " + csvFilePath);
        } catch (IOException e) {
            System.out.println("Error occurred while exporting data: " + e.getMessage());
        }
    }

    //Formats the difference between dates stored in long to hours:minutes:seconds
    public static String formatElapsedTime (long seconds) {

        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds (hours);

        long minutes = TimeUnit.SECONDS.toMinutes (seconds);
        seconds -= TimeUnit.MINUTES.toSeconds (minutes);

        return String.format ("%dhr:%dmin:%dsec", hours, minutes, seconds);
    }
}

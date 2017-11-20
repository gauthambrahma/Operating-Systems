
/*Name: Gautham Brahma Ponnaganti.
 *Course No.: CS5323
 *Assignment title:OS Project PHASE II
 *Date:4-28-2016
 *Global Variables:
 *-ErrorHandler:instance of error handler to trap errors if any that occurs.
 *
 *Brief description: This is where the execution of the system starts with. It will call loader sub-system and 
 *the execution will branch back to here if there is any error occurs. 
 *
 *Changes in PhaseIII:Made changes for multilevel feedback queues
 */

import java.io.*;
import java.util.ArrayList;

public class system {
	public static String fileNameToAppend;

	public void begin(String fileName) {
	try {
	String filename = "MLFBQ.txt";
	FileWriter fw = new FileWriter(filename);
	fw.close();
	} catch (IOException e) {
	e.printStackTrace();
	}
	ArrayList<Integer> matrix = new ArrayList<Integer>();
	if (fileName.contains(".txt"))
		fileNameToAppend = fileName.substring(0, fileName.indexOf(".txt"));
	else
		fileNameToAppend = fileName;
	File file = new File(fileName);
	File output = new File("output");
	if (output.delete()) {
	File outputFile = new File("output");
	}
	if (!file.exists()) {
	errorHandler.HandleError(ErrorHandler.FILE_NOT_FOUND);
	HandleError(ErrorHandler.FILE_NOT_FOUND);
	}
	for (int i = 3; i <= 5; i++) {
	for (int j = 35; j <= 50; j = j + 5) {
	Scheduler.allowedNumberOfTurns = i;
	Scheduler.allowedQuantum = j;
	Loader loader = new Loader();
	loader.loader(file);
	matrix.add(Scheduler.matrixMetric);
	clearStaticVariables();
	}
	}
	try {
	String filename = "3x4Matrix.txt";
	FileWriter fw = new FileWriter(filename);
	fw.write(String.format("%-5s %-5s %-5s %-5s %-5s", "", "35", "40", "45",
			"50"));
	fw.write("\n");
	fw.write(String.format("%-5s %-5s %-5s %-5s %-5s", "3", matrix.get(0),
			matrix.get(1), matrix.get(2), matrix.get(3)));
	fw.write("\n");
	fw.write(String.format("%-5s %-5s %-5s %-5s %-5s", "4", matrix.get(4),
			matrix.get(5), matrix.get(6), matrix.get(7)));
	fw.write("\n");
	fw.write(String.format("%-5s %-5s %-5s %-5s %-5s", "5", matrix.get(8),
			matrix.get(9), matrix.get(10), matrix.get(11)));
	fw.write("\n");
	fw.close();
	} catch (IOException e) {
	e.printStackTrace();
	}
	}

	private void clearStaticVariables() {
	for (int i = 0; i < Loader.diskAvailability.length; i++) {
	Loader.diskAvailability[i] = null;
	}
	Loader.emptyPagesOnDisk = 255;
	Loader.fileEndedExecuteAnyways = false;
	Loader.NOJITB = 0;
	Loader.executionEnded = false;
	CPU.lastJob = false;
	Scheduler.matrixMetric = 0;
	}

	private void HandleError(int warningCode) {
	if (warningCode == ErrorHandler.FILE_NOT_FOUND) {
	System.out.println("File not found");
	}
	if (warningCode == ErrorHandler.SYSTEM_ERROR) {
	System.out.println("System Internal Error.");
	}
	}

	static ErrorHandler errorHandler = new ErrorHandler();

	/* Execution starting point */
	public static void main(String[] args) {
	/* check for number of arguments passed as a file name */
	String FileName = null;

	system systemObject = new system();
	if (args.length > 0) {
	FileName = args[0];
	}
	/* Begin the system by passing the File Name */
	try {
	systemObject.begin(FileName);
	} catch (Exception e) {
	errorHandler.HandleError(ErrorHandler.SYSTEM_ERROR);
	}
	}
}

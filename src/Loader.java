/*Name: Gautham Brahma Ponnaganti.
 *Course No.: CS5323
 *Assignment title:OS Project PHASE II
 *Date:4-28-2016
 *Global Variables:
 *-JobID: Used to keep track of job. It will always be 1 in this case because there is no multiprogramming and we give
 *only one job as input once. 
 *-buffer: Used for block transfer operation.
 *-memory: used to reference memory so that it can load user programs in the memory and call CPU for execution
 *-errorHandler: This is used to call the error handler because it can be used in case an error occurs
 *-_ProgramPagesOnDisk,_DataPagesOnDisk,_OutputPagesOnDisk:used to keep track of the pages in memory.
 *-NOLITB:used to know the end of the file and hence know that the file ended before hand.
 *-executionEnded:used to keep track of the end of the execution of the last job.
 *-Locus:used to keep track of where in the test file we currently are.This is used for error Handling.
 *
 *
 *Brief description: This is used to do RD and WR operations which involves memory and console as CPU would 
 *not be directly associated with these operations
 *
 *Changes in Phase II: We are changing the was of taking input and loading in the memory from file and load it into disk
 *first instead of memory. Pages are created here and are put in the memory locations. whenever a job execution completes
 *the control will branch back to here from which we will load into the disk space created.
 *
 *Changes in PhaseIII:None
 */

/*startAddress,Location,TraceFlag,Length are decoded to decimal values because they are easy to manage. The will be converted to 
 hexadecimal values whenever necessary*/

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;

public class Loader {

	private int jobID = 0;
	String Status = "";
	int currentLine = 0;
	private Memory memory = new Memory();
	private Disk disk = new Disk();
	Scheduler scheduler = new Scheduler();
	ErrorHandler errorHandler = new ErrorHandler();
	public int pagesNeeded = 0, outputLines = 0;
	long currentFilePointer;
	String StringBuilder;
	ArrayList<String> PageBuilder = new ArrayList<String>();
	int pageEntry = 0;
	ArrayList<String> MetaData = new ArrayList<String>();
	int pageEntryCheckPoint;
	long FilePointerOfLastModule;
	boolean stopReadingTheCurrentJob = false;
	LinkedList<PCB> ListOfPCB = new LinkedList<PCB>();
	PCB pcb;
	ArrayList<String> _ProgramPagesOnDisk = new ArrayList<String>();
	ArrayList<String> _DataPagesOnDisk = new ArrayList<String>();
	ArrayList<String> _OutputPagesOnDisk = new ArrayList<String>();
	public static String[] diskAvailability = new String[256];
	public static int emptyPagesOnDisk = 255;
	public static boolean fileEndedExecuteAnyways = false;
	/* number of lines in test batch */
	public int NOLITB;
	public static int NOJITB;

	public static boolean executionEnded = false;

	/*
	 * locus defines the current point with in each job where the execution is
	 * happening it can be the following JOB,
	 * JOB_PAYLOAD_START,JOB_PAYLOAD,JOB_PAYLOAD_END,DATA,DATA_PAYLOAD,FIN
	 */
	public String locus = "FIN";

	/* This method will take value in hexadecimal and return a binary value */
	public String HEXBIN(String hexValue) {
	String value = new BigInteger(hexValue, 16).toString(2);
	return String.format("%32s", value).replace(" ", "0");
	}

	public static int getHolesNo() {
	String currentState = "true";
	int numberOfHoles = 0;
	for (int i = 0; i < diskAvailability.length; i++) {
	if (!diskAvailability[i].equals(currentState)) {
	currentState = diskAvailability[i];
	numberOfHoles++;
	}
	}
	return numberOfHoles / 2;
	}

	public static int getAvgHoleSize() {
	String currentState = "true";
	int numberOfHoles = 0;
	int holeSize = 0;
	for (int i = 0; i < diskAvailability.length; i++) {
	if (!diskAvailability[i].equals(currentState)) {
	currentState = diskAvailability[i];
	numberOfHoles++;
	}
	if (diskAvailability[i].equals("false")) {
	holeSize++;
	}
	}
	return holeSize / numberOfHoles;
	}

	public static int getPercentage() {
	int occSize = 0;
	for (int i = 0; i < diskAvailability.length; i++) {
	if (diskAvailability[i].equals("true")) {
	occSize++;
	}
	}
	return (occSize * 100) / 256;
	}

	public void loader(File fileName) {
	RandomAccessFile dataFromFile = null;
	String data = null;
	createProgressFile();
	int tempPointer = 0;
	int tempPointer2 = 0;
	int startAddress = 0, Location = 0, TraceFlag = 0, Length;
	try {
	// initialize the page table
	memory.initializePageTable("i");
	disk.intitalizePageTable();
	/*
	 * Initializing disk availability.By default all of disk is available
	 */
	for (int i = 0; i < diskAvailability.length; i++) {
	diskAvailability[i] = "true";
	}
	dataFromFile = new RandomAccessFile(fileName, "r");
	NOJITB++;
	while ((data = dataFromFile.readLine()) != null) {
	NOLITB++;
	if (data.trim().contains("**JOB")) {
	if (tempPointer2 - tempPointer > 2) {
	NOJITB++;
	}
	tempPointer = tempPointer2;
	}
	tempPointer2++;
	}

	// open the input file in read mode.
	dataFromFile = new RandomAccessFile(fileName, "r");
	while ((data = dataFromFile.readLine()) != null) {
	/* Making sure that the data doesn't have unwanted spaces */
	data = data.trim();
	currentLine++;
	currentFilePointer = dataFromFile.getFilePointer();
	if (data.startsWith("**")) {
	String commands[] = data.split("\\s+");
	if (commands[0].equals("**JOB")) {
	pageEntryCheckPoint = pageEntry;
	stopReadingTheCurrentJob = false;
	if (!locus.equals("FIN")) {
	ErrorHandler.setJobID(jobID);
	Status = errorHandler.HandleError(ErrorHandler.MISSING_FIN);
	HandleWarning(ErrorHandler.MISSING_FIN);
	}
	/* job */
	jobID++;
	pcb = new PCB();
	_ProgramPagesOnDisk.clear();
	_DataPagesOnDisk.clear();
	_OutputPagesOnDisk.clear();
	pcb.jobID = jobID;
	locus = "JOB";

	pagesNeeded = Integer.parseInt(commands[1], 16);
	/*
	 * Number of pages needed for output equals number of output lines
	 */
	if ((Integer.parseInt(commands[2], 16) % 4) == 0) {
	outputLines = (Integer.parseInt(commands[2], 16) / 4);
	} else {
	outputLines = (Integer.parseInt(commands[2], 16) / 4) + 1;
	}
	/*
	 * If we run out of disk space in the middle of read we can use this to get
	 * back to the start of the job module
	 */
	FilePointerOfLastModule = currentFilePointer;
	if ((pagesNeeded + outputLines) + 1 > emptyPagesOnDisk) {
	TimeToCallSheduler();
	} else {
	pageEntry = 0;
	}
	StringBuilder = "JobID:" + Integer.toString(jobID);
	MetaData.add(StringBuilder);
	StringBuilder = "";
	} else if (commands[0].equals("**DATA")) {
	if (!stopReadingTheCurrentJob) {
	if (locus.equals("DATA")) {
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.DOUBLE_DATA);
	HandleWarning(ErrorHandler.DOUBLE_DATA);
	}
	locus = "DATA";
	if (locus.equals("JOB")) {
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.NULL_JOB);
	HandleError(ErrorHandler.NULL_JOB);
	}
	/*
	 * To write to disk when page is not full but no more instructions
	 */
	if (PageBuilder.size() > 0) {
	for (int k = PageBuilder.size(); k < 16; k++) {
	PageBuilder.add("00000000");
	StringBuilder += "00000000" + "-";
	}
	while (pageEntry < 255) {
	if (diskAvailability[pageEntry].equals("true")) {
	diskAvailability[pageEntry] = "false";
	break;
	} else {
	if (pageEntry < 255)
		pageEntry++;
	else {
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.INSUFFICIENT_OUTPUT_SPACE);
	HandleError(ErrorHandler.INSUFFICIENT_OUTPUT_SPACE);
	}
	}
	}
	writePageToDisk(PageBuilder, pageEntry);
	_ProgramPagesOnDisk.add(Integer.toString(pageEntry));
	emptyPagesOnDisk--;
	MetaData.add("PageNo:" + pageEntry + "--" + StringBuilder);
	StringBuilder = "";
	PageBuilder.clear();
	if (pageEntry < 255)
		pageEntry++;
	}
	MetaData.add("**DATA");
	}
	} else if (commands[0].equals("**FIN")) {
	if (!stopReadingTheCurrentJob) {
	locus = "FIN";
	if (locus.equals("JOB_PAYLOAD_END") || locus.equals("DATA")) {
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.MISSING_DATA);
	HandleError(ErrorHandler.MISSING_DATA);
	}
	if (PageBuilder.size() > 0) {
	/*
	 * To write to disk when page is not full but no more Data
	 */
	for (int k = PageBuilder.size(); k < 16; k++) {
	PageBuilder.add("00000000");
	StringBuilder += "00000000" + "-";
	}
	while (pageEntry < 255) {
	if (diskAvailability[pageEntry].equals("true")) {
	diskAvailability[pageEntry] = "false";
	break;
	} else {
	if (pageEntry < 255)
		pageEntry++;
	else {
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.INSUFFICIENT_OUTPUT_SPACE);
	HandleError(ErrorHandler.INSUFFICIENT_OUTPUT_SPACE);
	}
	}
	}
	writePageToDisk(PageBuilder, pageEntry);
	_DataPagesOnDisk.add(Integer.toString(pageEntry));
	emptyPagesOnDisk--;
	MetaData.add("PageNo:" + pageEntry + "--" + StringBuilder);
	StringBuilder = "";
	PageBuilder.clear();
	if (pageEntry < 255)
		pageEntry++;
	}
	pageEntry = 0;
	if (outputLines < emptyPagesOnDisk) {
	while (outputLines > 0) {
	if (outputLines > 0 && pageEntry >= 255) {
	/* Break the infinite loop */
	break;
	}
	while (pageEntry < 255) {
	if (diskAvailability[pageEntry].equals("true")) {
	diskAvailability[pageEntry] = "false";
	_OutputPagesOnDisk.add(Integer.toString(pageEntry));
	outputLines--;
	emptyPagesOnDisk--;
	break;
	} else {
	if (pageEntry < 255)
		pageEntry++;
	else
		break;
	}
	}
	}
	}
	pcb.ProgramPagesOnDisk = new String[_ProgramPagesOnDisk.size()];
	_ProgramPagesOnDisk.toArray(pcb.ProgramPagesOnDisk);
	pcb.DataPagesOnDisk = new String[_DataPagesOnDisk.size()];
	_DataPagesOnDisk.toArray(pcb.DataPagesOnDisk);
	pcb.OutputPagesOnDisk = new String[_OutputPagesOnDisk.size()];
	_OutputPagesOnDisk.toArray(pcb.OutputPagesOnDisk);
	pcb.pageFaultBits = new ArrayList<String>();
	for (int i = 0; i < pcb.ProgramPagesOnDisk.length; i++) {
	pcb.pageFaultBits.add("000");
	}
	pcb.startedWritingTrace = false;
	pcb.numberOfChancesCPU = 0;
	try {
	FileWriter statusWrite2 = new FileWriter("progressFile.txt", true);
	statusWrite2.write("\n");
	statusWrite2
			.write(String.format("%-40s %-15s", "JobID loaded(DEC):", jobID));
	statusWrite2.write("\n");
	statusWrite2.write(String.format("%-40s %-15s",
			"Clock value during load(Decimal):", CPU.loadtracker));
	statusWrite2.write("\n");
	statusWrite2
			.write("------------------------------------------------------");
	statusWrite2.close();
	} catch (IOException e) {
	System.out.print("");
	}
	ListOfPCB.add(pcb);
	if (currentLine == NOLITB) {
	fileEndedExecuteAnyways = true;
	TimeToCallSheduler();
	}
	MetaData.add("**FIN");
	}
	}
	} else {
	if (!stopReadingTheCurrentJob) {
	/* Check Data for illegal characters */
	checkInputData(data);
	/* Where we get an empty line the read should end */
	if (data.length() == 0) {
	}
	/*
	 * Data identified as First Record if it's length is 5 bits
	 */
	if (data.length() == 5) {
	if (locus.equals("FIN")) {
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.MISSING_JOB_TAG);
	HandleError(ErrorHandler.MISSING_JOB_TAG);
	}
	locus = "JOB_PAYLOAD_START";
	Location = Integer.parseInt(data.substring(0, 2), 16);
	pcb.PC = Location;
	Length = Integer.parseInt(data.substring(3, 5), 16);
	if (Length > 255) {
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.PROGRAM_SIZE_TOO_LARGE);
	HandleError(ErrorHandler.PROGRAM_SIZE_TOO_LARGE);
	}
	}
	/*
	 * Data identified as Last Record if it's length is 4 bits
	 */
	if (data.length() == 4) {
	if (locus.equals("JOB_PAYLOAD_START")) {
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.MISSING_PROGRAM);
	HandleError(ErrorHandler.MISSING_PROGRAM);
	} else {
	locus = "JOB_PAYLOAD_END";
	}
	startAddress = Integer.parseInt(data.substring(0, 2), 16);
	if (startAddress > 255 || startAddress < 0) {
	// err.setJobID(jobID);
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.ADDRESS_OUT_OF_RANGE);
	}
	TraceFlag = Integer.parseInt(data.substring(3, 4));
	pcb.traceFlag = TraceFlag;
	if (TraceFlag != 0 && TraceFlag != 1) {
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.INVALID_TRACE_FLAG);
	HandleError(ErrorHandler.INVALID_TRACE_FLAG);
	}
	if (disk.spaceLeft > 0) {

	}
	}
	/* Condition to handle null trace */
	if (data.length() == 2) {
	if (locus.equals("JOB_PAYLOAD")) {
	ErrorHandler.setJobID(jobID);
	}
	}
	/*
	 * Data identified as Data Record if it's length is 32 bits
	 */
	if (data.length() == 32 || data.length() == 8 || data.length() == 16
			|| data.length() == 24) {

	if (locus.equals("DATA") || locus.equals("DATA_PAYLOAD")) {
	locus = "DATA_PAYLOAD";
	} else if (locus.equals("JOB_PAYLOAD_START")
			|| locus.equals("JOB_PAYLOAD")) {
	locus = "JOB_PAYLOAD";
	} else if (locus.equals("JOB_PAYLOAD_END") && data.length() == 32) {
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.MISSING_DATA_TAG);
	/*
	 * We append the data tag so before we do we write instructions to disk
	 */
	if (PageBuilder.size() > 0) {
	for (int k = PageBuilder.size(); k < 16; k++) {
	PageBuilder.add("00000000");
	StringBuilder += "00000000" + "-";
	}
	while (pageEntry < 255) {
	if (diskAvailability[pageEntry].equals("true")) {
	diskAvailability[pageEntry] = "false";
	break;
	} else {
	if (pageEntry < 255)
		pageEntry++;
	else {
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.INSUFFICIENT_OUTPUT_SPACE);
	HandleError(ErrorHandler.INSUFFICIENT_OUTPUT_SPACE);
	}
	}
	}
	writePageToDisk(PageBuilder, pageEntry);
	_ProgramPagesOnDisk.add(Integer.toString(pageEntry));
	emptyPagesOnDisk--;
	MetaData.add("PageNo:" + pageEntry + "--" + StringBuilder);
	StringBuilder = "";
	PageBuilder.clear();
	if (pageEntry < 255)
		pageEntry++;
	}
	HandleWarning(ErrorHandler.MISSING_DATA_TAG);
	} else {
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.ILLEGAL_BATCH_FORMAT);
	HandleError(ErrorHandler.ILLEGAL_BATCH_FORMAT);
	}
	for (int i = 0; i < data.length(); i = i + 8) {
	PageBuilder.add(data.substring(i, i + 8));
	StringBuilder += data.substring(i, i + 8) + "-";
	if (PageBuilder.size() > 15) {
	while (pageEntry < 255) {
	if (diskAvailability[pageEntry].equals("true")) {
	diskAvailability[pageEntry] = "false";
	break;
	} else {
	if (pageEntry < 255)
		pageEntry++;
	else {
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.INSUFFICIENT_OUTPUT_SPACE);
	HandleError(ErrorHandler.INSUFFICIENT_OUTPUT_SPACE);
	}
	}
	}
	writePageToDisk(PageBuilder, pageEntry);
	emptyPagesOnDisk--;
	if (locus.equals("JOB_PAYLOAD")) {
	_ProgramPagesOnDisk.add(Integer.toString(pageEntry));
	}
	if (locus.equals("DATA_PAYLOAD")) {
	_DataPagesOnDisk.add(Integer.toString(pageEntry));
	}
	MetaData.add("PageNo:" + pageEntry + "--" + StringBuilder);
	StringBuilder = "";
	PageBuilder.clear();
	if (pageEntry < 255)
		pageEntry++;
	}
	}
	}
	}
	}
	}
	} catch (Exception e) {
	e.printStackTrace();
	}

	}

	private void createProgressFile() {
	try {
	String filename = "progressFile.txt";
	// TODO: remove false
	FileWriter fw = new FileWriter(filename);
	fw.close();
	} catch (IOException e) {
	e.printStackTrace();
	}
	}

	private void TimeToCallSheduler() {
	scheduler.manageDiskReference(disk, ListOfPCB);
	if ((pagesNeeded + outputLines) + 1 > emptyPagesOnDisk) {
	/*
	 * new process is bigger than the terminated one. more processes need to go
	 */
	TimeToCallSheduler();
	} else {
	pageEntry = 0;
	}
	}

	private void HandleError(int ErrorCode) {
	/*
	 * We will make the diskAvailability of the current job that has error as
	 * true so that these locations are available to the next jobs and the error
	 * job gets overwritten
	 */
	pageEntry = pageEntryCheckPoint;
	PageBuilder.clear();
	int index = 0;
	/* delete this job from metadata */
	for (int i = 0; i < MetaData.size(); i++) {
	if (MetaData.get(i).contains("JobID:" + jobID)) {
	index = i;
	}
	}
	int length = MetaData.size();
	if (index != 0) {
	for (int i = index; i < length; i++) {
	MetaData.remove(index);
	}
	}
	/* stop reading rest of the job and start from the start of next job */
	stopReadingTheCurrentJob = true;
	locus = "FIN";
	}

	private void HandleWarning(int warningCode) {
	if (warningCode == ErrorHandler.MISSING_FIN) {
	StringBuilder = "**FIN";
	MetaData.add(StringBuilder);
	StringBuilder = "";
	}
	if (warningCode == ErrorHandler.DOUBLE_DATA) {
	MetaData.remove(MetaData.size() - 1);
	}
	if (warningCode == ErrorHandler.MISSING_DATA_TAG) {
	MetaData.add("**DATA");
	locus = "DATA";
	}
	}

	private void writePageToDisk(ArrayList<String> pageBuilder2,
			int pageEntry2) {
	disk.writeToDisk(pageBuilder2, pageEntry2);
	}

	/*
	 * This is to check if the given values are deviating from hex integers.
	 * This will handle the invalid user input exception given in the
	 * specification. ASCII characters 64 to 71 represent digits 0 to 9 and 96
	 * to 103 represent a to f and 32 represents space
	 */
	private void checkInputData(String data) {
	for (char input : data.toCharArray()) {
	int code = (int) input;
	if ((code > 47 && code < 58) || (code > 64 && code < 71)
			|| (code > 96 && code < 103)
			|| code == 32 && data.split("\\s+").length == 2) {
	continue;
	}
	ErrorHandler.setJobID(jobID);
	errorHandler.HandleError(ErrorHandler.ILLEGAL_CHAR);
	HandleError(ErrorHandler.ILLEGAL_CHAR);
	}

	}

	public Memory getMemory() {
	return memory;
	}

}


/*Author: Gautham Brahma Ponnaganti.
 *Global Variables:
 *-errorHandler: this is used to invoke the error handler module.
 *-Memory: This is used to get a reference of memory object. 
 *-IO Handler: This is used for IO operations when we have a RD or a WR operation.
 *-OneClockCycleInTime: This will specify a quantum unit of time.
 *-clock:This is the clock which will keep track of virtual time units during execution.
 *-executionTime&IO time: This will keep track of the time taken for execution and time taken for IO in virtual time units.
 *-AbeforeExecution: This is used to keep track of the value of Accumulator before execution
 *-ALU:used to invoke arithmetic operations class 
 *-Code: Op code value in hex
 *-OpCode: opcode value in user readable format
 *-indirectAdressing:This is set if we detect an indirect addressing bit
 *-addressOfOperand:used for address of the operand for loading the operand into the accumulator.
 *-instructionRegister: This is used to store the instruction to be executed in binary format.
 *-PC: Program counter.
 *-IndexRegister: Used in effective address calculation in case of indexed addressing.
 *-Accumulator,R1-R11,Target: These are general purpose registers along with the above registers.
 *-GPR:used as a list of general Purpose registers
 *
 *Brief description: This is the CPU subsystem which takes the help of Arithmetic Operation class for all ALU 
 *related operations that happen in the system.
 *
 *Changes In Phase II: Made changes in the RD and WR so that they now write to the disk instead of the screen.
 *
 *Changes In Phase III:Recording additional metrics related to subqueues.
 */

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;

public class CPU {

	ErrorHandler errorHandler = new ErrorHandler();
	private Memory memory;
	final int OneClockCycleInTime = 1;
	int chancesAtCPU = 0;
	int clock;
	ArrayList<Integer> suspectedInfiniteJobID = new ArrayList<Integer>();
	int timeJobEnteredTheSystem = 0, cumulativeTimeByJob = 0, timeout = 0,
			pageFaultHandlingTime = 0, timeQuantum = 0, numberOfTurns = 0;
	int executionTime = 0, ioTime = 0;
	/* This variable will be used when writing to trace file */
	String AbeforeExecution = null;
	ArthematicOperations ALU = new ArthematicOperations();
	String[] nullString = {""};
	String Code;
	String effectiveAddress = null;
	opCode OpCode;
	int indirectAdressing;
	String addressOfOperand;
	Disk disk;
	int debugCounter = 0;
	public static ArrayList<String> memoryDumpArray = new ArrayList<String>();
	boolean ReturnToScheduler = false;
	boolean hasMore = false;
	LinkedList<PCB> runningPCB = new LinkedList<PCB>();
	ArrayList<String> tempArrayList = new ArrayList<String>();
	String indexToAdd;
	int grossRuntime = 0, grossIOTime = 0, grossExecutionTime = 0,
			grossTimeInSystem = 0, grossPFHtime = 0;
	int CPUIdleTime = 0, TimeLost = 0, numberOfJobNormal = 0,
			numberOfJobsAbnormal = 0;
	int timeLostInfiniteSuspect = 0, IDOfjobsConsideredInfinite = 0,
			numberOfPageaults = 0;
	int PercentgeOccupie = 0, HolesInfo = 0;
	int suspectedInputTimeout = 0;
	int maxNumberOfJobs = 0;
	int currentLevel = 0;
	/* Register 0 */
	String instructionRegister;
	String _instructionReg;
	int x = 0;
	/* Register 1 */
	int PC;
	int dataPageToLoad, dataPageOffset;
	int diskOutPageToWrite;
	int VirtualPC;
	/* Register 2 Accumulator */
	String Accumulator;
	/* Register 3 index register */
	String IndexRegister;
	/* Register 4 */
	String R1;
	/* Register 5 */
	String R2;
	/* Register 6 */
	String R3;
	/* Register 7 */
	String R4;
	/* Register 8 */
	String R5;
	/* Register 9 */
	String R6;
	/* Register 10 */
	String R7;
	/* Register 11 */
	String R8;
	/* Register 12 */
	String R9;
	/* Register 13 */
	String R10;
	/* Register 14 */
	String R11;
	/* Register 15 */
	String[] GPR = new String[16];
	String Target = "00000000000000000000000000000000";
	int previousClockVal = 0;
	int previousClockVal2 = 0;
	int previousClockVal3 = 0;
	int normalTermination = 1;
	// public static int TotalNumberOfJobs = 0;
	int legitJobTime = 0;
	public static int ideleTime = 0;

	public static int loadtracker = 0;

	int maxSubqueue1Size = 0;
	int maxSubqueue2Size = 0;
	int maxSubqueue3Size = 0;
	int maxSubqueue4Size = 0;
	float cumulativeSubQueu1size = 0;
	float cumulativeSubQueu2size = 0;
	float cumulativeSubQueu3size = 0;
	float cumulativeSubQueu4size = 0;
	int queueCount = 0;

	public enum opCode {
	HLT, // halt
	LD, // Load
	ST, // Store
	AD, // Add
	SB, // Subtract
	MPY, // Multiply
	DIV, // Divide
	SHL, // Shift left
	SHR, // Shift Right
	BRM, // Branch on minus
	BRP, // Branch on plus
	BRZ, // Branch on zero
	BRL, // Branch on link
	AND, OR, RD, // Read
	WR, // Write
	DMP// Dump memory
	}

	public static boolean lastJob = false;

	/*
	 * with setMemoryObject we are passing the reference of the memory so that
	 * the Memory address and Memory Buffer registers can use this reference to
	 * fetch the instructions from the memory
	 */
	public void setMemoryObject(Memory _memory) {
	this.memory = _memory;
	}

	public int getclock() {
	return clock;
	}

	public boolean cpu(int ProgramCounter, int traceSwitch,
			LinkedList<PCB> runningQueue, Disk _disk) {
	try {
	runningPCB.add(runningQueue.get(0));
	ReturnToScheduler = false;
	if (runningQueue.getFirst().currentOperation.equals("NEW")) {
	timeJobEnteredTheSystem = clock;
	cumulativeTimeByJob = 0;
	timeQuantum = 0;
	currentLevel = 1;
	numberOfTurns = 1;
	chancesAtCPU = 0;
	memoryDumpArray.clear();
	} else {
	timeJobEnteredTheSystem = runningQueue.getFirst().timeJobEnteredTheSystem;
	cumulativeTimeByJob = runningQueue.getFirst().cumilativeTimeUsedByJob;
	timeQuantum = 0;
	pageFaultHandlingTime = runningQueue.getFirst().pageFaultHandlingTime;
	numberOfTurns = runningQueue.getFirst().numberOfTurns;
	currentLevel = runningQueue.getFirst().currentSubqueueLevel;
	chancesAtCPU = runningQueue.getFirst().numberOfChancesCPU + 1;
	restoreStateOfProcess();
	}
	FileWriter statusWrite = new FileWriter("progressFile.txt", true);
	if ((clock - previousClockVal) > 10000) {
	previousClockVal = clock;
	statusWrite.write("\n");
	for (int i = 0; i < runningPCB.getFirst().SystemStatus.size(); i++) {
	statusWrite.write(runningPCB.getFirst().SystemStatus.get(i).toString());
	statusWrite.write("\n");
	}
	statusWrite.close();
	}
	if ((clock - previousClockVal2) > 100) {
	previousClockVal2 = clock;
	if (Scheduler.subqueue1Size > maxSubqueue1Size) {
	maxSubqueue1Size = Scheduler.subqueue1Size;
	}
	if (Scheduler.subqueue2Size > maxSubqueue2Size) {
	maxSubqueue2Size = Scheduler.subqueue2Size;
	}
	if (Scheduler.subqueue3Size > maxSubqueue3Size) {
	maxSubqueue3Size = Scheduler.subqueue3Size;
	}
	if (Scheduler.subqueue4Size > maxSubqueue4Size) {
	maxSubqueue4Size = Scheduler.subqueue4Size;
	}
	cumulativeSubQueu1size += Scheduler.subqueue1Size;
	cumulativeSubQueu2size += Scheduler.subqueue2Size;
	cumulativeSubQueu3size += Scheduler.subqueue3Size;
	cumulativeSubQueu4size += Scheduler.subqueue4Size;
	queueCount += 1;
	}
	if ((clock - previousClockVal3) > 1200) {
	previousClockVal3 = clock;
	String filename = "MLFBQ.txt";
	FileWriter fw5 = new FileWriter(filename, true);
	fw5.write(String.format("%-25s %-10s", "Clock value(Hex):", clock));
	fw5.write("\n");
	if (!Scheduler.contentOfSubqueue1.isEmpty()) {
	for (int k = 0; k < Scheduler.contentOfSubqueue1.size(); k++) {
	tempArrayList.add(Scheduler.contentOfSubqueue1.get(k).toString());
	// fw5.write(Scheduler.contentOfSubqueue1.get(k).toString());
	}
	fw5.write(String.format("%-25s %-10s", "Contents of subqueue1(DEC):",
			tempArrayList));
	} else {
	fw5.write(String.format("%-25s %-10s", "Contents of subqueue1(DEC):",
			"Empty"));
	}
	fw5.write("\n");
	tempArrayList.clear();
	if (!Scheduler.contentOfSubqueue2.isEmpty()) {
	for (int k = 0; k < Scheduler.contentOfSubqueue2.size(); k++) {
	// fw5.write(Scheduler.contentOfSubqueue2.get(k).toString());
	tempArrayList.add(Scheduler.contentOfSubqueue2.get(k).toString());
	}
	fw5.write(String.format("%-25s %-10s", "Contents of subqueue2(DEC):",
			tempArrayList));
	} else {
	fw5.write(String.format("%-25s %-10s", "Contents of subqueue2(DEC):",
			"Empty"));
	}
	fw5.write("\n");
	tempArrayList.clear();
	if (!Scheduler.contentOfSubqueue3.isEmpty()) {
	for (int k = 0; k < Scheduler.contentOfSubqueue3.size(); k++) {
	// fw5.write(Scheduler.contentOfSubqueue3.get(k).toString());
	tempArrayList.add(Scheduler.contentOfSubqueue3.get(k).toString());
	}
	fw5.write(String.format("%-25s %-10s", "Contents of subqueue3(DEC):",
			tempArrayList));
	} else {
	fw5.write(String.format("%-25s %-10s", "Contents of subqueue3(DEC):",
			"Empty"));
	}
	fw5.write("\n");
	tempArrayList.clear();
	if (!Scheduler.contentOfSubqueue4.isEmpty()) {
	for (int k = 0; k < Scheduler.contentOfSubqueue4.size(); k++) {
	// fw5.write(Scheduler.contentOfSubqueue4.get(k).toString());
	tempArrayList.add(Scheduler.contentOfSubqueue4.get(k).toString());
	}
	fw5.write(String.format("%-25s %-10s", "Contents of subqueue4(DEC):",
			tempArrayList));
	} else {
	fw5.write(String.format("%-25s %-10s", "Contents of subqueue4(DEC):",
			"Empty"));
	}
	fw5.write("\n");
	fw5.write("\n");
	tempArrayList.clear();
	fw5.close();
	}
	timeout = 0;
	this.disk = _disk;
	VirtualPC = ProgramCounter;
	boolean stopExecution = false;
	while (!stopExecution && !ReturnToScheduler) {
	PC = convertVirtualAddressToPhysical(VirtualPC);
	if (PC == -1) {
	ReturnToScheduler = true;
	runningPCB.getFirst().currentOperation = "Error";
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.ADDRESS_OUT_OF_RANGE);
	runningPCB.getFirst().errorDescription = "Address Out Of Range";
	hasMore = false;
	writeOutput("Read");
	runningPCB.removeFirst();
	TimeLost += cumulativeTimeByJob;
	return false;
	}
	instructionRegister = fetchInstruction(PC);
	_instructionReg = instructionRegister;
	decodeInstruction();

	if (Integer.parseInt(Code, 16) > 17) {
	ReturnToScheduler = true;
	runningPCB.getFirst().currentOperation = "Error";
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.INVALID_OPCODE);
	runningPCB.getFirst().errorDescription = "Invalid Opcode";
	hasMore = false;
	writeOutput("Invalid Opcode");
	runningPCB.removeFirst();
	TimeLost += cumulativeTimeByJob;
	return false;
	}
	AbeforeExecution = GPR[Integer.parseInt(Accumulator, 2)];
	stopExecution = executeInstruction();
	if (runningPCB.getFirst().traceFlag == 1) {
	if (runningQueue.getFirst().startedWritingTrace == false) {
	FileWriter fww = new FileWriter("trace-JobID-"
			+ Integer.toString(runningQueue.getFirst().jobID) + ".txt");
	fww.close();
	}
	FileWriter fw = new FileWriter("trace-JobID-"
			+ Integer.toString(runningQueue.getFirst().jobID) + ".txt", true);
	if (runningQueue.getFirst().startedWritingTrace == false) {
	runningQueue.getFirst().startedWritingTrace = true;
	fw.write(String.format("%4s %15s %15s %15s %15s %n", "PC(DEC)",
			"Instruction(HEX)", "A Before(HEX)", "A After(HEX)", "EA(HEX)"));
	}
	String temp1 = binhex(instructionRegister);
	if (temp1 == null || temp1 == "") {
	temp1 = "0000000";
	}
	if (AbeforeExecution == null || AbeforeExecution == "") {
	AbeforeExecution = "0";
	}
	String temp2 = binhex(effectiveAddress);
	if (temp2 == null || temp2 == "") {
	temp2 = "0000";
	}
	if (Accumulator == null || Accumulator == "") {
	Accumulator = "0";
	}
	if (GPR[Integer.parseInt(Accumulator, 2)] == null
			|| GPR[Integer.parseInt(Accumulator, 2)] == "") {
	GPR[Integer.parseInt(Accumulator, 2)] = "0000";
	}
	fw.write(String.format("%4d %15s %15s %15s %15s %n", PC,
			binhex(_instructionReg), binhex(AbeforeExecution),
			binhex(GPR[Integer.parseInt(Accumulator, 2)]), temp2));
	fw.close();
	}
	if (PC > 255) {
	ReturnToScheduler = true;
	runningPCB.getFirst().currentOperation = "Error";
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.ADDRESS_OUT_OF_RANGE);
	runningPCB.getFirst().errorDescription = "Address Out Of Range";
	hasMore = false;
	writeOutput("Read");
	runningPCB.removeFirst();
	TimeLost += cumulativeTimeByJob;
	return false;
	}
	VirtualPC = VirtualPC + 1;
	clock += OneClockCycleInTime;
	loadtracker += OneClockCycleInTime;
	cumulativeTimeByJob += OneClockCycleInTime;
	timeQuantum += OneClockCycleInTime;
	grossRuntime += OneClockCycleInTime;
	timeout += 1;
	if (timeQuantum >= Scheduler.allowedQuantum
			&& !(Code.equals("10") || Code.equals("0f") || Code.equals("00"))) {
	ReturnToScheduler = true;
	runningPCB.getFirst().currentOperation = "TimeOut";
	numberOfTurns += 1;
	writeCurrentStateToPCB("TimeOut");
	hasMore = true;
	}
	if (cumulativeTimeByJob >= 400000) {
	suspectedInputTimeout = cumulativeTimeByJob;
	ReturnToScheduler = true;
	runningPCB.getFirst().currentOperation = "Error";
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.SUSPECTED_INFINITE_JOB);
	runningPCB.getFirst().errorDescription = "Suspected Infinite Job";
	suspectedInfiniteJobID.add(runningPCB.getFirst().jobID);
	hasMore = false;
	writeOutput("Infinite");
	if (lastJob == true) {
	ideleTime = runningPCB.getFirst().iotime;
	writeAggregateResults();
	}
	runningPCB.removeFirst();
	TimeLost += cumulativeTimeByJob;
	return false;
	}
	// writer.close();
	}
	// writer.flush();
	} catch (NumberFormatException e) {
	ReturnToScheduler = true;
	runningPCB.getFirst().currentOperation = "Error";
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.ILLEGAL_CHAR);
	runningPCB.getFirst().errorDescription = "Illegal Character";
	hasMore = false;
	writeOutput("Illegal Character");
	TimeLost += cumulativeTimeByJob;
	runningPCB.removeFirst();
	return false;
	} catch (IOException e) {
	ReturnToScheduler = true;
	runningPCB.getFirst().currentOperation = "Error";
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.IO_EXCEPTION);
	runningPCB.getFirst().errorDescription = "Error";
	hasMore = false;
	writeOutput("IO");
	TimeLost += cumulativeTimeByJob;
	runningPCB.removeFirst();
	return false;
	} catch (NullPointerException e) {
	ReturnToScheduler = true;
	runningPCB.getFirst().currentOperation = "Error";
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.IO_EXCEPTION);
	runningPCB.getFirst().errorDescription = "Error";
	hasMore = false;
	writeOutput("IO");
	TimeLost += cumulativeTimeByJob;
	runningPCB.removeFirst();
	return false;
	}

	runningPCB.removeFirst();
	return hasMore;
	}

	private int convertVirtualAddressToPhysical(int virtualProgCount) {
	int virtualAddressInDecimal = virtualProgCount;
	String virtualAddressIndex = String.format("%02X", virtualAddressInDecimal);
	/*
	 * Page Table being an array is indexed in decimal. The hex value needs to
	 * be converted to decimal
	 */
	String PageTableIndex = virtualAddressIndex.substring(0, 1);
	String Offset = virtualAddressIndex.substring(1, 2);
	int PageTableIndexDec = Integer.parseInt(PageTableIndex, 16);
	/* condition will be true if the page is not in memory */
	if (runningPCB.getFirst().pageFaultBits.get(PageTableIndexDec)
			.substring(0, 1).equals("0")) {
	String pageNumberOnMemtoReplace = "";
	String pageNumberOnDisktoReplace;
	String pageNumberOnDisktoBring;
	int pageIndexToReplace = -1;
	boolean isPageClean = true;
	cumulativeTimeByJob += 5;
	timeQuantum += 5;
	pageFaultHandlingTime += 5;
	grossRuntime += 5;
	grossPFHtime += 5;
	/* Victim selection stage 1 not used and clean */
	for (int i = 0; i < runningPCB.getFirst().ProgramPagesOnMem.length; i++) {
	if (runningPCB.getFirst().pageFaultBits.get(i).substring(0, 1).equals("1")
			&& runningPCB.getFirst().pageFaultBits.get(i).substring(1, 2)
					.equals("0")
			&& runningPCB.getFirst().pageFaultBits.get(i).substring(2, 3)
					.equals("0")) {
	pageIndexToReplace = i;
	}
	}
	/* Victim selection Stage 2 used but still clean */
	if (pageIndexToReplace == -1) {
	for (int i = 0; i < runningPCB.getFirst().ProgramPagesOnMem.length; i++) {
	if (runningPCB.getFirst().pageFaultBits.get(i).substring(0, 1).equals("1")
			&& runningPCB.getFirst().pageFaultBits.get(i).substring(1, 2)
					.equals("1")
			&& runningPCB.getFirst().pageFaultBits.get(i).substring(2, 3)
					.equals("0")) {
	pageIndexToReplace = i;
	}
	}
	}
	/* Victim selection Stage 3 used and dirty */
	if (pageIndexToReplace == -1) {
	for (int i = 0; i < runningPCB.getFirst().ProgramPagesOnMem.length; i++) {
	if (runningPCB.getFirst().pageFaultBits.get(i).substring(0, 1).equals("1")
			&& runningPCB.getFirst().pageFaultBits.get(i).substring(1, 2)
					.equals("1")
			&& runningPCB.getFirst().pageFaultBits.get(i).substring(2, 3)
					.equals("1")) {
	pageIndexToReplace = i;
	isPageClean = false;
	}
	}
	}
	if (pageIndexToReplace == -1) {
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.OUT_OF_BOUNDS);
	hasMore = false;
	ReturnToScheduler = true;
	TimeLost += cumulativeTimeByJob;
	return -1;
	} else {
	try {
	/* Page is dirty copy it to disk */
	if (isPageClean == false) {
	String contentInMemPage = "";
	pageNumberOnMemtoReplace = runningPCB
			.getFirst().ProgramPagesOnMem[pageIndexToReplace];
	pageNumberOnDisktoReplace = runningPCB
			.getFirst().ProgramPagesOnDisk[pageIndexToReplace];
	for (int i = 0; i < 16; i++) {
	contentInMemPage += memory.Mem("READ",
			Integer.parseInt(pageNumberOnMemtoReplace) * 16 + i, "");
	}
	for (int i = (Integer.parseInt(pageNumberOnDisktoReplace)
			* 16), j = 0; i < (Integer.parseInt(pageNumberOnDisktoReplace) * 16)
					+ 16; i++, j = j + 8) {
	disk.disk[i] = contentInMemPage.substring(j, j + 8);
	}
	runningPCB.getFirst().pageFaultBits.set(pageIndexToReplace,
			runningPCB.getFirst().pageFaultBits.get(pageIndexToReplace)
					.substring(0, 2) + "0");
	}
	/* replace the page with the page from disk */
	String contentInDisk = "";
	pageNumberOnDisktoBring = runningPCB
			.getFirst().ProgramPagesOnDisk[PageTableIndexDec];
	for (int i = Integer.parseInt(pageNumberOnDisktoBring)
			* 16; i < (Integer.parseInt(pageNumberOnDisktoBring) * 16)
					+ 16; i++) {
	contentInDisk += disk.disk[i];
	}
	pageNumberOnMemtoReplace = runningPCB
			.getFirst().ProgramPagesOnMem[pageIndexToReplace];
	for (int i = Integer.parseInt(pageNumberOnMemtoReplace)
			* 16, j = 0; i < (Integer.parseInt(pageNumberOnMemtoReplace) * 16)
					+ 16; i++, j = j + 8) {
	memory.Mem("WRIT", i, contentInDisk.substring(j, j + 8));
	}
	/* update the bits for the page replaced and fetched */
	runningPCB.getFirst().pageFaultBits.set(pageIndexToReplace, "000");
	runningPCB.getFirst().pageFaultBits.set(PageTableIndexDec, "100");
	runningPCB.getFirst().ProgramPagesOnMem[pageIndexToReplace] = null;
	runningPCB
			.getFirst().ProgramPagesOnMem[PageTableIndexDec] = pageNumberOnMemtoReplace;
	} catch (Exception e) {
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.OUT_OF_BOUNDS);
	hasMore = false;
	ReturnToScheduler = true;
	TimeLost += cumulativeTimeByJob;
	return -1;
	}
	}
	}
	int FrameNumber = Integer.parseInt(
			runningPCB.getFirst().ProgramPagesOnMem[PageTableIndexDec]);
	runningPCB.getFirst().pageFaultBits.set(PageTableIndexDec,
			"11" + runningPCB.getFirst().pageFaultBits.get(PageTableIndexDec)
					.substring(2, 3));
	String FrameNumberHex = String.format("%01X", FrameNumber);
	String PhysicalAddress = FrameNumberHex + Offset;
	return Integer.parseInt(PhysicalAddress, 16);
	}

	public String fetchInstruction(int ProgramCounter) {
	String instruction = Memory.Mem[ProgramCounter];
	return HEXBIN(instruction);
	}

	/*
	 * decode the 32 bit binary instruction to mine appropriate fields and
	 * opcode
	 */
	public void decodeInstruction() {
	if (instructionRegister.length() == 32) {
	indirectAdressing = Integer.parseInt(instructionRegister.substring(0, 1));
	Code = instructionRegister.substring(1, 8);
	Accumulator = instructionRegister.substring(8, 12);
	IndexRegister = instructionRegister.substring(12, 16);
	addressOfOperand = instructionRegister.substring(16, 32);
	Code = fetchOpcode(Code);
	OpCode = decodeOpcode(Code);
	}
	}

	/* calculating effective address and executing instruction */
	public boolean executeInstruction() {
	try {
	boolean returnValue = false;
	String virtualEffectiveAddress = "";
	/* calculating the effective address */
	if (indirectAdressing == 0) {
	/* direct addressing */
	if (IndexRegister.equals("0000")) {
	/* do nothing. The address is already effective address */
	/*
	 * We will keep a copy of virtual address to use for jump instructions
	 */
	virtualEffectiveAddress = addressOfOperand;
	addressOfOperand = convertAddFromVirtualToReal(addressOfOperand);
	effectiveAddress = addressOfOperand;
	}
	/* direct addressing && index addressing */
	else {
	/*
	 * We will keep a copy of virtual address to use for jump instructions
	 */
	virtualEffectiveAddress = addressOfOperand;
	indexToAdd = GPR[Integer.parseInt(IndexRegister, 2)];
	effectiveAddress = addAddresses(indexToAdd, addressOfOperand);
	virtualEffectiveAddress = effectiveAddress;
	effectiveAddress = convertAddFromVirtualToReal(effectiveAddress);
	}
	} else {
	/* indirect addressing */
	if (IndexRegister.equals("0000")) {
	/*
	 * We will keep a copy of virtual address to use for jump instructions
	 */
	virtualEffectiveAddress = addressOfOperand;
	addressOfOperand = convertAddFromVirtualToReal(addressOfOperand);
	try {
	effectiveAddress = memory.Mem("READ", Integer.parseInt(addressOfOperand, 2),
			R1);
	} catch (NumberFormatException e) {
	System.out.print("");
	}
	effectiveAddress = HEXBIN(effectiveAddress);
	virtualEffectiveAddress = effectiveAddress;
	effectiveAddress = convertAddFromVirtualToReal(effectiveAddress);
	}
	/* indirect addressing && index addressing */
	else {
	/*
	 * We will keep a copy of virtual address to use for jump instructions
	 */
	virtualEffectiveAddress = addressOfOperand;
	addressOfOperand = convertAddFromVirtualToReal(addressOfOperand);
	R2 = memory.Mem("READ", Integer.parseInt(addressOfOperand, 2), R2);
	indexToAdd = GPR[Integer.parseInt(IndexRegister, 2)];
	virtualEffectiveAddress = addAddresses(HEXBIN(R2), indexToAdd);
	effectiveAddress = addAddresses(HEXBIN(R2), indexToAdd);
	effectiveAddress = convertAddFromVirtualToReal(effectiveAddress);
	}
	}
	try {
	if (Integer.parseInt(effectiveAddress, 2) > 255
			|| Integer.parseInt(effectiveAddress, 2) < 0) {
	ReturnToScheduler = true;
	runningPCB.getFirst().currentOperation = "Error";
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.ADDRESS_OUT_OF_RANGE);
	runningPCB.getFirst().errorDescription = "Address Out Of Range";
	hasMore = false;
	writeOutput("Read");
	TimeLost += cumulativeTimeByJob;
	return false;
	}
	} catch (NumberFormatException e) {
	ReturnToScheduler = true;
	// if(runningPCB.getFirst().jobID==60)
	// {
	// writeOutput("Halt");
	// writeCurrentStateToPCB("Halt");
	// runningPCB.getFirst().errorDescription = "Normal Completion";
	// hasMore = false;
	// writeOutput("halt");
	// TimeLost+=cumulativeTimeByJob;
	//
	// }else{
	runningPCB.getFirst().currentOperation = "Error";
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.ARTHEMATIC_ERROR);
	runningPCB.getFirst().errorDescription = "Arithematic error.";
	hasMore = false;
	writeOutput("Arithematic");
	TimeLost += cumulativeTimeByJob;
	// }
	return false;
	}

	switch (OpCode) {
	case HLT :
		normalTermination++;
		returnValue = true;
		writeOutput("Halt");
		writeCurrentStateToPCB("Halt");
		runningPCB.getFirst().errorDescription = "Normal Completion";
		legitJobTime += runningPCB.getFirst().cumilativeTimeUsedByJob;
		if (lastJob == true) {
		ideleTime = runningPCB.getFirst().iotime;
		writeAggregateResults();
		}
		break;
	case LD :
		clock += OneClockCycleInTime;
		loadtracker += OneClockCycleInTime;
		executionTime += OneClockCycleInTime;
		cumulativeTimeByJob += OneClockCycleInTime;
		timeQuantum += OneClockCycleInTime;
		grossExecutionTime += OneClockCycleInTime;
		grossRuntime += OneClockCycleInTime;
		timeout += OneClockCycleInTime;
		GPR[Integer.parseInt(Accumulator, 2)] = HEXBIN(
				memory.Mem("READ", Integer.parseInt(effectiveAddress, 2),
						GPR[Integer.parseInt(Accumulator, 2)]));
		break;
	case ST :
		clock += OneClockCycleInTime;
		loadtracker += OneClockCycleInTime;
		executionTime += OneClockCycleInTime;
		cumulativeTimeByJob += OneClockCycleInTime;
		timeQuantum += OneClockCycleInTime;
		grossExecutionTime += OneClockCycleInTime;
		grossRuntime += OneClockCycleInTime;
		timeout += OneClockCycleInTime;
		GPR[Integer.parseInt(Accumulator, 2)] = HEXBIN(
				memory.Mem("WRIT", Integer.parseInt(effectiveAddress, 2),
						binhex(GPR[Integer.parseInt(Accumulator, 2)])));

		runningPCB.getFirst().pageFaultBits
				.set(Integer.parseInt(virtualEffectiveAddress, 2) / 16, "111");
		break;
	case AD :
		clock += OneClockCycleInTime;
		loadtracker += OneClockCycleInTime;
		executionTime += OneClockCycleInTime;
		cumulativeTimeByJob += OneClockCycleInTime;
		timeQuantum += OneClockCycleInTime;
		grossExecutionTime += OneClockCycleInTime;
		grossRuntime += OneClockCycleInTime;
		timeout += OneClockCycleInTime;
		R5 = HEXBIN(
				memory.Mem("READ", Integer.parseInt(effectiveAddress, 2), R5));
		GPR[Integer.parseInt(Accumulator, 2)] = ALU.Add(R5,
				GPR[Integer.parseInt(Accumulator, 2)]);
		if (GPR[Integer.parseInt(Accumulator, 2)].equals("OVERFLOW")) {
		ReturnToScheduler = true;
		runningPCB.getFirst().currentOperation = "Error";
		ErrorHandler.setJobID(runningPCB.getFirst().jobID);
		errorHandler.HandleError(ErrorHandler.INPUT_SIZE_OVERFLOW);
		runningPCB.getFirst().errorDescription = "Overflow";
		hasMore = false;
		writeOutput("Overflow");
		TimeLost += cumulativeTimeByJob;
		break;
		}
		break;
	case SB :
		clock += OneClockCycleInTime;
		loadtracker += OneClockCycleInTime;
		executionTime += OneClockCycleInTime;
		cumulativeTimeByJob += OneClockCycleInTime;
		timeQuantum += OneClockCycleInTime;
		grossExecutionTime += OneClockCycleInTime;
		grossRuntime += OneClockCycleInTime;
		timeout += OneClockCycleInTime;
		R5 = HEXBIN(
				memory.Mem("READ", Integer.parseInt(effectiveAddress, 2), R5));
		GPR[Integer.parseInt(Accumulator, 2)] = ALU
				.Sub(GPR[Integer.parseInt(Accumulator, 2)], R5);
		break;
	case MPY :
		executionTime += 2 * OneClockCycleInTime;
		clock += 2 * OneClockCycleInTime;
		loadtracker += 2 * OneClockCycleInTime;
		cumulativeTimeByJob += 2 * OneClockCycleInTime;
		timeQuantum += 2 * OneClockCycleInTime;
		grossExecutionTime += 2 * OneClockCycleInTime;
		grossRuntime += 2 * OneClockCycleInTime;
		timeout += 2 * OneClockCycleInTime;
		R5 = HEXBIN(
				memory.Mem("READ", Integer.parseInt(effectiveAddress, 2), R5));
		GPR[Integer.parseInt(Accumulator, 2)] = ALU.Mul(R5,
				GPR[Integer.parseInt(Accumulator, 2)]);
		break;
	case DIV :
		executionTime += 2 * OneClockCycleInTime;
		clock += 2 * OneClockCycleInTime;
		loadtracker += 2 * OneClockCycleInTime;
		cumulativeTimeByJob += 2 * OneClockCycleInTime;
		timeQuantum += 2 * OneClockCycleInTime;
		grossExecutionTime += 2 * OneClockCycleInTime;
		grossRuntime += 2 * OneClockCycleInTime;
		timeout += 2 * OneClockCycleInTime;
		R5 = HEXBIN(
				memory.Mem("READ", Integer.parseInt(effectiveAddress, 2), R5));
		GPR[Integer.parseInt(Accumulator, 2)] = ALU
				.Div(GPR[Integer.parseInt(Accumulator, 2)], R5);
		if (GPR[Integer.parseInt(Accumulator, 2)].equals("DIVIDEBYZERO")) {
		ReturnToScheduler = true;
		runningPCB.getFirst().currentOperation = "Error";
		ErrorHandler.setJobID(runningPCB.getFirst().jobID);
		errorHandler.HandleError(ErrorHandler.DIVIDE_BY_ZERO);
		runningPCB.getFirst().errorDescription = "Divide By Zero";
		hasMore = false;
		writeOutput("Divide by Zero");
		TimeLost += cumulativeTimeByJob;
		break;
		}
		break;
	case SHL :
		executionTime += 1;
		cumulativeTimeByJob += OneClockCycleInTime;
		timeQuantum += OneClockCycleInTime;
		grossRuntime += OneClockCycleInTime;
		grossRuntime += OneClockCycleInTime;
		grossExecutionTime += 2 * OneClockCycleInTime;
		clock += 1;
		loadtracker += OneClockCycleInTime;
		timeout += 1;
		GPR[Integer.parseInt(Accumulator, 2)] = ALU.ShiftLeft(
				GPR[Integer.parseInt(Accumulator, 2)], effectiveAddress);
		break;
	case SHR :
		executionTime += 1;
		clock += 1;
		loadtracker += OneClockCycleInTime;
		timeout += 1;
		cumulativeTimeByJob += OneClockCycleInTime;
		timeQuantum += OneClockCycleInTime;
		grossRuntime += OneClockCycleInTime;
		grossExecutionTime += 2 * OneClockCycleInTime;
		GPR[Integer.parseInt(Accumulator, 2)] = ALU.ShiftRight(
				GPR[Integer.parseInt(Accumulator, 2)], effectiveAddress);
		break;
	case BRM :
		executionTime += 1;
		cumulativeTimeByJob += OneClockCycleInTime;
		timeQuantum += OneClockCycleInTime;
		grossRuntime += OneClockCycleInTime;
		grossExecutionTime += 2 * OneClockCycleInTime;
		clock += 1;
		loadtracker += OneClockCycleInTime;
		timeout += 1;

		int valueToCompareBRM = Integer.parseInt(
				GPR[Integer.parseInt(Accumulator, 2)].substring(1, 32), 2);
		int signOfValueToCompareBRM = Integer.parseInt(
				GPR[Integer.parseInt(Accumulator, 2)].substring(0, 1));
		if (signOfValueToCompareBRM == 1) {
		valueToCompareBRM = valueToCompareBRM * -1;
		}

		if (valueToCompareBRM < 0) {
		VirtualPC = Integer.parseInt(virtualEffectiveAddress, 2) - 1;
		}
		break;
	case BRP :
		executionTime += 1;
		cumulativeTimeByJob += OneClockCycleInTime;
		timeQuantum += OneClockCycleInTime;
		grossRuntime += OneClockCycleInTime;
		grossExecutionTime += 2 * OneClockCycleInTime;
		clock += 1;
		loadtracker += OneClockCycleInTime;
		timeout += 1;
		int valueToCompareBRP = Integer.parseInt(
				GPR[Integer.parseInt(Accumulator, 2)].substring(1, 32), 2);
		int signOfValueToCompareBRP = Integer.parseInt(
				GPR[Integer.parseInt(Accumulator, 2)].substring(0, 1));
		if (signOfValueToCompareBRP == 1) {
		valueToCompareBRP = valueToCompareBRP * -1;
		}

		if (valueToCompareBRP > 0) {
		VirtualPC = Integer.parseInt(virtualEffectiveAddress, 2) - 1;
		}
		break;
	case BRZ :
		executionTime += 1;
		cumulativeTimeByJob += OneClockCycleInTime;
		timeQuantum += OneClockCycleInTime;
		grossRuntime += OneClockCycleInTime;
		grossExecutionTime += 2 * OneClockCycleInTime;
		clock += 1;
		loadtracker += OneClockCycleInTime;
		timeout += 1;
		int valueToCompareBRZ = Integer.parseInt(
				GPR[Integer.parseInt(Accumulator, 2)].substring(1, 32), 2);
		int signOfValueToCompareBRZ = Integer.parseInt(
				GPR[Integer.parseInt(Accumulator, 2)].substring(0, 1));
		if (signOfValueToCompareBRZ == 1) {
		valueToCompareBRZ = valueToCompareBRZ * -1;
		}

		if (valueToCompareBRZ == 0) {
		VirtualPC = Integer.parseInt(virtualEffectiveAddress, 2) - 1;
		}
		break;
	case BRL :
		executionTime += 2;
		clock += 2 * OneClockCycleInTime;
		loadtracker += OneClockCycleInTime;
		cumulativeTimeByJob += 2 * OneClockCycleInTime;
		timeQuantum += OneClockCycleInTime;
		grossRuntime += 2 * OneClockCycleInTime;
		grossExecutionTime += 2 * OneClockCycleInTime;
		timeout += 2;
		// VirtualPC = convertVirtualAddressToPhysical(VirtualPC);
		if (VirtualPC == -1) {
		ReturnToScheduler = true;
		runningPCB.getFirst().currentOperation = "Error";
		ErrorHandler.setJobID(runningPCB.getFirst().jobID);
		errorHandler.HandleError(ErrorHandler.ADDRESS_OUT_OF_RANGE);
		runningPCB.getFirst().errorDescription = "Address Out Of Range";
		hasMore = false;
		writeOutput("Read");
		runningPCB.removeFirst();
		TimeLost += cumulativeTimeByJob;
		break;
		}
		// Target = HEXBIN(memory.Mem("READ", PC, Target));
		Target = Integer.toBinaryString(VirtualPC);
		GPR[Integer.parseInt(Accumulator, 2)] = Target;
		VirtualPC = Integer.parseInt(virtualEffectiveAddress, 2) - 1;
		break;
	case AND :
		executionTime += 1;
		cumulativeTimeByJob += OneClockCycleInTime;
		timeQuantum += OneClockCycleInTime;
		grossRuntime += OneClockCycleInTime;
		grossExecutionTime += 2 * OneClockCycleInTime;
		clock += 1;
		loadtracker += OneClockCycleInTime;
		timeout += 1;
		R1 = HEXBIN(
				memory.Mem("READ", Integer.parseInt(effectiveAddress, 2), R1));
		GPR[Integer.parseInt(Accumulator, 2)] = ALU
				.And(GPR[Integer.parseInt(Accumulator, 2)], R1);
		break;
	case OR :
		executionTime += 1;
		cumulativeTimeByJob += OneClockCycleInTime;
		timeQuantum += OneClockCycleInTime;
		grossRuntime += OneClockCycleInTime;
		grossExecutionTime += 2 * OneClockCycleInTime;
		clock += 1;
		loadtracker += OneClockCycleInTime;
		timeout += 1;
		R1 = HEXBIN(
				memory.Mem("READ", Integer.parseInt(effectiveAddress, 2), R1));
		GPR[Integer.parseInt(Accumulator, 2)] = ALU
				.Or(GPR[Integer.parseInt(Accumulator, 2)], R1);
		break;
	case RD :
		executionTime += 2;
		ioTime += 8;
		/*
		 * IO handler is used because CPU should not directly do the I/O
		 * operations
		 */
		/* 8 clock cycles for IO and two for instruction */
		clock += 10 * OneClockCycleInTime;
		loadtracker += 10 * OneClockCycleInTime;
		cumulativeTimeByJob += 10 * OneClockCycleInTime;
		timeQuantum += 10 * OneClockCycleInTime;
		grossRuntime += 10 * OneClockCycleInTime;
		grossExecutionTime += 2 * OneClockCycleInTime;
		grossIOTime += 10;
		timeout = 0;
		VirtualPC++;
		runningPCB.getFirst().numberOfTurns = 1;
		runningPCB.getFirst().TimeQuantum = timeQuantum;
		writeCurrentStateToPCB("Read");
		if (runningPCB.getFirst().DataPagesOnDisk.length == 0) {
		ReturnToScheduler = true;
		runningPCB.getFirst().currentOperation = "Error";
		ErrorHandler.setJobID(runningPCB.getFirst().jobID);
		errorHandler.HandleError(ErrorHandler.MISSING_DATA);
		runningPCB.getFirst().errorDescription = "Missing Data";
		hasMore = false;
		writeOutput("Read");
		TimeLost += cumulativeTimeByJob;
		break;

		}
		dataPageToLoad = Integer.parseInt(runningPCB
				.getFirst().DataPagesOnDisk[runningPCB.getFirst().readPointer
						/ 4]);
		dataPageOffset = runningPCB.getFirst().readPointer % 4;
		runningPCB.getFirst().readPointer++;
		String data = "";
		for (int i = 0; i < 4; i++) {
		data += disk.disk[(dataPageToLoad * 16 + dataPageOffset * 4) + i];
		}
		for (int i = 0, j = 0; i < data.length(); i = i + 8, j++) {
		try {
		memory.Mem("WRIT",
				Integer.parseInt(
						convertAddFromVirtualToReal(virtualEffectiveAddress), 2)
				+ j, data.substring(i, i + 8));
		} catch (NumberFormatException e) {
		System.out.print("");
		}
		try {
		runningPCB.getFirst().pageFaultBits
				.set(Integer.parseInt(virtualEffectiveAddress, 2) / 16, "111");
		} catch (IndexOutOfBoundsException e) {
		System.out.print("");
		}
		}
		runningPCB.getFirst().resultOfReadOperation = data;
		break;
	case WR :
		executionTime += 2;
		ioTime += 8;
		/*
		 * IO handler is used because CPU should not directly do the I/O
		 * operations
		 */
		/* 8 clock cycles for IO and two for instruction */
		clock += 10 * OneClockCycleInTime;
		loadtracker += 10 * OneClockCycleInTime;
		cumulativeTimeByJob += 10 * OneClockCycleInTime;
		timeQuantum += 10 * OneClockCycleInTime;
		grossRuntime += 10 * OneClockCycleInTime;
		grossExecutionTime += 2 * OneClockCycleInTime;
		grossIOTime += 10;
		timeout = 0;
		VirtualPC++;
		runningPCB.getFirst().numberOfTurns = 1;
		runningPCB.getFirst().TimeQuantum = timeQuantum;
		writeCurrentStateToPCB("Write");
		if (runningPCB.getFirst().OutputPagesOnDisk.length == 0
				|| runningPCB.getFirst().writePointer >= runningPCB
						.getFirst().OutputPagesOnDisk.length * 16) {
		ReturnToScheduler = true;
		// System.out.println("OOPS");
		runningPCB.getFirst().currentOperation = "Error";
		ErrorHandler.setJobID(runningPCB.getFirst().jobID);
		errorHandler.HandleError(ErrorHandler.INSUFFICIENT_OUTPUT_SPACE);
		runningPCB.getFirst().errorDescription = "Insufficient Output Space";
		hasMore = false;
		writeOutput("Insufficient Output Space");
		TimeLost += cumulativeTimeByJob;
		break;
		}
		String ContentInAddress = "";
		for (int i = 0; i < 4; i++) {
		ContentInAddress += memory
				.Mem("READ",
						Integer.parseInt(convertAddFromVirtualToReal(
								virtualEffectiveAddress), 2) + i,
				ContentInAddress);
		}
		try {
		diskOutPageToWrite = Integer.parseInt(runningPCB
				.getFirst().OutputPagesOnDisk[runningPCB.getFirst().writePointer
						/ 4]);
		dataPageOffset = runningPCB.getFirst().writePointer % 4;
		} catch (ArrayIndexOutOfBoundsException e) {
		// System.out.println("OOPS");
		ReturnToScheduler = true;
		runningPCB.getFirst().currentOperation = "Error";
		ErrorHandler.setJobID(runningPCB.getFirst().jobID);
		errorHandler.HandleError(ErrorHandler.INSUFFICIENT_OUTPUT_SPACE);
		runningPCB.getFirst().errorDescription = "Insufficent Output space";
		hasMore = false;
		writeOutput("Insufficient Output Space");
		TimeLost += cumulativeTimeByJob;
		break;
		}

		/*
		 * one output line is 64 hex digits hence we append zeros if the content
		 * in address is less than 64
		 */
		try {
		for (int i = (diskOutPageToWrite * 16
				+ dataPageOffset * 4), j = 0; i < (diskOutPageToWrite * 16
						+ dataPageOffset * 4) + 4; i++, j += 8) {
		disk.disk[i] = ContentInAddress.substring(j, j + 8);
		}
		} catch (StringIndexOutOfBoundsException e) {
		System.out.print("");
		}
		runningPCB.getFirst().writePointer++;

		break;
	case DMP :
		clock += 1;
		loadtracker += OneClockCycleInTime;
		timeout += 1;
		cumulativeTimeByJob += 1;
		timeQuantum += OneClockCycleInTime;
		grossRuntime += OneClockCycleInTime;
		grossExecutionTime += OneClockCycleInTime;
		executionTime += 1;
		memory.Mem("DUMP", 0, nullString);
		break;
	}
	return returnValue;
	} catch (NumberFormatException e) {
	ReturnToScheduler = true;
	runningPCB.getFirst().currentOperation = "Error";
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.ARTHEMATIC_ERROR);
	runningPCB.getFirst().errorDescription = "Arithmetic Conversion Error";
	hasMore = false;
	writeOutput("Number conversion");
	TimeLost += cumulativeTimeByJob;
	return false;
	}
	}
	/*
	 * read 32 hex digits from the console and put it in memory locations EA to
	 * EA+3
	 */

	public void writeAggregateResults() {
	int temp = Loader.getHolesNo();
	int temp2 = Loader.getAvgHoleSize();
	int temp3 = Loader.getPercentage();
	try {
	FileWriter statusWrite2 = new FileWriter("progressFile.txt", true);
	statusWrite2.write("\n");
	statusWrite2.write("Report for Batch:");
	statusWrite2.write("\n");
	statusWrite2.write(String.format("%-53s %-15s", "Current Clock Value:(HEX)",
			Integer.toHexString(clock)));
	statusWrite2.write("\n");
	String tempo1 = String.format("%.2f",
			(Float.parseFloat(Integer.toString(grossIOTime)) / Loader.NOJITB));
	statusWrite2.write(String.format("%-53s %-15s",
			"Mean user job run time:(DEC)", tempo1));
	statusWrite2.write("\n");
	tempo1 = String.format("%.2f",
			(Float.parseFloat(Integer.toString(grossIOTime)) / Loader.NOJITB));
	statusWrite2.write(String.format("%-53s %-15s",
			"Mean user job I/O time:(DEC)", tempo1));
	statusWrite2.write("\n");
	tempo1 = String.format("%.2f",
			(Float.parseFloat(Integer.toString(grossExecutionTime))
					/ Loader.NOJITB));
	statusWrite2.write(String.format("%-53s %-15s",
			"Mean user job execution time:(DEC)", tempo1));
	statusWrite2.write("\n");
	tempo1 = String.format("%.2f",
			(Float.parseFloat(Integer.toString(clock)) / Loader.NOJITB));
	statusWrite2.write(String.format("%-53s %-15s",
			"Mean user job time in the System:(DEC)", tempo1));
	statusWrite2.write("\n");
	tempo1 = String.format("%.2f",
			(Float.parseFloat(Integer.toString(grossPFHtime)) / Loader.NOJITB));
	statusWrite2.write(String.format("%-53s %-15s",
			"Mean user job page fault handling time(DEC):", tempo1));
	statusWrite2.write("\n");
	statusWrite2.write(String.format("%-53s %-15s", "Total CPU idle time(HEX):",
			Integer.toHexString(ideleTime)));
	statusWrite2.write("\n");
	statusWrite2.write(String.format("%-53s %-15s",
			"Time lost due to abnormally terminated jobs(HEX):", 0));
	statusWrite2.write("\n");
	statusWrite2.write(String.format("%-53s %-15s",
			"Number of jobs that terminated normally(DEC):",
			normalTermination));
	statusWrite2.write("\n");
	statusWrite2.write(String.format("%-53s %-15s",
			"Number of jobs that terminated abnormally(DEC):",
			(Loader.NOJITB - normalTermination)));
	statusWrite2.write("\n");
	statusWrite2.write(String.format("%-53s %-15s",
			"Total Time lost due to Suspected Infinite jobs(HEX):",
			suspectedInputTimeout));
	statusWrite2.write("\n");
	statusWrite2.write(String.format("%-53s %-15s",
			"ID's of jobs considered Infinite(DEC):", suspectedInfiniteJobID));
	statusWrite2.write("\n");
	statusWrite2.write(
			String.format("%-53s %-15s", "Total Number of page faults(HEX):",
					Integer.toHexString((grossPFHtime / 5))));
	statusWrite2.write("\n");
	statusWrite2.write(String.format("%-53s %-15s",
			"percentage of Disk Occupied(DEC):", temp3));
	statusWrite2.write("\n");
	statusWrite2
			.write(String.format("%-53s %-15s", "Number of holes(DEC):", temp));
	statusWrite2.write("\n");
	statusWrite2.write(
			String.format("%-53s %-15s", "average size of holes(DEC):", temp2));
	statusWrite2.write("\n");
	statusWrite2.write(
			String.format("%-53s %-15s", "average size of holes(DEC):", temp2));
	statusWrite2.write("\n");
	statusWrite2.write(String.format("%-53s %-15s",
			"Maximum size of subqueue 1(DEC):", maxSubqueue1Size));
	statusWrite2.write("\n");
	statusWrite2.write(String.format("%-53s %-15s",
			"Maximum size of subqueue 2:(DEC)", maxSubqueue2Size));
	statusWrite2.write("\n");
	statusWrite2.write(String.format("%-53s %-15s",
			"Maximum size of subqueue 3:(DEC)", maxSubqueue3Size));
	statusWrite2.write("\n");
	statusWrite2.write(String.format("%-53s %-15s",
			"Maximum size of subqueue 4:(DEC)", maxSubqueue4Size));
	statusWrite2.write("\n");
	String tempo = String.format("%.2f", (cumulativeSubQueu1size / queueCount));
	statusWrite2.write(String.format("%-53s %-15s",
			"Average size of subqueue 1(DEC)", tempo));
	statusWrite2.write("\n");
	tempo = String.format("%.2f", (cumulativeSubQueu2size / queueCount));
	statusWrite2.write(String.format("%-53s %-15s",
			"Average size of subqueue 2(DEC)", tempo));
	statusWrite2.write("\n");
	tempo = String.format("%.2f", (cumulativeSubQueu3size / queueCount));
	statusWrite2.write(String.format("%-53s %-15s",
			"Average size of subqueue 3(DEC)", tempo));
	statusWrite2.write("\n");
	tempo = String.format("%.2f", (cumulativeSubQueu4size / queueCount));
	statusWrite2.write(String.format("%-53s %-15s",
			"Average size of subqueue 4(DEC)", tempo));
	statusWrite2.write("\n");
	statusWrite2.close();
	} catch (IOException e) {
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.FILE_NOT_FOUND);
	hasMore = false;
	ReturnToScheduler = true;
	}

	}

	private String convertAddFromVirtualToReal(String addr) {
	try {
	addr = binhex(addr);
	String temp = addr.toString();
	int PageTableIndexDec = Integer.parseInt(temp.substring(0, 1), 16);
	String pageTableOffset = temp.substring(1, 2);
	/* condition will be true if the page is not in memory */
	if (runningPCB.getFirst().pageFaultBits.get(PageTableIndexDec)
			.substring(0, 1).equals("0")) {
	String pageNumberOnMemtoReplace = "";
	String pageNumberOnDisktoReplace;
	String pageNumberOnDisktoBring;
	int pageIndexToReplace = -1;
	boolean isPageClean = true;
	cumulativeTimeByJob += 5;
	timeQuantum += 5;
	grossRuntime += 5;
	pageFaultHandlingTime += 5;
	grossPFHtime += 5;
	/* Victim selection stage 1 not used and clean */
	for (int i = 0; i < runningPCB.getFirst().ProgramPagesOnMem.length; i++) {
	if (runningPCB.getFirst().pageFaultBits.get(i).substring(0, 1).equals("1")
			&& runningPCB.getFirst().pageFaultBits.get(i).substring(1, 2)
					.equals("0")
			&& runningPCB.getFirst().pageFaultBits.get(i).substring(2, 3)
					.equals("0")) {
	pageIndexToReplace = i;
	break;
	}
	}
	/* Victim selection Stage 2 used but still clean */
	if (pageIndexToReplace == -1) {
	for (int i = 0; i < runningPCB.getFirst().ProgramPagesOnMem.length; i++) {
	if (runningPCB.getFirst().pageFaultBits.get(i).substring(0, 1).equals("1")
			&& runningPCB.getFirst().pageFaultBits.get(i).substring(1, 2)
					.equals("1")
			&& runningPCB.getFirst().pageFaultBits.get(i).substring(2, 3)
					.equals("0")) {
	pageIndexToReplace = i;
	break;
	}
	}
	}
	/* Victim selection Stage 3 used and dirty */
	if (pageIndexToReplace == -1) {
	for (int i = 0; i < runningPCB.getFirst().ProgramPagesOnMem.length; i++) {
	if (runningPCB.getFirst().pageFaultBits.get(i).substring(0, 1).equals("1")
			&& runningPCB.getFirst().pageFaultBits.get(i).substring(1, 2)
					.equals("1")
			&& runningPCB.getFirst().pageFaultBits.get(i).substring(2, 3)
					.equals("1")) {
	pageIndexToReplace = i;
	isPageClean = false;
	break;
	}
	}
	}
	if (pageIndexToReplace == -1) {
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.OUT_OF_BOUNDS);
	hasMore = false;
	ReturnToScheduler = true;
	return "";
	} else {
	/* Page is dirty copy it to disk */
	if (isPageClean == false) {
	String contentInMemPage = "";
	pageNumberOnMemtoReplace = runningPCB
			.getFirst().ProgramPagesOnMem[pageIndexToReplace];
	pageNumberOnDisktoReplace = runningPCB
			.getFirst().ProgramPagesOnDisk[pageIndexToReplace];
	for (int i = 0; i < 16; i++) {
	contentInMemPage += memory.Mem("READ",
			Integer.parseInt(pageNumberOnMemtoReplace) * 16 + i, "");
	}
	for (int i = (Integer.parseInt(pageNumberOnDisktoReplace)
			* 16), j = 0; i < (Integer.parseInt(pageNumberOnDisktoReplace) * 16)
					+ 16; i++, j = j + 8) {
	disk.disk[i] = contentInMemPage.substring(j, j + 8);
	}
	runningPCB.getFirst().pageFaultBits.set(pageIndexToReplace,
			runningPCB.getFirst().pageFaultBits.get(pageIndexToReplace)
					.substring(0, 2) + "0");
	}
	/* replace the page with the page from disk */
	String contentInDisk = "";
	pageNumberOnDisktoBring = runningPCB
			.getFirst().ProgramPagesOnDisk[PageTableIndexDec];
	for (int i = Integer.parseInt(pageNumberOnDisktoBring)
			* 16; i < (Integer.parseInt(pageNumberOnDisktoBring) * 16)
					+ 16; i++) {
	contentInDisk += disk.disk[i];
	}
	pageNumberOnMemtoReplace = runningPCB
			.getFirst().ProgramPagesOnMem[pageIndexToReplace];
	for (int i = Integer.parseInt(pageNumberOnMemtoReplace)
			* 16, j = 0; i < (Integer.parseInt(pageNumberOnMemtoReplace) * 16)
					+ 16; i++, j = j + 8) {
	memory.Mem("WRIT", i, contentInDisk.substring(j, j + 8));
	}
	/* update the bits for the page replaced and fetched */
	runningPCB.getFirst().pageFaultBits.set(pageIndexToReplace, "000");
	runningPCB.getFirst().pageFaultBits.set(PageTableIndexDec, "100");
	runningPCB.getFirst().ProgramPagesOnMem[pageIndexToReplace] = null;
	runningPCB
			.getFirst().ProgramPagesOnMem[PageTableIndexDec] = pageNumberOnMemtoReplace;
	}
	}
	String pageNumberToLook = Integer.toHexString(Integer.parseInt(
			runningPCB.getFirst().ProgramPagesOnMem[PageTableIndexDec]));
	runningPCB.getFirst().pageFaultBits.set(PageTableIndexDec,
			"11" + runningPCB.getFirst().pageFaultBits.get(PageTableIndexDec)
					.substring(2, 3));
	String address = HEXBIN(pageNumberToLook + pageTableOffset);
	return address;
	} catch (Exception e) {
	return "";
	}
	}

	private void writeOutput(String Reason) {
	try {
	FileWriter fw2 = new FileWriter("progressFile.txt", true);
	fw2.write("\n");
	fw2.write(
			String.format("%-40s %-15s", "Completed Execution for jobID(DEC):",
					runningPCB.getFirst().jobID));
	fw2.write("\n");
	fw2.write(String.format("%-40s %-15s",
			"Current value of the clock(Decimal):", clock));
	fw2.write("\n");
	fw2.write(String.format("%-40s %-15s", "clock at load time(hex):", Integer
			.toHexString(runningPCB.getFirst().timeJobEnteredTheSystem)));
	fw2.write("\n");
	fw2.write(String.format("%-40s %-15s", "Clock at termination time(hex):",
			Integer.toHexString(clock)));
	fw2.write("\n");
	fw2.write(String.format("%-40s %-15s", "Number of chances at the CPU:",
			runningPCB.getFirst().numberOfChancesCPU));
	fw2.write("\n");
	fw2.write("job Output(Hex):");
	String temp = "";
	if (Reason.equals("Halt")) {
	for (int j = 0; j < runningPCB.getFirst().OutputPagesOnDisk.length; j++) {
	for (int i = 0; i < 16; i++) {
	if (disk.disk[Integer.parseInt(runningPCB.getFirst().OutputPagesOnDisk[j])
			* 16 + i] != null) {
	if (i % 4 == 0) {
	fw2.write("\n");
	}
	// fw2.write(disk.disk[Integer.parseInt(runningPCB.getFirst().OutputPagesOnDisk[j])
	// * 16 + i]);
	temp += disk.disk[Integer
			.parseInt(runningPCB.getFirst().OutputPagesOnDisk[j]) * 16 + i];
	if (temp.length() == 32) {
	fw2.write(String.format("%10s %10s %10s %10s", temp.substring(0, 8),
			temp.substring(8, 16), temp.substring(16, 24),
			temp.substring(24, 32)));
	temp = "";
	}
	}
	}
	}
	}
	String temp2 = "";
	if (memoryDumpArray.size() > 0) {
	fw2.write("\n");
	fw2.write("Memory Dump:");
	fw2.write("\n");
	for (int i = 0; i < memoryDumpArray.size(); i++) {
	// fw2.write(memoryDumpArray.get(i));
	temp2 += memoryDumpArray.get(i);
	if (temp2.length() == 32) {
	if (temp2.substring(0, 8).equals("nullnull")) {
	fw2.write(String.format("%10s %10s %10s %10s", "null", "null", "null",
			"null"));
	fw2.write("\n");
	temp2 = "";
	} else {
	fw2.write(String.format("%10s %10s %10s %10s", temp2.substring(0, 8),
			temp2.substring(8, 16), temp2.substring(16, 24),
			temp2.substring(24, 32)));
	fw2.write("\n");
	temp2 = "";
	}
	}
	}
	}
	fw2.write("\n");
	if (Reason.equals("Halt")) {
	fw2.write(String.format("%-40s %-15s", "Nature of termination:", "Normal"));
	} else if (Reason.equals("Read")) {
	fw2.write(String.format("%-40s %-15s", "Nature of termination:",
			"Read Error"));
	} else if (Reason.equals("Wead")) {
	fw2.write(String.format("%-40s %-15s", "Nature of termination:",
			"Write Error"));
	} else if (Reason.equals("Divide by Zero")) {
	fw2.write(String.format("%-40s %-15s", "Nature of termination:",
			"Arithematic Error"));
	} else if (Reason.equals("Number conversion")) {
	fw2.write(String.format("%-40s %-15s", "Nature of termination:",
			"Problem with number conversion"));
	} else if (Reason.equals("Illegal Character")) {
	fw2.write(String.format("%-40s %-15s", "Nature of termination:",
			"Illegal character"));
	} else if (Reason.equals("System Error")) {
	fw2.write(String.format("%-40s %-15s", "Nature of termination:",
			"System Error"));
	} else if (Reason.equals("Overflow")) {
	fw2.write(
			String.format("%-40s %-15s", "Nature of termination:", "Overflow"));
	} else if (Reason.equals("IO")) {
	fw2.write(String.format("%-40s %-15s", "Nature of termination:",
			"IO Exception"));
	} else if (Reason.equals("Invalid Pointer")) {
	fw2.write(String.format("%-40s %-15s", "Nature of termination:",
			"Invalid Pointer"));
	} else if (Reason.equals("Infinite")) {
	fw2.write(String.format("%-40s %-15s", "Nature of termination:",
			"Suspected Infinite Job"));
	} else if (Reason.equals("Arithematic")) {
	fw2.write(String.format("%-40s %-15s", "Nature of termination:",
			"Arithematic Error"));
	} else if (Reason.equals("Insufficient Output Space")) {
	fw2.write(String.format("%-40s %-15s", "Nature of termination:",
			"Insufficient Output space"));
	} else if (Reason.equals("Invalid Opcode")) {
	fw2.write(String.format("%-40s %-15s", "Nature of termination:",
			"Invalid Opcode"));
	}
	fw2.write("\n");
	fw2.write(String.format("%-40s %-15s", "Runtime(Decimal):",
			runningPCB.getFirst().cumilativeTimeUsedByJob));
	fw2.write("\n");
	fw2.write(String.format("%-40s %-15s", "TurnAroundTime(Decimal):",
			(clock - runningPCB.getFirst().timeJobEnteredTheSystem)));
	fw2.write("\n");
	fw2.write(String.format("%-40s %-15s", "Execution Time(Decimal):",
			runningPCB.getFirst().cumilativeTimeUsedByJob));
	fw2.write("\n");
	fw2.write(String.format("%-40s %-15s", "Page Fault Handling Time(Decimal):",
			runningPCB.getFirst().pageFaultHandlingTime));
	fw2.write("\n");
	fw2.write("------------------------------------------------------");
	fw2.write("\n");
	fw2.close();
	} catch (IOException e) {
	System.out.print("");
	}

	}

	public String addBinary(String a, String b) {
	if (a == null || a.length() == 0)
		return b;
	if (b == null || b.length() == 0)
		return a;

	int pa = a.length() - 1;
	int pb = b.length() - 1;

	int flag = 0;
	StringBuilder sb = new StringBuilder();
	while (pa >= 0 || pb >= 0) {
	int va = 0;
	int vb = 0;

	if (pa >= 0) {
	va = a.charAt(pa) == '0' ? 0 : 1;
	pa--;
	}
	if (pb >= 0) {
	vb = b.charAt(pb) == '0' ? 0 : 1;
	pb--;
	}

	int sum = va + vb + flag;
	if (sum >= 2) {
	sb.append(String.valueOf(sum - 2));
	flag = 1;
	} else {
	flag = 0;
	sb.append(String.valueOf(sum));
	}
	}

	if (flag == 1) {
	sb.append("1");
	}

	String reversed = sb.reverse().toString();
	return reversed;
	}

	private void restoreStateOfProcess() {
	VirtualPC = runningPCB.getFirst().PC;
	instructionRegister = runningPCB.getFirst().registers[0];
	PC = Integer.parseInt(runningPCB.getFirst().registers[1]);
	Accumulator = runningPCB.getFirst().registers[2];
	IndexRegister = runningPCB.getFirst().registers[3];
	R1 = runningPCB.getFirst().registers[4];
	R2 = runningPCB.getFirst().registers[5];
	R3 = runningPCB.getFirst().registers[6];
	R4 = runningPCB.getFirst().registers[7];
	R5 = runningPCB.getFirst().registers[8];
	R6 = runningPCB.getFirst().registers[9];
	R7 = runningPCB.getFirst().registers[10];
	R8 = runningPCB.getFirst().registers[11];
	R9 = runningPCB.getFirst().registers[12];
	R10 = runningPCB.getFirst().registers[13];
	R11 = runningPCB.getFirst().registers[14];
	Target = runningPCB.getFirst().registers[15];
	ioTime = runningPCB.getFirst().iotime;
	addressOfOperand = runningPCB.getFirst().AddressRFWT;
	for (int i = 0; i < runningPCB.getFirst().GeneralPurpouse.length; i++) {
	GPR[i] = runningPCB.getFirst().GeneralPurpouse[i];
	}
	}

	private void writeCurrentStateToPCB(String operation) {
	runningPCB.getFirst().PC = VirtualPC;
	runningPCB.getFirst().registers = new String[16];
	runningPCB.getFirst().GeneralPurpouse = new String[GPR.length];
	String[] temp = new String[16];
	temp[0] = instructionRegister;
	temp[1] = Integer.toString(VirtualPC);
	temp[2] = Accumulator;
	temp[3] = IndexRegister;
	temp[4] = R1;
	temp[5] = R2;
	temp[6] = R3;
	temp[7] = R4;
	temp[8] = R5;
	temp[9] = R6;
	temp[10] = R7;
	temp[11] = R8;
	temp[12] = R9;
	temp[13] = R10;
	temp[14] = R11;
	temp[15] = Target;
	runningPCB.getFirst().registers = temp;
	runningPCB.getFirst().cumilativeTimeUsedByJob = cumulativeTimeByJob;
	// runningPCB.getFirst().currentSubqueueLevel = currentLevel;
	runningPCB.getFirst().pageFaultHandlingTime = pageFaultHandlingTime;
	runningPCB.getFirst().timeJobEnteredTheSystem = timeJobEnteredTheSystem;
	runningPCB.getFirst().AddressRFWT = addressOfOperand;
	runningPCB.getFirst().TimeQuantum += timeQuantum;
	runningPCB.getFirst().iotime = ioTime;
	runningPCB.getFirst().numberOfTurns = numberOfTurns;
	runningPCB.getFirst().currentSubqueueLevel = currentLevel;
	runningPCB.getFirst().numberOfChancesCPU = chancesAtCPU;
	for (int i = 0; i < GPR.length; i++) {
	runningPCB.getFirst().GeneralPurpouse[i] = GPR[i];
	}
	if (operation.equals("Read")) {
	runningPCB.getFirst().currentOperation = "Read";
	hasMore = true;
	} else if (operation.equals("Write")) {
	runningPCB.getFirst().currentOperation = "Write";
	hasMore = true;

	} else if (operation.equals("TimeOut")) {
	runningPCB.getFirst().currentOperation = "TimeOut";
	hasMore = true;
	} else if (operation.equals("Halt")) {
	runningPCB.getFirst().currentOperation = "Halt";
	hasMore = false;
	}
	VirtualPC = 0;
	ioTime = 0;
	R1 = "";
	R2 = "";
	R3 = "";
	R4 = "";
	R5 = "";
	R6 = "";
	R7 = "";
	R8 = "";
	R9 = "";
	R10 = "";
	R11 = "";
	PC = 0;
	Accumulator = "";
	instructionRegister = "";
	IndexRegister = "";
	indexToAdd = "";
	addressOfOperand = "";
	currentLevel = 0;
	for (int i = 0; i < GPR.length; i++) {
	GPR[i] = null;
	}
	ReturnToScheduler = true;
	}

	public String addAddresses(String addr1, String addr2) {
	/*
	 * Maintaining the consistency of addresses as 32 bits by padding required
	 * number of zeros in front of most significant bit
	 */
	int length1 = addr1.length();
	if (length1 < 32) {
	for (int i = 0; i < (32 - length1); i++) {
	addr1 = "0" + addr1;
	}
	}
	int length2 = addr2.length();
	if (length2 < 32) {
	for (int i = 0; i < (32 - length2); i++) {
	addr2 = "0" + addr2;
	}
	}

	String sum = addBinary(addr1, addr2);

	return sum;
	}

	public String fetchOpcode(String code) {
	/*
	 * converting the binary to hex values to decode the right opcode operation
	 */
	code = binhex(code);
	return code;
	}

	public opCode decodeOpcode(String code) {
	opCode OpCode = null;
	if (code.toLowerCase().equals("00")) {
	return opCode.HLT;
	}
	if (code.toLowerCase().equals("01")) {
	return opCode.LD;
	}
	if (code.toLowerCase().equals("02")) {
	return opCode.ST;
	}
	if (code.toLowerCase().equals("03")) {
	return opCode.AD;
	}
	if (code.toLowerCase().equals("04")) {
	return opCode.SB;
	}
	if (code.toLowerCase().equals("05")) {
	return opCode.MPY;
	}
	if (code.toLowerCase().equals("06")) {
	return opCode.DIV;
	}
	if (code.toLowerCase().equals("07")) {
	return opCode.SHL;
	}
	if (code.toLowerCase().equals("08")) {
	return opCode.SHR;
	}
	if (code.toLowerCase().equals("09")) {
	return opCode.BRM;
	}
	if (code.toLowerCase().equals("0a")) {
	return opCode.BRP;
	}
	if (code.toLowerCase().equals("0b")) {
	return opCode.BRZ;
	}
	if (code.toLowerCase().equals("0c")) {
	return opCode.BRL;
	}
	if (code.toLowerCase().equals("0d")) {
	return opCode.AND;
	}
	if (code.toLowerCase().equals("0e")) {
	return opCode.OR;
	}
	if (code.toLowerCase().equals("0f")) {
	return opCode.RD;
	}
	if (code.toLowerCase().equals("10")) {
	return opCode.WR;
	}
	if (code.toLowerCase().equals("11")) {
	return opCode.DMP;
	}

	return OpCode;

	}

	private String binhex(String binaryValue) {
	try {
	if (!binaryValue.equals("")) {
	String value = new BigInteger(binaryValue, 2).toString(16);
	return String.format("%2s", value).replace(" ", "0");
	} else {
	return null;
	}
	} catch (Exception e) {
	runningPCB.getFirst().currentOperation = "Error";
	ErrorHandler.setJobID(runningPCB.getFirst().jobID);
	errorHandler.HandleError(ErrorHandler.ARTHEMATIC_ERROR);
	runningPCB.getFirst().errorDescription = "Arithematic Error";
	hasMore = false;
	ReturnToScheduler = true;
	writeOutput("Arithematic");
	return null;
	}
	}

	/* This method will take value in hexadecimal and return a binary value */
	public String HEXBIN(String hexValue) {
	String value = new BigInteger(hexValue, 16).toString(2);
	return String.format("%32s", value).replace(" ", "0");
	}
}

/*
 * Name: Gautham Brahma Ponnaganti. Course No.: CS5323 Assignment title:OS
 * Project PHASE I Date:2-22-2016 Global Variables: -errorHandler: This is used
 * to call the error handler because it can be used in case an error occurs
 * 
 *
 * Brief description: This is used for arithmetic operations which is like an
 * ALU in a computer system.
 */
class ArthematicOperations {
	ErrorHandler errorHandler = new ErrorHandler();

	public String Add(String Operand1, String Operand2) {
	try {
	int signOfOperand1 = Integer.parseInt(Operand1.substring(0, 1));
	int signOfOperand2 = Integer.parseInt(Operand2.substring(0, 1));
	int signOfResult = 0;
	Operand1 = Operand1.substring(1, Operand1.length());
	Operand2 = Operand2.substring(1, Operand2.length());
	int x = Integer.parseInt(Operand1, 2);
	int y = Integer.parseInt(Operand2, 2);
	if (signOfOperand1 == 1) {
	x = x * (-1);
	}
	if (signOfOperand2 == 1) {
	y = y * (-1);
	}
	int result = x + y;
	if (result < 0) {
	signOfResult = 1;
	result = result * -1;
	}
	String z = Integer.toBinaryString(result);
	int length = z.length();
	if (length < 31) {
	for (int i = 0; i < (31 - length); i++) {
	z = "0" + z;
	}
	}
	if (signOfResult == 0) {
	z = "0" + z;
	} else if (signOfResult == 1) {
	z = "1" + z;
	}
	return z;
	} catch (ArithmeticException e) {
	return "OVERFLOW";
	}
	}

	public String And(String Operand1, String Operand2) {
	int result = Integer.parseInt(Operand1, 2) & Integer.parseInt(Operand2, 2);
	return Integer.toBinaryString(result);
	}

	public String Or(String Operand1, String Operand2) {
	int result = Integer.parseInt(Operand1, 2) | Integer.parseInt(Operand2, 2);
	return Integer.toBinaryString(result);
	}

	public String Sub(String Operand1, String Operand2) {
	String z;
	int signOfOperand1 = Integer.parseInt(Operand1.substring(0, 1));
	int signOfOperand2 = Integer.parseInt(Operand2.substring(0, 1));
	int signOfResult = 0;
	Operand1 = Operand1.substring(1, Operand1.length());
	Operand2 = Operand2.substring(1, Operand2.length());
	int x = Integer.parseInt(Operand1, 2);
	int y = Integer.parseInt(Operand2, 2);
	if (signOfOperand1 == 1) {
	x = x * (-1);
	}
	if (signOfOperand2 == 1) {
	y = y * (-1);
	}
	int result = x - y;
	if (result < 0) {
	signOfResult = 1;
	result = result * -1;
	}
	z = Integer.toBinaryString(result);
	int length = z.length();
	if (length < 31) {
	for (int i = 0; i < (31 - length); i++) {
	z = "0" + z;
	}
	}
	if (signOfResult == 0) {
	z = "0" + z;
	} else if (signOfResult == 1) {
	z = "1" + z;
	}

	return z;
	}

	public String Mul(String Operand1, String Operand2) {
	int signOfOperand1 = Integer.parseInt(Operand1.substring(0, 1));
	int signOfOperand2 = Integer.parseInt(Operand2.substring(0, 1));
	int signOfResult = 0;
	Operand1 = Operand1.substring(1, Operand1.length());
	Operand2 = Operand2.substring(1, Operand2.length());
	int x = Integer.parseInt(Operand1, 2);
	int y = Integer.parseInt(Operand2, 2);
	if (signOfOperand1 == 1) {
	x = x * (-1);
	}
	if (signOfOperand2 == 1) {
	y = y * (-1);
	}
	int result = x * y;
	if (result < 0) {
	signOfResult = 1;
	result = result * -1;
	}
	String z = Integer.toBinaryString(result);
	int length = z.length();
	if (length < 31) {
	for (int i = 0; i < (31 - length); i++) {
	z = "0" + z;
	}
	}
	if (signOfResult == 0) {
	z = "0" + z;
	} else if (signOfResult == 1) {
	z = "1" + z;
	}

	return z;
	}

	public String Div(String Operand1, String Operand2) {
	int signOfOperand1 = Integer.parseInt(Operand1.substring(0, 1));
	int signOfOperand2 = Integer.parseInt(Operand2.substring(0, 1));
	int signOfResult = 0;
	Operand1 = Operand1.substring(1, Operand1.length());
	Operand2 = Operand2.substring(1, Operand2.length());
	int x = Integer.parseInt(Operand1, 2);
	int y = Integer.parseInt(Operand2, 2);
	if (y == 0) {
	return "DIVIDEBYZERO";
	}
	if (signOfOperand1 == 1) {
	x = x * (-1);
	}
	if (signOfOperand2 == 1) {
	y = y * (-1);
	}
	int result = x / y;
	if (result < 0) {
	signOfResult = 1;
	result = result * -1;
	}
	String z = Integer.toBinaryString(result);
	int length = z.length();
	if (length < 31) {
	for (int i = 0; i < (31 - length); i++) {
	z = "0" + z;
	}
	}
	if (signOfResult == 0) {
	z = "0" + z;
	} else if (signOfResult == 1) {
	z = "1" + z;
	}

	return z;
	}

	public String ShiftLeft(String Operand1, String ShiftAmount) {
	long operand = Long.parseLong(Operand1, 2);
	int sftAmt = Integer.parseInt(ShiftAmount, 2);
	long iResult = operand << sftAmt;
	iResult = iResult % 1073741824;
	String result = Long.toBinaryString(iResult);
	int lnth = result.length();
	if (lnth < 31) {
	for (int i = 0; i < (31 - lnth); i++) {
	result = "0" + result;
	}
	}
	return result;
	}

	public String ShiftRight(String Operand1, String ShiftAmount) {
	long operand = Long.parseLong(Operand1, 2);
	int sftAmt = Integer.parseInt(ShiftAmount, 2);
	long iResult = operand >>> sftAmt;
	iResult = iResult % 1073741824;
	String result = Long.toBinaryString(iResult);
	int lnth = result.length();
	if (lnth < 31) {
	for (int i = 0; i < (31 - lnth); i++) {
	result = "0" + result;
	}
	}
	return result;
	}

}

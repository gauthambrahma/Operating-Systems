
/*Global Variables:
 * -Process/running/blocked/terminated Queue: These are list of PCB's necessary for maintaining various states
 * of a process in multiprogramming.
 * -PagesForProgram:Number of pages a program needs.
 * -diskIndexToDeleteFrom, memIndexToDeleteFrom, processIndexToDeleteFrom:during the termination of a process we use
 * these variables to deallocate the space allocated for them.
 * -MemAvailability: used to keep track of the memory availability.
 * 
 * Brief Description:Scheduler is responsible for maintaining ready and blocked queue and it is also responsible for 
 * creating PCB for jobs. It is responsible to handle whenever control branched back from the CPU by scheduling
 * new processes or by terminating completed or error processes.
 * 
 * Changes in PhaseIII: Changed the scheduling algorithm from round robin to multilevel feedback queues
 * */

import java.util.ArrayList;
import java.util.LinkedList;

public class Scheduler {
	/** Task time limit for detecting infinite loop. */
	public static final int TIME_LIMIT = 1000;

	/**
	 * Current running process. This value must not be null when there is a job
	 * running in CPU.
	 */

	PCB pcb;
	Disk diskReference;
	Memory memory = new Memory();
	CPU cpu = new CPU();
	LinkedList<PCB> processQueue = new LinkedList<PCB>();
	LinkedList<PCB> readyQueue = new LinkedList<PCB>();
	LinkedList<PCB> runningQueue = new LinkedList<PCB>();
	LinkedList<PCB> BlockedQueue = new LinkedList<PCB>();
	LinkedList<PCB> TerminatedQueue = new LinkedList<PCB>();
	LinkedList<PCB> subQueue1 = new LinkedList<PCB>();
	LinkedList<PCB> subQueue2 = new LinkedList<PCB>();
	LinkedList<PCB> subQueue3 = new LinkedList<PCB>();
	LinkedList<PCB> subQueue4 = new LinkedList<PCB>();
	String[] PagesForProgram;
	ArrayList<String> _ProgramPagesOnMem = new ArrayList<String>();
	ArrayList<String> _DataPagesOnMem = new ArrayList<String>();
	ArrayList<String> _OutputPagesOnMem = new ArrayList<String>();
	public static ArrayList<Integer> contentOfSubqueue1 = new ArrayList<Integer>();
	public static ArrayList<Integer> contentOfSubqueue2 = new ArrayList<Integer>();
	public static ArrayList<Integer> contentOfSubqueue3 = new ArrayList<Integer>();
	public static ArrayList<Integer> contentOfSubqueue4 = new ArrayList<Integer>();
	int terminatedJobID = 0;
	/* By default all of the memory is available */
	String[] MemAvailability = {"true", "true", "true", "true", "true", "true",
			"true", "true", "true", "true", "true", "true", "true", "true",
			"true", "true"};
	int emptyPagesOnMem = 16;
	int PageIndex = 0;
	int diskIndexToDeleteFrom, memIndexToDeleteFrom, processIndexToDeleteFrom;
	String[] blockForMem = new String[16];
	int processQueSize = 0;
	int recursionCount = 0;
	boolean isRecursiveCall = false;
	boolean hasMore = true;
	int pagesNeeded = 0;
	boolean lastFewProcess = false;
	ErrorHandler err = new ErrorHandler();
	public static int allowedQuantum = 35;
	public static int allowedNumberOfTurns = 3;
	public static int subqueue1Size = 0;
	public static int subqueue2Size = 0;
	public static int subqueue3Size = 0;
	public static int subqueue4Size = 0;
	public static int matrixMetric = 0;

	public void manageDiskReference(Disk _disk, LinkedList<PCB> _listOfPCB) {
	diskReference = _disk;
	processQueue = _listOfPCB;
	loadToMemory();
	}

	private void loadToMemory() {
	for (int i = processQueSize; i < processQueue.size(); i++) {
	/* check the number of pages the program takes */
	pagesNeeded = processQueue.get(i).ProgramPagesOnDisk.length;
	if (pagesNeeded == 1) {
	pagesNeeded = 1;
	} else if ((pagesNeeded / 3) <= 2) {
	pagesNeeded = 2;
	} else {
	pagesNeeded = pagesNeeded / 3;
	}
	PagesForProgram = new String[pagesNeeded];
	try {
	for (int l = 0; l < pagesNeeded; l++) {
	PagesForProgram[l] = processQueue.get(i).ProgramPagesOnDisk[l];
	}
	} catch (ArrayIndexOutOfBoundsException e) {
	ErrorHandler.setJobID(processQueue.get(i).jobID);
	err.HandleError(ErrorHandler.OUT_OF_BOUNDS);
	processQueue.remove(i);
	break;
	}
	try {
	/* check if the program fits on the disk */
	if (PagesForProgram.length <= emptyPagesOnMem) {
	/*
	 * write the contents of the disk into the memory locations available
	 */
	writeDiskContentToMem(PagesForProgram,
			processQueue.get(i).ProgramPagesOnDisk.length,
			processQueue.get(i).jobID);
	processQueue.get(i).ProgramPagesOnMem = new String[_ProgramPagesOnMem
			.size()];
	_ProgramPagesOnMem.toArray(processQueue.get(i).ProgramPagesOnMem);
	_ProgramPagesOnMem.clear();
	processQueue.get(i).currentOperation = "NEW";
	/* Setting bit 0 as 1 indicating the page is in memory */
	try {
	for (int k = 0; k < PagesForProgram.length; k++) {
	processQueue.get(i).pageFaultBits.set(k,
			"1" + processQueue.get(i).pageFaultBits.get(k).substring(1, 3));
	}
	} catch (IndexOutOfBoundsException e) {
	ErrorHandler.setJobID(processQueue.get(i).jobID);
	err.HandleError(ErrorHandler.OUT_OF_BOUNDS);
	processQueue.remove(i);
	break;
	}
	processQueue.get(i).currentSubqueueLevel = 1;
	subQueue1.add(processQueue.get(i));
	}
	if (PagesForProgram.length > emptyPagesOnMem
			|| i == processQueue.size() - 1) {
	processQueSize = 0;
	if (subQueue1.size() > 0) {
	processQueSize = subQueue1.size();
	}
	if (subQueue2.size() > 0) {
	processQueSize += subQueue2.size();
	}
	if (subQueue3.size() > 0) {
	processQueSize += subQueue3.size();
	}
	if (subQueue4.size() > 0) {
	processQueSize += subQueue4.size();
	}

	cpu.setMemoryObject(memory);
	scheduleReadyProcesses();

	/*
	 * remove the disk and memory spaces allocated for terminated process and
	 * delete the terminated process from the process queue and branch back to
	 * loader.
	 */

	/* For Programs on disk */
	for (int j = 0; j < TerminatedQueue
			.getLast().ProgramPagesOnDisk.length; j++) {
	diskIndexToDeleteFrom = Integer
			.parseInt(TerminatedQueue.getLast().ProgramPagesOnDisk[j]);
	Loader.diskAvailability[diskIndexToDeleteFrom] = "true";
	Loader.emptyPagesOnDisk++;
	for (int k = diskIndexToDeleteFrom * 16; k < (diskIndexToDeleteFrom * 16)
			+ 16; k++) {
	diskReference.disk[k] = null;
	}
	}

	/* For data on disk */
	if (!(TerminatedQueue.getLast().currentOperation.equals("error")
			|| TerminatedQueue.getLast().errorDescription
					.equals("Missing Data"))) {
	for (int j = 0; j < TerminatedQueue.getLast().DataPagesOnDisk.length; j++) {
	diskIndexToDeleteFrom = Integer
			.parseInt(TerminatedQueue.getLast().DataPagesOnDisk[j]);
	Loader.diskAvailability[diskIndexToDeleteFrom] = "true";
	Loader.emptyPagesOnDisk++;
	for (int k = diskIndexToDeleteFrom * 16; k < (diskIndexToDeleteFrom * 16)
			+ 16; k++) {
	diskReference.disk[k] = null;
	}
	}
	}
	/* output space on disk */
	for (int j = 0; j < TerminatedQueue
			.getLast().OutputPagesOnDisk.length; j++) {
	diskIndexToDeleteFrom = Integer
			.parseInt(TerminatedQueue.getLast().OutputPagesOnDisk[j]);
	Loader.diskAvailability[diskIndexToDeleteFrom] = "true";
	Loader.emptyPagesOnDisk++;
	for (int k = diskIndexToDeleteFrom * 16; k < (diskIndexToDeleteFrom * 16)
			+ 16; k++) {
	diskReference.disk[k] = null;
	}
	}
	/* program on memory */
	for (int j = 0; j < TerminatedQueue
			.getLast().ProgramPagesOnMem.length; j++) {
	if (!TerminatedQueue.getLast().pageFaultBits.get(j).substring(0, 1)
			.equals("0")) {
	try {
	memIndexToDeleteFrom = Integer
			.parseInt(TerminatedQueue.getLast().ProgramPagesOnMem[j]);
	MemAvailability[memIndexToDeleteFrom] = "true";
	emptyPagesOnMem++;
	for (int k = memIndexToDeleteFrom * 16; k < (memIndexToDeleteFrom * 16)
			+ 16; k++) {
	Memory.Mem[k] = null;
	}
	} catch (NumberFormatException e) {
	ErrorHandler.setJobID(processQueue.get(i).jobID);
	err.HandleError(ErrorHandler.ARTHEMATIC_ERROR);
	processQueue.remove(i);
	break;
	}

	}
	}

	/* from process queue */
	terminatedJobID = TerminatedQueue.getLast().jobID;
	for (int j = 0; j < processQueue.size(); j++) {
	if (processQueue.get(j).jobID == terminatedJobID) {
	processIndexToDeleteFrom = j;
	break;
	}
	}
	processQueue.remove(processIndexToDeleteFrom);
	processQueSize--;
	break;
	}
	} catch (NullPointerException e) {
	ErrorHandler.setJobID(processQueue.get(i).jobID);
	err.HandleError(ErrorHandler.NULL_POINTER);

	for (int j = 0; j < processQueue.size(); j++) {
	if (processQueue.get(j).jobID == terminatedJobID) {
	processIndexToDeleteFrom = j;
	break;
	}
	}
	processQueue.remove(processIndexToDeleteFrom);
	break;
	}
	}

	if (Loader.fileEndedExecuteAnyways == true) {

	while (processQueue.size() > 0) {
	try {
	for (int i = processQueSize; i < processQueue.size(); i++) {
	/* check the number of pages the program takes */
	pagesNeeded = processQueue.get(i).ProgramPagesOnDisk.length;
	if (pagesNeeded == 1) {
	pagesNeeded = 1;
	} else if ((pagesNeeded / 3) <= 2) {
	pagesNeeded = 2;
	} else {
	pagesNeeded = pagesNeeded / 3;
	}
	PagesForProgram = new String[pagesNeeded];
	for (int l = 0; l < pagesNeeded; l++) {
	PagesForProgram[l] = processQueue.get(i).ProgramPagesOnDisk[l];
	}
	if (PagesForProgram.length <= emptyPagesOnMem && lastFewProcess == false) {
	/*
	 * write the contents of the disk into the memory locations available
	 */
	writeDiskContentToMem(PagesForProgram,
			processQueue.get(i).ProgramPagesOnDisk.length,
			processQueue.get(i).jobID);
	processQueue.get(i).ProgramPagesOnMem = new String[_ProgramPagesOnMem
			.size()];
	_ProgramPagesOnMem.toArray(processQueue.get(i).ProgramPagesOnMem);
	_ProgramPagesOnMem.clear();
	processQueue.get(i).currentOperation = "NEW";
	/*
	 * Setting bit 0 as 1 indicating the page is in memory
	 */
	for (int k = 0; k < PagesForProgram.length; k++) {
	processQueue.get(i).pageFaultBits.set(k,
			"1" + processQueue.get(i).pageFaultBits.get(k).substring(1, 3));
	}
	processQueue.get(i).currentSubqueueLevel = 1;
	subQueue1.add(processQueue.get(i));
	}
	if (PagesForProgram.length > emptyPagesOnMem || i == processQueue.size() - 1
			|| lastFewProcess == true) {
	if (subQueue1.size() > 0) {
	processQueSize = subQueue1.size();
	}
	if (subQueue2.size() > 0) {
	processQueSize += subQueue2.size();
	}
	if (subQueue3.size() > 0) {
	processQueSize += subQueue3.size();
	}
	if (subQueue4.size() > 0) {
	processQueSize += subQueue4.size();
	}

	cpu.setMemoryObject(memory);
	scheduleReadyProcesses();
	/*
	 * remove the disk and memory spaces allocated for terminated process and
	 * delete the terminated process from the process queue and branch back to
	 * loader.
	 */
	/* For Programs on disk */
	for (int j = 0; j < TerminatedQueue
			.getLast().ProgramPagesOnDisk.length; j++) {
	diskIndexToDeleteFrom = Integer
			.parseInt(TerminatedQueue.getLast().ProgramPagesOnDisk[j]);
	Loader.diskAvailability[diskIndexToDeleteFrom] = "true";
	Loader.emptyPagesOnDisk++;
	for (int k = diskIndexToDeleteFrom * 16; k < (diskIndexToDeleteFrom * 16)
			+ 16; k++) {
	diskReference.disk[k] = null;
	}
	}
	/* For data on disk */
	if (!(TerminatedQueue.getLast().currentOperation.equals("error")
			|| TerminatedQueue.getLast().errorDescription
					.equals("Missing Data"))) {
	for (int j = 0; j < TerminatedQueue.getLast().DataPagesOnDisk.length; j++) {
	diskIndexToDeleteFrom = Integer
			.parseInt(TerminatedQueue.getLast().DataPagesOnDisk[j]);
	Loader.diskAvailability[diskIndexToDeleteFrom] = "true";
	Loader.emptyPagesOnDisk++;
	for (int k = 16 * diskIndexToDeleteFrom; k < (diskIndexToDeleteFrom * 16)
			+ 16; k++) {
	diskReference.disk[k] = null;
	}
	}
	}
	/* output space on disk */
	for (int j = 0; j < TerminatedQueue
			.getLast().OutputPagesOnDisk.length; j++) {
	diskIndexToDeleteFrom = Integer
			.parseInt(TerminatedQueue.getLast().OutputPagesOnDisk[j]);
	Loader.diskAvailability[diskIndexToDeleteFrom] = "true";
	Loader.emptyPagesOnDisk++;
	for (int k = 16 * diskIndexToDeleteFrom; k < (diskIndexToDeleteFrom * 16)
			+ 16; k++) {
	diskReference.disk[k] = null;
	}
	}
	/* program on memory */
	for (int j = 0; j < TerminatedQueue
			.getLast().ProgramPagesOnMem.length; j++) {
	if (!TerminatedQueue.getLast().pageFaultBits.get(j).substring(0, 1)
			.equals("0")) {
	memIndexToDeleteFrom = Integer
			.parseInt(TerminatedQueue.getLast().ProgramPagesOnMem[j]);
	MemAvailability[memIndexToDeleteFrom] = "true";
	emptyPagesOnMem++;
	for (int k = 16 * memIndexToDeleteFrom; k < (memIndexToDeleteFrom * 16)
			+ 16; k++) {
	Memory.Mem[k] = null;
	}
	}
	}
	/* from process queue */
	terminatedJobID = TerminatedQueue.getLast().jobID;
	for (int j = 0; j < processQueue.size(); j++) {
	if (processQueue.get(j).jobID == terminatedJobID) {
	processIndexToDeleteFrom = j;
	break;
	}
	}
	processQueue.remove(processIndexToDeleteFrom);
	if (processQueue.isEmpty()) {
	}
	processQueSize--;
	break;
	}
	}
	if (processQueSize == processQueue.size() && processQueue.size() > 0) {
	lastFewProcess = true;
	processQueSize = 0;
	if (processQueue.size() == 1) {
	CPU.lastJob = true;
	}
	}
	} catch (Exception e) {
	break;
	}
	}
	}
	}

	private boolean scheduleReadyProcesses() {
	while (hasMore == true) {
	if (subQueue1.size() > 0) {
	runningQueue.add(subQueue1.getFirst());
	} else if (subQueue2.size() > 0) {
	runningQueue.add(subQueue2.getFirst());
	} else if (subQueue3.size() > 0) {
	runningQueue.add(subQueue3.getFirst());
	} else if (subQueue4.size() > 0) {
	runningQueue.add(subQueue4.getFirst());
	}
	// TODO:Need to uncomment and change this
	snapshotCPU();
	if (runningQueue.size() == 0) {
	System.out.print("");
	}
	subqueue1Size = subQueue1.size();
	subqueue2Size = subQueue2.size();
	subqueue3Size = subQueue3.size();
	subqueue4Size = subQueue4.size();
	if (subQueue1.size() > 0) {
	for (int j = 0; j < subQueue1.size(); j++) {
	contentOfSubqueue1.add(subQueue1.get(j).jobID);
	}
	}
	if (subQueue2.size() > 0) {
	for (int j = 0; j < subQueue2.size(); j++) {
	contentOfSubqueue2.add(subQueue2.get(j).jobID);
	}
	}
	if (subQueue3.size() > 0) {
	for (int j = 0; j < subQueue3.size(); j++) {
	contentOfSubqueue3.add(subQueue3.get(j).jobID);
	}
	}
	if (subQueue4.size() > 0) {
	for (int j = 0; j < subQueue4.size(); j++) {
	contentOfSubqueue4.add(subQueue4.get(j).jobID);
	}
	}
	hasMore = cpu.cpu(runningQueue.getFirst().PC,
			runningQueue.getFirst().traceFlag, runningQueue, diskReference);
	contentOfSubqueue1.clear();
	contentOfSubqueue2.clear();
	contentOfSubqueue3.clear();
	contentOfSubqueue4.clear();
	if (runningQueue.getFirst().currentOperation.equals("Read")) {
	/*
	 * The below method is called when nothing is terminated we just shift the
	 * states of process
	 */
	scheduleNextProcess();
	} else if (runningQueue.getFirst().currentOperation.equals("Write")) {
	scheduleNextProcess();
	} else if (runningQueue.getFirst().currentOperation.equals("TimeOut")) {
	scheduleNextProcessT();
	}
	}
	/*
	 * current process done executing or encountered some error while executing
	 */
	if (runningQueue.size() == 0) {
	System.out.print("");
	}
	terminateRunningProcess();
	hasMore = true;
	return false;
	}

	private void snapshotCPU() {
	runningQueue.getFirst().SystemStatus.clear();
	runningQueue.getFirst().SystemStatus.add("Status of the the system:");
	// runningQueue.getFirst().SystemStatus.add("Content of ready queue:");
	String temporary = "";
	for (int i = 0; i < subQueue1.size(); i++) {
	temporary += subQueue1.get(i).jobID + " ";
	}
	runningQueue.getFirst().SystemStatus.add(
			String.format("%-40s %-15s", "Content of subQueue1:", temporary));
	temporary = "";
	for (int i = 0; i < subQueue2.size(); i++) {
	temporary += subQueue2.get(i).jobID + " ";
	}
	runningQueue.getFirst().SystemStatus.add(
			String.format("%-40s %-15s", "Content of subQueue2:", temporary));
	temporary = "";
	for (int i = 0; i < subQueue3.size(); i++) {
	temporary += subQueue3.get(i).jobID + " ";
	}
	runningQueue.getFirst().SystemStatus.add(
			String.format("%-40s %-15s", "Content of subQueue3:", temporary));
	temporary = "";
	for (int i = 0; i < subQueue4.size(); i++) {
	temporary += subQueue4.get(i).jobID + " ";
	}
	runningQueue.getFirst().SystemStatus.add(
			String.format("%-40s %-15s", "Content of subQueue4:", temporary));
	String blockedQueueContent = "";
	for (int i = 0; i < BlockedQueue.size(); i++) {
	blockedQueueContent += BlockedQueue.get(i).jobID;
	}
	runningQueue.getFirst().SystemStatus.add(String.format("%-40s %-15s",
			"Content of Blocked Queue:", blockedQueueContent));
	if (runningQueue.size() > 0) {
	runningQueue.getFirst().SystemStatus.add(String.format("%-40s %-15s",
			"ID of current executing Job:", runningQueue.getFirst().jobID));
	} else {
	runningQueue.getFirst().SystemStatus.add(String.format("%-40s %-15s",
			"ID of current executing Job:", BlockedQueue.getFirst().jobID));
	}
	runningQueue.getFirst().SystemStatus.add(
			String.format("%-40s %-15s", "Memory Free:", emptyPagesOnMem * 16));
	runningQueue.getFirst().SystemStatus.add(String.format("%-40s %-15s",
			"Memory Used:", (256 - (emptyPagesOnMem * 16))));
	runningQueue.getFirst().SystemStatus.add(
			String.format("%-40s %-15s", "Current Degree of Multiprogramming:",
					(BlockedQueue.size() + subQueue1.size() + subQueue2.size()
							+ subQueue3.size() + subQueue4.size())));
	runningQueue.getFirst().SystemStatus
			.add("------------------------------------------------------");
	}

	private void scheduleNextProcessT() {
	/* adding the first process of ready list to the end of the list */
	if (BlockedQueue.size() > 0) {
	if (BlockedQueue.getFirst().currentSubqueueLevel == 1) {
	subQueue1.add(BlockedQueue.getFirst());
	} else if (BlockedQueue.getFirst().currentSubqueueLevel == 2) {
	subQueue2.add(BlockedQueue.getFirst());
	} else if (BlockedQueue.getFirst().currentSubqueueLevel == 3) {
	subQueue3.add(BlockedQueue.getFirst());
	} else if (BlockedQueue.getFirst().currentSubqueueLevel == 4) {
	subQueue1.add(BlockedQueue.getFirst());
	subQueue1.getLast().currentSubqueueLevel = 1;
	}
	BlockedQueue.removeFirst();
	}
	if (runningQueue.getFirst().currentSubqueueLevel == 1) {
	if ((runningQueue.getFirst().TimeQuantum
			* runningQueue.getFirst().numberOfTurns) > (allowedNumberOfTurns
					* allowedQuantum)
			|| runningQueue.getFirst().numberOfTurns > allowedNumberOfTurns) {
	runningQueue.getFirst().currentSubqueueLevel = 2;
	runningQueue.getFirst().TimeQuantum = 0;
	subQueue2.add(runningQueue.getFirst());
	subQueue1.removeFirst();
	matrixMetric++;
	} else {
	subQueue1.add(subQueue1.getFirst());
	subQueue1.removeFirst();
	}
	} else if (runningQueue.getFirst().currentSubqueueLevel == 2) {
	if ((runningQueue.getFirst().TimeQuantum * runningQueue
			.getFirst().numberOfTurns) > ((allowedNumberOfTurns + 2)
					* allowedQuantum)
			|| runningQueue.getFirst().numberOfTurns > allowedNumberOfTurns
					+ 2) {
	runningQueue.getFirst().currentSubqueueLevel = 3;
	runningQueue.getFirst().TimeQuantum = 0;
	subQueue3.add(runningQueue.getFirst());
	subQueue2.removeFirst();
	matrixMetric++;
	} else {
	subQueue2.add(subQueue2.getFirst());
	subQueue2.removeFirst();
	}
	} else if (runningQueue.getFirst().currentSubqueueLevel == 3) {
	if ((runningQueue.getFirst().TimeQuantum * runningQueue
			.getFirst().numberOfTurns) > ((allowedNumberOfTurns + 4)
					* allowedQuantum)
			|| runningQueue.getFirst().numberOfTurns > allowedNumberOfTurns
					+ 4) {
	runningQueue.getFirst().currentSubqueueLevel = 4;
	runningQueue.getFirst().TimeQuantum = 0;
	subQueue4.add(runningQueue.getFirst());
	subQueue3.removeFirst();
	matrixMetric++;
	} else {
	subQueue3.add(subQueue3.getFirst());
	subQueue3.removeFirst();
	}
	} else if (runningQueue.getFirst().currentSubqueueLevel == 4) {
	if ((runningQueue.getFirst().TimeQuantum * runningQueue
			.getFirst().numberOfTurns) > ((allowedNumberOfTurns + 6)
					* allowedQuantum)
			|| (runningQueue.getFirst().TimeQuantum
					* runningQueue.getFirst().numberOfTurns) > (9
							* allowedQuantum * allowedNumberOfTurns)) {
	runningQueue.getFirst().currentSubqueueLevel = 1;
	runningQueue.getFirst().TimeQuantum = 0;
	subQueue1.add(runningQueue.getFirst());
	subQueue4.removeFirst();
	matrixMetric++;
	} else {
	subQueue4.add(subQueue4.getFirst());
	subQueue4.removeFirst();
	}
	}
	runningQueue.removeFirst();
	}

	private void terminateRunningProcess() {
	if (runningQueue.getFirst().currentSubqueueLevel == 1) {
	TerminatedQueue.add(subQueue1.getFirst());
	subQueue1.removeFirst();
	} else if (runningQueue.getFirst().currentSubqueueLevel == 2) {
	TerminatedQueue.add(subQueue2.getFirst());
	subQueue2.removeFirst();
	} else if (runningQueue.getFirst().currentSubqueueLevel == 3) {
	TerminatedQueue.add(subQueue3.getFirst());
	subQueue3.removeFirst();
	} else if (runningQueue.getFirst().currentSubqueueLevel == 4) {
	TerminatedQueue.add(subQueue4.getFirst());
	subQueue4.removeFirst();
	}
	runningQueue.removeFirst();
	if (BlockedQueue.size() > 0) {
	if (BlockedQueue.getFirst().currentSubqueueLevel == 1) {
	subQueue1.add(BlockedQueue.getFirst());
	} else if (BlockedQueue.getFirst().currentSubqueueLevel == 2) {
	subQueue2.add(BlockedQueue.getFirst());
	} else if (BlockedQueue.getFirst().currentSubqueueLevel == 3) {
	subQueue3.add(BlockedQueue.getFirst());
	} else if (BlockedQueue.getFirst().currentSubqueueLevel == 4) {
	subQueue1.add(BlockedQueue.getFirst());
	subQueue1.getLast().currentSubqueueLevel = 1;
	}
	BlockedQueue.removeFirst();
	}
	}

	private void scheduleNextProcess() {
	if (Loader.fileEndedExecuteAnyways == true && processQueue.size() == 1) {
	/*
	 * do nothing we have only one process let it have the CPU even if IO is
	 * being performed
	 */
	runningQueue.removeFirst();

	} else {
	/* unblock the process and put them into appropriate sub queues */
	if (BlockedQueue.size() > 0) {
	if (BlockedQueue.getFirst().currentSubqueueLevel == 1) {
	subQueue1.add(BlockedQueue.getFirst());
	} else if (BlockedQueue.getFirst().currentSubqueueLevel == 2) {
	subQueue2.add(BlockedQueue.getFirst());
	} else if (BlockedQueue.getFirst().currentSubqueueLevel == 3) {
	subQueue3.add(BlockedQueue.getFirst());
	} else if (BlockedQueue.getFirst().currentSubqueueLevel == 4) {
	subQueue1.add(BlockedQueue.getFirst());
	subQueue1.getLast().currentSubqueueLevel = 1;
	}
	BlockedQueue.removeFirst();
	}
	/*
	 * put the running process into blocked queue because it asked for I/O
	 */
	if (runningQueue.getFirst().currentSubqueueLevel == 1) {
	BlockedQueue.add(subQueue1.getFirst());
	subQueue1.removeFirst();
	} else if (runningQueue.getFirst().currentSubqueueLevel == 2) {
	BlockedQueue.add(subQueue2.getFirst());
	subQueue2.removeFirst();
	} else if (runningQueue.getFirst().currentSubqueueLevel == 3) {
	BlockedQueue.add(subQueue3.getFirst());
	subQueue3.removeFirst();
	} else if (runningQueue.getFirst().currentSubqueueLevel == 4) {
	BlockedQueue.add(subQueue4.getFirst());
	subQueue4.removeFirst();
	}
	runningQueue.removeFirst();
	}
	}

	private void writeDiskContentToMem(String[] pagesForProgram2, int lenth,
			int jobid) {
	int programIndex = 0;
	PageIndex = 0;
	try {
	while (PageIndex < 16 && programIndex < pagesForProgram2.length) {
	if (MemAvailability[PageIndex].equals("true")) {
	MemAvailability[PageIndex] = "false";
	int thisPageIndexInDisk = Integer.parseInt(pagesForProgram2[programIndex]);
	for (int i = thisPageIndexInDisk * 16, j = 0; i < (thisPageIndexInDisk * 16)
			+ 16; i++, j++) {
	blockForMem[j] = diskReference.disk[i];
	}
	memory.writeToMemory("WRIT", PageIndex, blockForMem);
	_ProgramPagesOnMem.add(Integer.toString(PageIndex));
	emptyPagesOnMem--;
	programIndex++;
	}
	PageIndex++;
	}
	int temp = _ProgramPagesOnMem.size();
	for (int i = temp; i < lenth; i++) {
	_ProgramPagesOnMem.add(null);
	}
	} catch (Exception e) {
	ErrorHandler.setJobID(jobid);
	err.HandleError(ErrorHandler.OUT_OF_BOUNDS);

	}

	}
}

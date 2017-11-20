/*
 *Brief description: This is data structure used to simulate a PCB in the system.It will be 
 * be an aid in facilitating multiprogramming as we can save the state of the system into this
 * data Structure when we want to context switch.
 *
 */
import java.util.ArrayList;
import java.util.LinkedList;

public class PCB {
	public int PC;
	public String[] ProgramPagesOnDisk;
	public String[] DataPagesOnDisk;
	public String[] OutputPagesOnDisk;
	public String[] ProgramPagesOnMem;
	public String[] DataPagesOnMem;
	public String[] OutputPagesOnMem;
	public String[] registers;
	public String[] GeneralPurpouse;
	public ArrayList<String> PageTable;
	public int timeJobEnteredTheSystem;
	public int cumilativeTimeUsedByJob;
	public int jobID;
	public String diskAddressofCurrentRD;
	public String diskAddressofCurrentWR;
	public int timeOfCompletionOfIO;
	public int traceFlag;
	public String currentOperation;
	/*RFWT=Read from or write to*/
	public String AddressRFWT;
	public String resultOfReadOperation;
	public String resultOfWriteOperation;
	public int readPointer;
	public int writePointer;
	public String errorDescription;
	public ArrayList<String> pageFaultBits;
	public int pageFaultHandlingTime;
	public LinkedList<Object> SystemStatus=new LinkedList<Object>();
	public boolean startedWritingTrace;
	public int iotime;
	public int TimeQuantum;
	public int numberOfTurns;
	public int currentSubqueueLevel;
	public int numberOfChancesCPU;
}

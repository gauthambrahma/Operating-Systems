
/*Author: Gautham Brahma Ponnaganti.
 *Global Variables:
 *-errorCode: This can be set by a piece of code where the error occurs and later when the error handler class 
 *calls this code it can keep track of which error by using this variable.
 *-  
 *
 *Brief description: This is the error handler subsystem which will trap errors in other subsystems. Then it will check the 
 *error code with custom error class and pull out the error number and custom error message and display the 
 *exception occurred in user readable format
 *
 *Changes in PhaseII:
 *Added new exceptions which arise due to reading of new file format when loading to disk and multiprogramming
 *and Page replacement.
 *
 *Changes in PhaseIII:None
 */
import java.io.FileWriter;
import java.io.IOException;

public class ErrorHandler {

	public void errorhandler(int e) {
	String errorMessage = null;
	int jobID = 0;
	errorCode = e;
	errorMessage = getProperty(String.valueOf(errorCode));
	jobID = ErrorHandler.getJobID();
	try {
	String filename = "progressFile.txt";
	FileWriter fw = new FileWriter(filename, true);
	fw.write("\n");
	if (errorCode > 50) {
	fw.write("**WARNING: ");
	} else {
	fw.write("**ERROR: ");
	}
	fw.write("JOBID(DEC):" + jobID + " Code:" + errorCode + " ");
	fw.write("Description:" + errorMessage + "\n");
	fw.write("------------------------------------------------------");
	fw.close();
	} catch (IOException e1) {
	e1.printStackTrace();
	}
	}

	/*
	 * This is to notify the user if an error in system which is not specified
	 * below occurs
	 */
	public String HandleError(int exception) {
	errorhandler(exception);
	if (exception > 50)
		return "Warning";
	else
		return "Error";
	}

	public static final int SYSTEM_ERROR = 999;

	/* IO related errors */
	public static final int FILE_NOT_FOUND = 2;
	public static final int INVALID_USER_INPUT = 3;
	public static final int UNABLE_TO_CREATE_FILE = 4;

	/* Loader related errors */
	public static final int ILLEGAL_CHAR = 5;
	public static final int INVALID_TRACE_FLAG = 6;
	public static final int PROGRAM_SIZE_TOO_LARGE = 7;

	/* Memory related exceptions */
	public static final int ADDRESS_OUT_OF_RANGE = 8;

	/* Arithmetic exceptions */
	public static final int DIVIDE_BY_ZERO = 9;
	public static final int INPUT_SIZE_OVERFLOW = 10;

	/* MISC */
	public static final int ILLEGAL_BATCH_FORMAT = 11;
	public static final int MISSING_PROGRAM = 12;
	public static final int MISSING_JOB_TAG = 13;
	public static final int TRACE_MISSING = 14;
	public static final int MISSING_DATA = 15;
	public static final int NULL_JOB = 16;
	public static final int INSUFFICIENT_OUTPUT_SPACE = 17;
	public static final int ARTHEMATIC_ERROR = 18;
	public static final int OUT_OF_BOUNDS = 19;
	public static final int IO_EXCEPTION = 20;
	public static final int NULL_POINTER = 21;
	public static final int SUSPECTED_INFINITE_JOB = 22;
	public static final int INVALID_OPCODE = 23;

	/* Warnings */
	public static final int MISSING_FIN = 51;
	public static final int DOUBLE_DATA = 52;
	public static final int MISSING_DATA_TAG = 53;

	private int errorCode;
	public static int JobID;

	public void customException(int msg) {
	this.errorCode = msg;
	}

	public int getErrorCode() {
	return errorCode;
	}

	public static void setJobID(int jobID) {
	JobID = jobID;
	}

	public static int getJobID() {
	return JobID;
	}

	public static String getProperty(String errorCode) {
	String errorMessage = null;
	if (errorCode.equals("2")) {
	errorMessage = "The file name you entered does not exist.";
	} else if (errorCode.equals("5")) {
	errorMessage = "Illegal Batch Character.";
	} else if (errorCode.equals("3")) {
	errorMessage = "Invalid input. The input entered must be hex number.";
	} else if (errorCode.equals("6")) {
	errorMessage = "Invalid Trace flag.";
	} else if (errorCode.equals("7")) {
	errorMessage = "Program Size too large.";
	} else if (errorCode.equals("8")) {
	errorMessage = "Address out of range. The address referenced by the program counter must be less than the size of the memory.";
	} else if (errorCode.equals("9")) {
	errorMessage = "Attempt to divide by zero.";
	} else if (errorCode.equals("4")) {
	errorMessage = "Unable to create file";
	} else if (errorCode.equals("10")) {
	errorMessage = "Buffer overflow.";
	} else if (errorCode.equals("11")) {
	errorMessage = "Illegal batch format";
	} else if (errorCode.equals("12")) {
	errorMessage = "Missing Program";
	} else if (errorCode.equals("13")) {
	errorMessage = "Missing Job";
	} else if (errorCode.equals("14")) {
	errorMessage = "Trace Bit Missing";
	} else if (errorCode.equals("15")) {
	errorMessage = "MISSING DATA";
	} else if (errorCode.equals("16")) {
	errorMessage = "NULL JOB";
	} else if (errorCode.equals("17")) {
	errorMessage = "Insufficient Output Space";
	} else if (errorCode.equals("18")) {
	errorMessage = "Problem with number conversion.";
	} else if (errorCode.equals("19")) {
	errorMessage = "Out of Bounds";
	} else if (errorCode.equals("20")) {
	errorMessage = "IO Exception";
	} else if (errorCode.equals("21")) {
	errorMessage = "Null pointer Exception";
	} else if (errorCode.equals("22")) {
	errorMessage = "Suspected Infinite Job";
	} else if (errorCode.equals("23")) {
	errorMessage = "Invalid Opcode";
	} else if (errorCode.equals("51")) {
	errorMessage = "MISSING **FIN";
	} else if (errorCode.equals("52")) {
	errorMessage = "Double **DATA";
	} else if (errorCode.equals("53")) {
	errorMessage = "MISSING **DATA";
	} else if (errorCode.equals("999")) {
	errorMessage = "System Error";
	}
	return errorMessage;
	}
}

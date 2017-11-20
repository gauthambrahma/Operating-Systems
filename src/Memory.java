
/*-MEM: This is used as a data structure to simulate physical memory.
 * 
 *
 *Brief description: This is used to simulate memory. 
 *
 *Changes in phase II:In this phase we will add a page table in the memory and 
 *all the address references to the memory will be virtual which will be converted into 
 *physical before we access the page table.
 *
 *Changes in Phase III:None
 */
import java.io.FileWriter;

public class Memory {
	public static String[] Mem = new String[256];
	static int[] pageTable = new int[16];

	/*
	 * x specifies read or write or dump,y is for memory address and z is the
	 * variable to be written or read into
	 */
	public void Mem(String x, int y, String[] z) {
	/*
	 * Initializing memory as an array of strings size 256 as it is given that
	 * array should be indexed from 00 to FF .The incoming indexes will be in
	 * hex values which we will convert them to decimal indexes
	 */
	/* translating physical address to virtual address */
	y = convertVirtualAddressToPhysical(y);
	if (y > 252) {
	}
	/*
	 * When we get an instruction to write we start from the location(which is
	 * y) and write to the next 4 memory locations
	 */
	else if (x.equals("WRIT")) {
	for (int i = y, k = 0; i < y + 4; i++, k++) {
	/*
	 * writing contents of z to 4 contiguous memory location starting from y
	 */
	Mem[i] = z[k];
	}
	} else if (x.equals("DUMP")) {
	try {
	String filename = "progressFile.txt";
	FileWriter fw = new FileWriter(filename, true);
	// fw.write("Memory Dump(in HEX): ");
	for (int i = 0; i < Mem.length; i++) {
	CPU.memoryDumpArray.add(Mem[i]);

	}
	fw.close();
	} catch (Exception e) {
	e.printStackTrace();
	}
	}
	}

	public int convertVirtualAddressToPhysical(int y) {
	int virtualAddressInDecimal = y;
	String virtualAddressIndex = String.format("%02X", virtualAddressInDecimal);
	/*
	 * Page Table being an array is indexed in decimal. The hex value needs to
	 * be converted to decimal
	 */
	String PageTableIndex = virtualAddressIndex.substring(0, 1);
	String Offset = virtualAddressIndex.substring(1, 2);
	int PageTableIndexDec = Integer.parseInt(PageTableIndex, 16);
	int FrameNumber = pageTable[PageTableIndexDec];

	String FrameNumberHex = String.format("%01X", FrameNumber);
	String PhysicalAddress = FrameNumberHex + Offset;
	return Integer.parseInt(PhysicalAddress, 16);
	}

	/*
	 * we are overloading Mem method because we are expecting a different return
	 * type from each of these methods and because we are writing the content
	 * from register which is a string and not content of a buffer which is a
	 * string array
	 */
	public String Mem(String x, int y, String z) {
	y = convertVirtualAddressToPhysical(y);
	if (x.equals("READ")) {
	z = Mem[y];
	} else if (x.equals("WRIT")) {
	/* writing contents of register z to memory location y */
	if (z.length() < 8) {
	for (int k = z.length(); k < 8; k++) {
	z = "0" + z;
	}
	}
	Mem[y] = z;
	}
	if (z == null) {
	z = "00000000";
	}
	return z;
	}

	/*
	 * This method will be accessed by the loader to initialize the page table
	 */
	public void initializePageTable(String index) {
	if (index.equals("i")) {
	for (int i = 0; i < pageTable.length; i++) {
	pageTable[i] = i;
	}
	} else if (index.equals("15-i")) {
	for (int i = 0; i < pageTable.length; i++) {
	pageTable[i] = 15 - i;
	}
	}
	}

	/* This is for pageTransfer */
	public void writeToMemory(String Action, int pageNumber,
			String[] PageToWrite) {
	if (Action.equals("WRIT") || (pageNumber * 16) < 255) {
	for (int i = pageNumber * 16, j = 0; i < (pageNumber * 16) + 16; i++, j++) {
	Mem[i] = PageToWrite[j];
	}
	}
	}

}

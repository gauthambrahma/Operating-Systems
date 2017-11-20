
/*Global Variables:
 *-SizeOfDisk:Specifies the size of the disk
 *-disk:array which is used for storage as a disk simulation
 *-spaceLedt:To keep track of empty space in disk.
 *-jobTracerPair:used in Loader 
 *-schedule:instance of scheduler
 *
 *Brief description: This class is the simulation of disk structure which holds data in a memory and have properties
 *such as empty space, total size etc. 
 *
 */

import java.util.ArrayList;
public class Disk {
	int sizeOfDisk = 4096;// 16 times the size of memory
	String[] disk = new String[sizeOfDisk];
	int[] DiskPageTable = new int[sizeOfDisk / 16];// 256
	int spaceLeft = sizeOfDisk;
	public ArrayList<Integer> jobTracerPair = new ArrayList<Integer>();
	public ArrayList<ArrayList<Integer>> JobTracker = new ArrayList<ArrayList<Integer>>();
	Scheduler schedule = new Scheduler();

	public void disk() {

	}

	/* This is used to initialize disk page table from loader */
	public void intitalizePageTable() {
	for (int i = 0; i < DiskPageTable.length; i++) {
	DiskPageTable[i] = i;
	}
	}

	/* this method can be used by others to probe the disk space available */
	public int diskSpaceAvailable() {
	return spaceLeft;
	}

	public void DumpDisk() {
	for (int i = 0; i < disk.length; i++) {
	System.out.println(disk[i]);
	}
	}

	public void writeToDisk(ArrayList<String> pageContent, int PageTableIndex) {
	int FrameStartAddress = getFrameStartAddress(PageTableIndex);
	for (int i = FrameStartAddress, j = 0; i < FrameStartAddress
			+ 16; i++, j++) {
	disk[i] = pageContent.get(j);
	}
	}

	private int getFrameStartAddress(int pageTableIndex) {
	/*
	 * we are appending two zeros at the end which is same as multiplying by 16
	 * in decimal
	 */
	return DiskPageTable[pageTableIndex] * 16;
	}

}

/* 
 * Copyright 2024 Ethan E. Gibson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.kq4iyo.hs;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

public class HyperSearch {
	
	public static AtomicInteger threadsActive = new AtomicInteger(0);
	public static int MAX_THREADS = 50;
	public static boolean doSearchDirNames = false;
	
	/**
	 * A wrapper class for File.listRoots()
	 * @return the value of File.listRoots
	 */
	public static File[] getDrives() {
        return File.listRoots();
    }

	/**
	 * Recursively searches a given directory for files with names matching the regex pattern
	 * @param root - The file to search
	 * @param pattern - The pattern to match names against
	 * @param threadNumber - The identification number of the thread running this search
	 * @return ArrayList<String> containing all matches if any.
	 */
	public static ArrayList<String> recursiveSearch(File root, String pattern, int threadNumber) {
		// Store our matches here
		ArrayList<String> matches = new ArrayList<>();
		
		// Get the children of the file
		File[] children = root.listFiles();
		if(children == null) // This should only happen with empty directories or where we don't have permission to read the directory
			return matches;
		
		for(File child:children) {
			if(!child.exists())
				continue;
				
			// If the file is a directory we search it, otherwise check its name
			if(child.isDirectory()) {
				matches.addAll(recursiveSearch(child, pattern, threadNumber));
				if(doSearchDirNames) {
					if(child.getName().matches(pattern))
					{
						matches.add(child.getAbsolutePath());
						System.out.println(Ansi.ansi().fg(Ansi.Color.GREEN).a("[" + threadNumber + "] Found a match at: " + child.getAbsolutePath()).reset());
					}
				}
			}else{
				if(child.getName().matches(pattern)) {
					matches.add(child.getAbsolutePath());
					System.out.println(Ansi.ansi().fg(Ansi.Color.GREEN).a("[" + threadNumber + "] Found a match at: " + child.getAbsolutePath()).reset());
				}
			}
		}
		
		// return our matches (if any)
		return matches;
	}
	
	/**
	 * The C Drive is so big that it needs special consideration
	 * Treat each directory inside of the C Drive as a root for its own thread
	 * @param pattern - the Pattern to Match
	 */
	public static void searchLargeDrive(File f, String pattern) {
		File[] children = f.listFiles();
		
		for(File child:children) {
			Thread t = new Thread() {
				@Override
				public void run() {
					File search = new File(child.getAbsolutePath());
					String p = new String(pattern);
					int threadNumber = Integer.parseInt(this.getName());
					
					recursiveSearch(search,p,threadNumber);
					
					HyperSearch.threadsActive.addAndGet(-1);
					try {
						this.join();
					}catch(Exception e) {
					}
				}
			};
			t.setName(String.valueOf(threadsActive.get() + 1));
			while(threadsActive.get() > MAX_THREADS)
			{
				try { Thread.currentThread().wait(1000); } catch(Exception e) {}
			}
			
			HyperSearch.threadsActive.addAndGet(1);
			t.start();
		}
	}
	
	/**
	 * returns the used space on the drive in GBs
	 * @param drive - The drive to check
	 * @return Space Used in GB
	 */
	public static long getDriveUsedSize(File drive) {
		long driveUsage = 0;
		
		// Get the size of the disk in GB.
		double diskSize = drive.getTotalSpace() / (1024.0 * 1024 * 1024);
		
		// Get the free space on the drive in GB
		double freeSpace = drive.getFreeSpace() / (1024.0 * 1024 * 1024);
		
		// Get how much is used
		driveUsage = (long)(diskSize - freeSpace);
		
		return driveUsage;
	}
	
	/**
	 * A wrapper for System.out.println
	 * @param s - The String to print
	 */
	public static void print(Object o) {
		System.out.println(o);
	}
	
	@SuppressWarnings("static-access") // I'm aware it's static, thank you
	public static void main(String[] args) {
		
		AnsiConsole.systemInstall();
		
		// Introduction Header
		print(Ansi.ansi().fg(Ansi.Color.YELLOW).a("/======================================\\"));
		print(Ansi.ansi().a("|                                      |").reset());
		print(Ansi.ansi().fg(Ansi.Color.YELLOW).a("| ").reset().a("HyperSearch - A file search utility").fg(Ansi.Color.YELLOW).a("  |"));
		print(Ansi.ansi().a("|                                      |"));
		print(Ansi.ansi().a("|          ").reset().a("Threading Enabled").fg(Ansi.Color.YELLOW).a("           |"));
		print(Ansi.ansi().a("|                                      |"));
		print(Ansi.ansi().a("\\======================================/").reset());
		System.out.print("\n");
		
		// Calculate a thread count, cap between 5 and 15.
		int numProcessors = Runtime.getRuntime().availableProcessors();
		MAX_THREADS = (numProcessors * 2) / 3;
		MAX_THREADS = Math.min(Math.max(MAX_THREADS, 5), 15);
		print("Your computer has " + Ansi.ansi().fg(Ansi.Color.BLUE).a(numProcessors).reset() + " processors, we will us a maximum of " + MAX_THREADS + " threads.\n\n");
		
		
		// Get Search Expression
		Scanner s = new Scanner(System.in);
		System.out.print("Input Search String (regex): ");
		String pattern = s.nextLine();
		
		// Determine if we should search directory names
		System.out.print("Search directory names (yes/no)[default=no]: ");
		String answer = s.nextLine().toLowerCase();
		if(answer.equals("yes") || answer.equals("ye") || answer.equals("y"))
			doSearchDirNames = true;
		
		// No more input
		s.close();
		
		// Console Padding
		print("\nProgram will search " + getDrives().length + " drives.");
		if(doSearchDirNames)
			print("Program will search directory names.");
		else
			print("Program will " + Ansi.ansi().fg(Ansi.Color.RED).a("not").reset() + " search directory names.");
		print("\n\n");
		
		long startTime = System.currentTimeMillis();
		
		// Get All Drives
		for(File f:getDrives())
		{
			
			// If the C:\ drive OR more than 150GB of space has been used on the disk
			// AND if there are 15 or more top-level files in the drive
			if(f.getAbsolutePath().equals("C:\\") || getDriveUsedSize(f) > 150 && (f.listFiles() != null && f.listFiles().length >= 15))
			{
				System.out.println("The " + f.getAbsolutePath() + " Drive Will Be Searched Using Multiple Threads");
				searchLargeDrive(f, pattern);
				continue;
			}
			
			// Create a new thread to search one specific drive
			Thread t = new Thread(){
				
				@Override
				public void run() {
					// Grab our ID from our name
					int threadNumber = Integer.parseInt(this.getName());
					
					// Get a copy of the file root
					File root = new File(f.getAbsolutePath());
					
					// Start Search
					print("[" + threadNumber + "] Started Searching " + root.getAbsolutePath());
					recursiveSearch(f, pattern, threadNumber);
					
					// Search is over, notify user and main thread
					print("[" + threadNumber + "] Finished Searching " + root.getAbsolutePath());
					HyperSearch.threadsActive.addAndGet(-1);
					try {
						this.join();
					}catch(Exception e) {
					}
				}
				
			};
			// Set the thread name
			t.setName(String.valueOf(threadsActive.get() + 1));

			// Wait for a free thread to open up
			while(threadsActive.get() > MAX_THREADS)
			{
				try { Thread.currentThread().wait(1000); } catch(Exception e) {}
			}
			
			
			// Increase the number of threads (for tracking)
			threadsActive.addAndGet(1);
			
			// Start the Thread
			t.start();
			AnsiConsole.systemUninstall();
		}
		
		// While our threads are doing things we just kinda wait around doing nothing.
		while(threadsActive.get() > 0)
		{
			try {
				// Sleep a while, wake up, re-check, sleep some more.
				Thread.currentThread().wait(2500);
			}catch(Exception e) {
			}
		}
		
		long endTime = System.currentTimeMillis();
		double executionTime = (endTime - startTime) / 1000.0;
		
		// Tell the user we are done.
		print(Ansi.ansi().fg(Ansi.Color.GREEN).a("\n\nSearching Completed! Search took " + executionTime + " seconds\n").reset().a("Press the '").fg(Ansi.Color.RED).a("X").reset().a("' on the top-right to close the console."));
	}
	
}

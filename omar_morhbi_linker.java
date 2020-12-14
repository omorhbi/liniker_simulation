import java.util.*;
import java.io.*; 

public class omar_morhbi_linker {
	public static ArrayList<String> symbolNames = new ArrayList<>();
	public static ArrayList<Integer> symbolAddresses = new ArrayList<>();
	public static ArrayList<Boolean> symbolsUsed = new ArrayList<>();
	public static ArrayList<Boolean> useExists = new ArrayList<>();
	public static ArrayList<Integer> typeList = new ArrayList<>();
	public static ArrayList<Integer> usedLoc = new ArrayList<>();
	
	public static void main(String[] args) throws IOException {

		String input = "";
		String line;
		System.out.println("Input:");
		Scanner userInput = new Scanner(System.in, "UTF-8");
		while (userInput.hasNextLine()) {
			line = userInput.nextLine();
			input += line + "\n";
		}
		Scanner scan = new Scanner(input);
		
		ArrayList<ArrayList<Integer>> memoryMap = new ArrayList<>();
		
		
		int baseAddress = 0;
		int numModules = scan.nextInt();
		
		// First Pass
		for (int i = 0; i < numModules; i++) {
			int definitionPairs = scan.nextInt();
	
			// Definition list
			for (int j = 0; j < definitionPairs; j++) {
				String symbol = scan.next();
				int tempAddress = scan.nextInt();
				
				int address = baseAddress + tempAddress;
				if (dupeCheck(symbol, address) != true){
					symbolNames.add(symbol);
					symbolAddresses.add(address);
					symbolsUsed.add(false);
					
				}
				else{
					continue;
				}
					
			}
			
			// Use list: skip for pass 1
			scan.nextLine();
			scan.nextLine();
			
			// Program list: increment base address only for pass 1
			int moduleSize = scan.nextInt();
			baseAddress += moduleSize;
			
			scan.nextLine();
		}
		
		scan.close();
		
		// --- SECOND PASS ---
		// During the second iteration, we recreate the memory table.
		// 1 - Immediate. Unchanged.
		// 2 - Absolute. Unchanged.
		// 3 - Relative. Must be relocated based on base address offset.
		// 4 - External. Must be resolved. 
		
		
		baseAddress = 0;
		scan = new Scanner(input);
		
		// Get the number of modules and go to the next line
		numModules = scan.nextInt();
		
		scan.nextLine();
		
		int totalModuleSize = 0;
		for (int i = 0; i < numModules; i++) {
			
			// Definition list: Skip for pass 2
			int numDefs = scan.nextInt();
			for (int j = 0; j < numDefs; j++) {
				scan.next();
				scan.next();
			}
			
			// Use list: Place symbol address/symbol into a Hashtable.
			int numUsePairs = scan.nextInt();
			ArrayList<Integer> usedSymbolAddresses = new ArrayList<>();
			ArrayList<String> usedSymbols = new ArrayList<>();
			ArrayList<Boolean> usedExists = new ArrayList<>();
			ArrayList<Boolean> useUsed = new ArrayList<>(); // Checks if programList elements have been used
		
			for (int j = 0; j < numUsePairs; j++) {
				
				String symbol = scan.next();
				int symbolAddress = scan.nextInt();
			
				usedSymbolAddresses.add(symbolAddress);
				usedSymbols.add(symbol);
				
				usedLoc.add(i);
				
				while (scan.hasNextInt()) {
					symbolAddress = scan.nextInt();
	
					if (symbolAddress == -1) {
						usedSymbolAddresses.add(symbolAddress);
						break;
					}
					else {
						usedSymbolAddresses.add(symbolAddress);
					}
				}
				
				if (symbolNames.contains(symbol)) {
					int symbolIndex = symbolNames.indexOf(symbol);
					symbolsUsed.set(symbolIndex, true);
					usedExists.add(true);
				}
				else {
					System.out.println(symbol + " is used but not defined; 111 used.");
					usedExists.add(false);
				}
				
			}
			
			// Program list: Adjust addresses 
			ArrayList<Integer> programList = new ArrayList<Integer>();
			int moduleSize = scan.nextInt();
			totalModuleSize += moduleSize;
			String[] programArray = scan.nextLine().split("  ");
			
			// first checks if a symbol's definition exceeds module size
			for (int y = 0; y < symbolAddresses.size(); y++) {
				int symAddress = symbolAddresses.get(y);
				if (symAddress > totalModuleSize - 1 && i == numModules - 1) {
					String symbolToGet = symbolNames.get(symbolAddresses.indexOf(symAddress));

					System.out.println(symbolToGet + "'s definition exceeds module size; last word in module used");
					int symAddressToChange = symAddress - (symAddress - (totalModuleSize - 1));
					symbolAddresses.set(y, symAddressToChange);
					
					int location = usedLoc.get(symbolNames.indexOf(symbolToGet));

					int counter = 0;
					
					for (int z = typeList.indexOf(location) + 1; z < typeList.size(); z++) {
						if (typeList.get(z) == 14) {
							int addressVal = memoryMap.get(location).get(counter);
							addressVal = addressVal - (symAddress - (totalModuleSize - 1));
							memoryMap.get(location).set(counter, addressVal);
						}
						if (typeList.get(z) == location + 1) {
							location++;
							counter = 0;
							continue;
						}
						counter += 1;
					}
				}

			}
			// Don't change as you go. Save the programlist and typelist and modify afterwards.
			// That way you can modify and print at the same time
			
			// check if programArray has an index with length greater than 10 (edge scenario)
			for (int z = 0; z < programArray.length; z++) {
				if (programArray[z].length() > 10) {
					programArray[z-1] = programArray[z];
					String stringToAdd = programArray[z].substring(6);
					programArray[z] = stringToAdd;
				}
			}
			
			typeList.add(i);
			for (int j = 0; j < programArray.length; j++) {
				
				if (programArray[j].length() < 5)
					continue;
	
				int address = Integer.parseInt(programArray[j].trim().substring(0, 4));
				
				char type = programArray[j].charAt(programArray[j].length() - 1);
				typeList.add(Character.getNumericValue(type) + 10);
				
				int nextAddress = nextAddress(address);
				
				switch (type) {
					case '1': // Immediate
						break;
					case '2': // Absolute
						if (nextAddress >= 300) {
							System.out.println("Absolute type address " + address +" exceeds machine size; max legal value used");
							address = changeAddress(address, 299);
						}
						break; 
					case '3': // Relative
						address += baseAddress;
						break;
					case '4': // External
						break;
				}
				programList.add(address);
				useUsed.add(false); // All addresses are marked unused at first
			}
			
			// Adjust External addresses last
			
			int indexCount = 0;
			for (int j = 0; j < usedSymbols.size(); j++) {
				
				String symbol = usedSymbols.get(j);

				for (int x = indexCount; x < usedSymbolAddresses.size(); x++) {
					int index = usedSymbolAddresses.get(x);
					
					if (index == -1) {
						indexCount++;
						break;
					}
					// If offset doesn't exist use 111
					int tempAddress = programList.get(index);
					int offset = usedExists.get(j) ? symbolAddresses.get(symbolNames.indexOf(symbol)) : 111;
				
					int finalAddress = changeAddress(tempAddress, offset);
					programList.set(index, finalAddress);
					
					// Check if used, then mark as used
					if (useUsed.get(index) == true) {
						if (memoryMap.isEmpty()) {
							System.out.println("Error: Multiple symbols used for memory map " + index + "; last one used.");
						}
						if (memoryMap.isEmpty() == false){
							int addressToGet = memoryMap.get(i).get(index);
							System.out.println("Error: Multiple symbols used for memory map " + memoryMap.indexOf(addressToGet) + "; last one used");
						}
					}					
					useUsed.set(index, true);
					indexCount++;
				}
			}
			
			memoryMap.add(programList);
			baseAddress += moduleSize;
		}

		// Outputs
		System.out.println("Symbol Table");
		
		for (int z = 0; z < symbolNames.size(); z++) {
			System.out.println(symbolNames.get(z) + "=" +  symbolAddresses.get(z));
		}
		
		System.out.println('\n');
		for (ArrayList<Integer> list : memoryMap) {
			System.out.println("---");
			for (int address : list) {
				System.out.println(address);
			}
		}
		System.out.println("---");
		
		
		for (int i = 0; i < symbolNames.size(); i++) {
			if (!symbolsUsed.get(i))
				System.out.println("Warning: Symbol " + symbolNames.get(i) + " is defined but never used");
		}
		
		scan.close();
		userInput.close();
	} // End main

	public static boolean dupeCheck(String symbol, int address){
		if (symbolNames.contains(symbol)){
			int indexToChange = symbolNames.indexOf(symbol);
			symbolAddresses.set(indexToChange, address);
			System.out.println("Error: " + symbol + "=" + symbolAddresses.get(symbolNames.indexOf(symbol)) +  " is multiply defined; only last value will be used.");
			return true;
		}
		return false;
	}

	public static int changeAddress(int address, int offset) {
		address = ((address / 1000) * 1000);	
		address += offset;
		return address;
	}
	
	public static int nextAddress(int address) {
		int multiplier = 1;
		int nextAddress = 0;
		while (address / 10 != 0) {
			int remainder = address % 10;
			nextAddress += (remainder * multiplier);
			multiplier *= 10;
			address /= 10;
		}
		
		return nextAddress;
	}
	
}

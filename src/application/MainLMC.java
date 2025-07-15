package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.LMC_Engine;

public class MainLMC extends Application {
	private LMC_Engine lmcEng = new LMC_Engine();
	private static Stage pStage;
	private static GridPane root;
	private static BorderPane instructionPane;
	private static GridPane memoryPane;
	private static BorderPane emulationPane;
	private static Button compAndLoad;
	private static Button importFile;
	private static Button exportFile;
	private static HBox executionBox;
	private static HBox emulatorBox;
	private static HBox instructionBox;
	private static TextArea instructionText;
	private static TextArea consoleText;
	private static TextField[] memoryTF;
	private static TextField tfReg;
	private static CheckBox ckOverFlow;
	private static TextField tfProgramCounter;
	private static FileChooser fileChooser;
	private static VBox vb;
	private static CheckMenuItem mFast;
	private static CheckMenuItem mSlow;
	private static CheckMenuItem mNormal;
//	private static FileChooser outputChooser;
	private static int programCounter;
	private static int register;
	private static boolean overflow;
	private static int[] memory;
	private static String compileResults;
	private static HashMap<String, Integer> labelMap;
	private static HashMap<String, Integer> pnuemonicsMap;
	private static HashSet<String> pnuemonics;
	private static String fileName;
	private static String dirPath;
	private static boolean isWindows;
	private static int prevMemLoc;
	private static int prevPC;
	private static boolean active = false;
	private static boolean done = false;
	private static int runDelay = 250;
	private static boolean debugMode = false;

	private static final String BLUE_BORDER = "-fx-border-color: blue;";
	private static final String RED_BORDER = "-fx-border-color: red; -fx-border-width: 3px;";
	private static final String CYAN_BORDER = "-fx-border-color: cyan; -fx-border-width: 3px;";
	private static final String GREEN_BORDER = "-fx-border-color: green; -fx-border-width: 3px;";
	private static final String RED_FONT = "-fx-text-fill: red";
	private static final String BLACK_FONT = "-fx-text-fill: black";
	private static final Font hdrFont = Font.font("Arial", FontWeight.BOLD, 12);

	@Override
	public void start(Stage primaryStage) {
		programCounter = 0;
		prevPC = 0;
		register = 0;
		overflow = false;
		prevMemLoc = -1;
		memory = new int[100];
		labelMap = new HashMap<String, Integer>();
		pnuemonics = new HashSet<String>(
				Arrays.asList("HLT", "ADD", "SUB", "LDA", "STA", "BRA", "BRZ", "BRP", "INP", "OUT"));
		pnuemonicsMap = new HashMap<String, Integer>();
		initializeHashMap();
		root = new GridPane();
		root.setHgap(5);
		root.setVgap(5);
		root.setPadding(new Insets(10));
		ColumnConstraints first = new ColumnConstraints(400);
		first.setHgrow(Priority.ALWAYS);
		root.getColumnConstraints().addAll(first);
		initInstructionPane();
		initInstructionBox();
		initMemoryPane();
		initEmulationPane();
		initExecutionBox();
		initEmulatorBox();
		initMenus();
		

		Scene scene = new Scene(vb, 1600, 415);
		pStage = primaryStage;
		pStage.setScene(scene);
		pStage.show();
	}

	private static void initMenus() {
		MenuBar mb = new MenuBar();
		Menu mAbout = new Menu("About...");
		Menu mControl = new Menu("Run Controls");
		MenuItem mVer = new MenuItem("Version");
		MenuItem mLbl = new MenuItem("How To: Labels");
		MenuItem mVar = new MenuItem("How To: Variables");
		MenuItem mSMC = new MenuItem("How To: Self-Modifying Code");
		MenuItem mDump = new MenuItem("Memory Dumps");
		mAbout.getItems().addAll(mVer,mLbl,mVar,mSMC,mDump);
		mFast = new CheckMenuItem("Run with 100mS delay");
		mFast.setSelected(false);
		mFast.setOnAction(e -> updateRunDelay(e));
		mNormal = new CheckMenuItem("Run with 250mS delay (default)");
		mNormal.setSelected(true);
		mNormal.setOnAction(e -> updateRunDelay(e));
		mSlow = new CheckMenuItem("Run with 500mS delay");
		mSlow.setOnAction(e -> updateRunDelay(e));
		mSlow.setSelected(false);
		CheckMenuItem mDebugMode = new CheckMenuItem("Debug Mode");
		mDebugMode.setSelected(false);
		mDebugMode.setOnAction(e -> updateDebugMode());
		mControl.getItems().addAll(mFast, mNormal, mSlow,mDebugMode);
		mb.getMenus().addAll(mAbout,mControl);
		vb = new VBox(mb,root);
	}
	
	private static void updateDebugMode() {
		debugMode = !debugMode;
		for (int i = 0; i < memoryTF.length; i++) {
			memoryTF[i].setEditable(debugMode);
		}
		tfProgramCounter.setEditable(debugMode);
		tfReg.setEditable(debugMode);
		consoleText.setText("Debug Mode is "+((debugMode)?"Enabled":"Disabled")+"\n");
	}

	private static void updateRunDelay(ActionEvent e) {
		mFast.setSelected(false);
		mNormal.setSelected(false);
		mSlow.setSelected(false);
		consoleText.clear();
		CheckMenuItem selected = ((CheckMenuItem) e.getSource());
		String selection = selected.getText();
		if (selection.contains("100mS")) { 
			runDelay = 100;
			mFast.setSelected(true);
			consoleText.setText("Run delay time between steps set to 100mS\n");
		} else if (selection.contains("250mS")) { 
			runDelay = 250;
			mNormal.setSelected(true);
			consoleText.setText("Run delay time between steps set to 250mS\n");
		} else if (selection.contains("500mS")) {
			runDelay = 500;
			mSlow.setSelected(true);
			consoleText.setText("Run delay time between steps set to 500mS\b");
		}
		
	}
	
	private static void initializeHashMap() {
		pnuemonicsMap.put("HLT", 0);
		pnuemonicsMap.put("ADD", 1);
		pnuemonicsMap.put("SUB", 2);
		pnuemonicsMap.put("STA", 3);
		pnuemonicsMap.put("LDA", 5);
		pnuemonicsMap.put("BRA", 6);
		pnuemonicsMap.put("BRZ", 7);
		pnuemonicsMap.put("BRP", 8);
		pnuemonicsMap.put("INP", 9);
		pnuemonicsMap.put("OUT", 9);
	}

	private static void initEmulatorBox() {
		emulatorBox = new HBox();
		emulatorBox.setAlignment(Pos.CENTER);
		emulatorBox.setSpacing(15);
		Button printInst = new Button("Print instruction set");
		printInst.setOnAction(e -> printInstructionSet());
		Button dumpMem = new Button("Dump memory...");
		dumpMem.setOnAction(e -> dumpMemory());
		Button clearLog = new Button("Clear logs");
		clearLog.setOnAction(e -> clearLogs());
		emulatorBox.getChildren().addAll(printInst, dumpMem, clearLog);
		printInstructionSet();
		root.add(emulatorBox, 2, 1);
	}

	private static void initExecutionBox() {
		executionBox = new HBox();
		executionBox.setAlignment(Pos.CENTER);
		executionBox.setSpacing(15);
		Button reset = new Button("Reset");
		reset.setOnAction(e -> resetLMC(true));
		Button step = new Button("Step");
		step.setOnAction(e -> stepLMC());
		Button run = new Button("Run");
		run.setOnAction(e -> runLMC());
		Label lblPC = new Label("PC:");
		tfProgramCounter = new TextField("" + programCounter);
		tfProgramCounter.setPrefWidth(40);
		tfProgramCounter.setEditable(false);
		tfProgramCounter.setAlignment(Pos.CENTER_RIGHT);
		Label lblReg = new Label("Register:");
		tfReg = new TextField("" + register);
		tfReg.setEditable(false);
		tfReg.setPrefWidth(40);
		tfReg.setAlignment(Pos.CENTER_RIGHT);
		ckOverFlow = new CheckBox();
		ckOverFlow.setText("<<overflow");
		ckOverFlow.setSelected(overflow);
		executionBox.getChildren().addAll(reset, step, run, lblPC, tfProgramCounter, lblReg, tfReg, ckOverFlow);
		root.add(executionBox, 1, 1);
	}

	private static void resetState() {
		programCounter = 0;
		prevPC = 0;
		register = 0;
		overflow = false;
		done = false;
		prevMemLoc = -1;
		tfProgramCounter.setText("" + programCounter);
		tfReg.setText("" + register);
		ckOverFlow.setSelected(overflow);
		consoleText.setText(consoleText.getText() + "\nLMC PC, Register and Overflow have been reset");
		updateMemoryBorder(programCounter, GREEN_BORDER);	
	}
	
	private static void resetLMC(boolean display) {
		clearLogs();
		clearMemory();
		if (instructionText.getText().isEmpty())
			consoleText.setText(consoleText.getText() + "\nLMC Memory has been cleared");
		else 
			compileAndLoadMemory();
		
	}

	private static void getInputFromUser() {
		int result = -1;
		do {
			TextInputDialog td = new TextInputDialog();
			td.setTitle("Input");
			td.setHeaderText(null);
			td.setContentText("Enter a number between 0 and 999\n");
			td.showAndWait();
			String response = td.getEditor().getText();
			if (response != null) {
				result = convertTokenToInt(response);
			}
			if (result < 0 || result > 999) {
				result = -1;
				Alert error = new Alert(AlertType.ERROR);
				error.setHeaderText("Error");
				error.setContentText(response + " is not a number between 0 - 999!");
				error.showAndWait();
			}

		} while (result == -1);
		register = result;
	}

	private static void outputRegToUser() {
		Alert out = new Alert(AlertType.INFORMATION);
		out.setTitle("Output");
		out.setHeaderText(null);
		out.setContentText("Register = " + register);
		out.showAndWait();
	}

	private static void endProgramAlert() {
		Alert out = new Alert(AlertType.INFORMATION);
		out.setTitle("End Program");
		out.setHeaderText(null);
		out.setContentText("Executed HLT Instruction!");
		out.showAndWait();
	}

	private static void execute_HLT() {
		if (!done) {
			consoleText.setText(consoleText.getText() + "\nLMC: HLT - Program Ended");
			endProgramAlert();
//			System.out.println("LMC: HLT - Program Ended");
			done = true;
		}
	}

	private static void execute_INP() {
		getInputFromUser();
		String msg = consoleText.getText();
		if (!msg.isEmpty())
			msg += "\n";
		msg += "LMC: INP (" + (register) + ")";
//		System.out.println("LMC: INP (" + (register) + ")");
		consoleText.setText(msg);
		overflow = false; // clear the overflow flag on INP - prior value makes no sense...
		programCounter++;
	}

	private static void execute_OUT() {
		outputRegToUser();
		String msg = consoleText.getText();
		if (!msg.isEmpty())
			msg += "\n";
		msg += "LMC: OUT (" + register + ")";
//		System.out.println("LMC: OUT (" + (register) + ")");
		consoleText.setText(msg);
		programCounter++;
	}

	private static void execute_ADD(int memLoc) {
		String msg = consoleText.getText();
		if (!msg.isEmpty())
			msg += "\n";
		msg += "LMC: ADD #" + String.format("%02d", memLoc) + " (" + memory[memLoc] + ")";
//		System.out.println("LMC: ADD #" + String.format("%02d", memLoc) + " (" + memory[memLoc] + ")");
		consoleText.setText(msg);
		register += memory[memLoc];
		overflow = register > 999;
		if (overflow)
			register -= 1000;
		programCounter++;
	}

	private static void execute_SUB(int memLoc) {
		String msg = consoleText.getText();
		if (!msg.isEmpty())
			msg += "\n";
		msg += "LMC: SUB #" + String.format("%02d", memLoc) + " (" + memory[memLoc] + ")";
//		System.out.println("LMC: SUB #" + String.format("%02d", memLoc) + " (" + memory[memLoc] + ")");
		consoleText.setText(msg);
		register -= memory[memLoc];
		overflow = register < 0;
		if (overflow)
			register += 1000;
		programCounter++;
	}

	private static void execute_STA(int memLoc) {
		String msg = consoleText.getText();
		if (!msg.isEmpty())
			msg += "\n";
		msg += "LMC: STA " + memLoc + " (storing " + register + " into #" + String.format("%02d", memLoc) + ")";
//		System.out.println("LMC: STA " + memLoc + " (storing " + register + " into #" + String.format("%02d", memLoc) + ")");
		consoleText.setText(msg);
		memory[memLoc] = register;
		memoryTF[memLoc].setText("" + register);
		programCounter++;
	}

	private static void execute_LDA(int memLoc) {
		String msg = consoleText.getText();
		if (!msg.isEmpty())
			msg += "\n";
		msg += "LMC: LDA " + memLoc + " (loading " + memory[memLoc] + " from #" + String.format("%02d", memLoc) + ")";
//		System.out.println("LMC: LDA " + memLoc + " (loading " + memory[memLoc] + " from #" + String.format("%02d", memLoc) + ")");
		consoleText.setText(msg);
		register = memory[memLoc];
		overflow = false; // clear the overflow flag on a load (prior value makes no sense)
		programCounter++;
	}

	private static void execute_BRA(int memLoc) {
		String msg = consoleText.getText();
		if (!msg.isEmpty())
			msg += "\n";
		msg += "LMC: BRA " + memLoc + " (branch to #" + String.format("%02d", memLoc) + ")";
//		System.out.println("LMC: BRA " + memLoc + " (branch to #" + String.format("%02d", memLoc) + ")");
		consoleText.setText(msg);
		programCounter = memLoc;
	}

	private static void execute_BRZ(int memLoc) {
		String msg = consoleText.getText();
		if (!msg.isEmpty())
			msg += "\n";
		msg += "LMC: BRZ " + memLoc + " (branch to #" + String.format("%02d", memLoc) + " if zero)";
//		System.out.println("LMC: BRZ " + memLoc + " (branch to #" + String.format("%02d", memLoc) + " if zero)");
		consoleText.setText(msg);
		if (register == 0 && !overflow) // CHG: only branch if register truly is zero - no overflow
			programCounter = memLoc;
		else
			programCounter++;
	}

	private static void execute_BRP(int memLoc) {
		String msg = consoleText.getText();
		if (!msg.isEmpty())
			msg += "\n";
		msg += "LMC: BRP " + memLoc + " (branch to #" + String.format("%02d", memLoc) + " if positive)";
//		System.out.println("LMC: BRP " + memLoc + " (branch to #" + String.format("%02d", memLoc) + " if positive)");
		consoleText.setText(msg);
		if (!overflow && register > 0)
			programCounter = memLoc;
		else
			programCounter++;
	}

	private static void executeInstruction(int opcode, int memLoc) {
		switch (opcode) {
		case 0 -> execute_HLT();
		case 9 -> {
			if (memLoc == 1)
				execute_INP();
			else if (memLoc == 2)
				execute_OUT();
		}
		case 1 -> execute_ADD(memLoc);
		case 2 -> execute_SUB(memLoc);
		case 3 -> execute_STA(memLoc);
		case 5 -> execute_LDA(memLoc);
		case 6 -> execute_BRA(memLoc);
		case 7 -> execute_BRZ(memLoc);
		case 8 -> execute_BRP(memLoc);
		}
	}

	private static void stepLMC() {
		active = true;
		updateMemoryBorder(prevPC, BLUE_BORDER);
		updateMemoryBorder(programCounter, RED_BORDER);
		prevPC = programCounter;
		int instr = memory[programCounter];
		int opcode = instr / 100;
		int memLoc = instr % 100;
		if (prevMemLoc != -1 && prevMemLoc != programCounter) {
			updateMemoryBorder(prevMemLoc, BLUE_BORDER);
			prevMemLoc = -1;
		}
		if (opcode != 0 && opcode != 9) {
			updateMemoryBorder(memLoc, CYAN_BORDER);
			prevMemLoc = memLoc;
		}
		executeInstruction(opcode, memLoc);
		tfProgramCounter.setText("" + programCounter);
		tfReg.setText("" + register);
		ckOverFlow.setSelected(overflow);
		active = false;
	}

	private static void runLMC() {

		new Thread(() -> {
			do {
				try {
					Thread.sleep(runDelay);
				} catch (InterruptedException e) {

				}
				Platform.runLater(() -> {
					if (!active)
						stepLMC();
				});
			} while (!done);
		}).start();
	}


	private static void printInstructionSet() {
		String instSet = "";
		instSet += "  0  [HLT]    halts execution\n";
		instSet += "1xx  [ADD xx] adds value at 'xx' to register\n";
		instSet += "2xx  [SUB xx] subtracts value at 'xx' from register\n";
		instSet += "3xx  [STA xx] stores register value in memory @ 'xx'\n";
		instSet += "5xx  [LDA xx] loads register with value in memory @ 'xx'\n";
		instSet += "6xx  [BRA xx] branches to 'xx;\n";
		instSet += "7xx  [BRZ xx] branches to 'xx' if register value equals 0\n";
		instSet += "8xx  [BRP xx] branches to 'xx' if register value is not equal to 0\n";
		instSet += "              AND overflow flag was not set\n";
		instSet += "901  [INP]    loads input from user into register\n";
		instSet += "902  [OUT]    displays register value to user\n";
		consoleText.setText(instSet);

	}

	private static void dumpMemory() {
		String memDump = "";
		for (int i = 0; i < memory.length; i++) {
			if (memory[i] != 0) 
				memDump += String.format("@%02d %d\n",i,memory[i]);
		}
		consoleText.setText(memDump);
	}

	private static void clearLogs() {
		consoleText.setText("");
	}

	private static String extractDir(String path) {
		String repl;
		if (isWindows) {
			repl = path.replaceAll("[\\\\][^\\\\]*$", "");
		} else {
			repl = path.replaceAll("/[^/]*$", "");
		}
		return repl;
	}

	private static String readImportFile(File inFile) {
		String code = "";
		String line;
		boolean first = true;
		if (inFile.exists() && inFile.isFile() && inFile.length() > 0) {
			try (BufferedReader br = new BufferedReader(new FileReader(inFile))) {
				while ((line = br.readLine()) != null) {
					if (!first)
						code += "\n";
					first = false;
					code += line;
				}
				br.close();
			} catch (Exception e) {
				System.out.println("Exception occurred while attempting to read file");
				consoleText
						.setText(consoleText.getText() + "\nERROR: Exception occurred while attempting to read file");
				code = "";
			}
		}
		return code;
	}

	private static String selectAndImportFile() {
		String code = "";
		if (!dirPath.isEmpty())
			fileChooser.setInitialDirectory(new File(dirPath));
		if (!fileName.isEmpty()) {
			fileChooser.setInitialFileName(fileName);
		}
		File temp = fileChooser.showOpenDialog(pStage);
		if (temp != null && temp.getPath() != null) {
			code = readImportFile(temp);
			if (!code.isEmpty()) {
				dirPath = extractDir(temp.getPath());
				fileName = temp.getName();
			}
		}
		return code;
	}

	private static void initInstructionBox() {
		instructionBox = new HBox();
		instructionBox.setAlignment(Pos.CENTER);
		instructionBox.setSpacing(15);
		compAndLoad = new Button("Compile/Load Memory");
		compAndLoad.setPrefWidth(150);
		compAndLoad.setAlignment(Pos.CENTER);
		compAndLoad.setOnAction(e -> compileAndLoadMemory());
		fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("lmc Files", "*.lmc"));
		importFile = new Button("Import...");
		importFile.setOnAction(e -> instructionText.setText(selectAndImportFile()));
		instructionBox.getChildren().addAll(compAndLoad, importFile);

		root.add(instructionBox, 0, 1);
		GridPane.setHalignment(instructionBox, HPos.CENTER);
	}

	private static void initEmulationPane() {
		emulationPane = new BorderPane();
		emulationPane.setPrefWidth(600);
		Label lblEmulator = new Label("Emulator Console:");
		emulationPane.setTop(lblEmulator);
		consoleText = new TextArea();
		consoleText.setEditable(false);
		consoleText.setStyle(RED_FONT);
		consoleText.setFont(Font.font("Courier New", FontWeight.BOLD, 14));
		consoleText.setPrefColumnCount(120);
		consoleText.setPrefRowCount(1000);
		ScrollPane emulSP = new ScrollPane();
		emulSP.setPrefHeight(300);
		emulSP.setContent(consoleText);
		emulationPane.setCenter(emulSP);
		root.add(emulationPane, 2, 0);
	}

	private static void initInstructionPane() {
		instructionPane = new BorderPane();
		instructionPane.setPrefWidth(400);
		Label lblInst = new Label("Instructions:");
		instructionText = new TextArea();
		instructionText.setPrefHeight(300);
		instructionPane.setTop(lblInst);
		ScrollPane instSP = new ScrollPane();
		instSP.setPrefHeight(300);
		instSP.setContent(instructionText);
		instructionPane.setCenter(instSP);
		root.add(instructionPane, 0, 0);
	}

	private static void updateMemoryBorder(int location, String border) {
		memoryTF[location].setStyle(border);
	}

	private static void initMemoryPane() {
		memoryPane = new GridPane();
		memoryPane.setHgap(5);
		memoryPane.setVgap(5);

		Label[] hLbl = new Label[10];
		Label[] vLbl = new Label[10];

		memoryTF = new TextField[100];
		for (int i = 0; i < memoryTF.length; i++) {
			memoryTF[i] = new TextField("" + memory[i]);
			memoryTF[i].setAlignment(Pos.CENTER_RIGHT);
			memoryTF[i].setEditable(false);
			memoryTF[i].setPrefWidth(50);
			updateMemoryBorder(i, (i == programCounter) ? GREEN_BORDER : BLUE_BORDER);
			memoryTF[i].setOnAction(e -> memoryTFChanged(e));
			memoryPane.add(memoryTF[i], (i % 10) + 1, (i / 10) + 1);
			if (i < 10) {
				hLbl[i] = new Label("" + i);
				hLbl[i].setFont(hdrFont);
				memoryPane.add(hLbl[i], i + 1, 0);
				memoryPane.setHalignment(hLbl[i], HPos.CENTER);
				memoryPane.setValignment(hLbl[i], VPos.BOTTOM);
				vLbl[i] = new Label("" + i * 10);
				vLbl[i].setFont(hdrFont);
				vLbl[i].setPrefWidth(25);
				vLbl[i].setAlignment(Pos.CENTER_RIGHT);
				memoryPane.add(vLbl[i], 0, i + 1);
			}
		}
		root.add(memoryPane, 1, 0);
	}

	private static void memoryTFChanged(ActionEvent e) {
		TextField src = (TextField) e.getSource();
		int i = 0;
		while (i < memoryTF.length) {
			if (memoryTF[i] == src)
				break;
			i++;
		}

		try {
			int value = Integer.parseInt(src.getText());
			if ((value < 0) || (value > 999))
				throw new NumberFormatException("Integer value (" + value + ") outside of allowed range: 0 - 999");
			updateMemoryLocation(i, value, true);

		} catch (NumberFormatException ex) {
			System.out.println("Exception when updating memory[" + i + "]: " + ex.getMessage());
			System.out.println("Restoring original value of memory[" + i + "] to " + memory[i]);
			int value = memory[i];
			memoryTF[i].setText("" + value);
		}
	}

	private static void updateMemoryLocation(int location, int value, boolean print) {
		memory[location] = value;
		if (print) {
			System.out.println("Updated memory[" + location + "] to " + value);
			consoleText.setText("Updated memory[" + location + "] to " + value);
		}
	}

	private static void clearMemory() {
		for (int i = 0; i < memory.length; i++) {
			memoryTF[i].setText("0");
			updateMemoryBorder(i,BLUE_BORDER);
			updateMemoryLocation(i, 0, false);
		}
	}

	private static void compileAndLoadMemory() {
//		resetLMC(false);
		clearLogs();
		clearMemory();
		consoleText.setText("Compiling source...");
		String text = instructionText.getText();
		boolean status = false;
		if (text.isEmpty()) {
			System.out.println("Nothing to compile!");
			consoleText.setText(consoleText.getText() + "\nNothing to compile!");
			status = false;

		} else {
			status = compile(text.split("\n"));
		}
		if (status) {
			System.out.println("Loading compiled code into memory");
			consoleText.setText(consoleText.getText() + "\nLoading compiled code into memory");
			for (int i = 0; i < memory.length; i++)
				updateMemoryLocation(i, Integer.parseInt(memoryTF[i].getText()), false);
		} else {
			System.out.println("Clearing memory");
			consoleText.setText(consoleText.getText() + "\nClearing memory");
			clearMemory();
		}
		resetState();
	}

	private static boolean compile(String[] code) {
		boolean status = true;
		ArrayList<String> source = new ArrayList<String>();
		for (String line : code) {
			source.add(line.replaceAll("//.*", "").replaceAll("^\\s+", "")); // remove leading spaces
		}
		labelMap.clear();
		// pass 1: process labels
		if (!assignLabelMemoryLocations(source))
			return false;
		if (!compileSourceCode(source))
			return false;

		return true;
	}

	private static int convertTokenToInt(String input) {
		int value;
		try {
			value = Integer.parseInt(input);
		} catch (NumberFormatException e) {
			value = -1;
		}
		return value;
	}

	private static int getRegisterAddress(String[] tokens, int index) {
		int regAddr = -1;
		if (index < tokens.length) {
			if (labelMap.containsKey(tokens[index]))
				regAddr = labelMap.get(tokens[index]);
			else
				regAddr = getMemoryLocation(tokens[index]);
		}
		return regAddr;
	}

	private static int compileTokens(String[] tokens, int index) {
		int value = 0;

		if (index < tokens.length) {
			value = convertTokenToInt(tokens[index]);
			if (value == -1) {
				String token = tokens[index];
				index++;
				if (pnuemonics.contains(token)) {
					if (token.equals("HLT"))
						value = pnuemonicsMap.get(token);
					else {
						value = pnuemonicsMap.get(token) * 100;
						if (value < 900) {
							int regAddr = getRegisterAddress(tokens, index);
							if (regAddr == -1)
								value = regAddr;
							else
								value += regAddr;
						} else {
							value += (token.equals("INP")) ? 1 : 2;
						}
					}
				}
			}
		}
		return value;
	}

	private static boolean compileSourceCode(ArrayList<String> source) {
		programCounter = 0;
		int lineNum = 0;
		for (String line : source) {
			lineNum++;
			if (line.matches("^\\s*$"))
				continue;
			String[] tokens = line.split("\\s+");
			int index = 0;
			int value = 0;
			int memAddr = programCounter;
			if (tokens[index].startsWith("@")) { // handle memory location
				memAddr = getMemoryLocation(tokens[index].substring(1));
				index++;
			}
			if (tokens[index].contains(":")) { // skip any labels -- already processed
				index++;
			}
			value = compileTokens(tokens, index);
			// try converting value as an integer... if it works, then done
			if (value == -1) {
				return outputCompileResults(line, "Unable to compile tokens", lineNum);
			}
			memoryTF[memAddr].setText("" + value);

			if (memAddr == programCounter)
				programCounter++;
		}
		if (programCounter == 0)
			return outputCompileResults(null, "No code found to compile!", -1);
		return outputCompileResults(null, "Compilation successful", lineNum);
	}

	private static boolean outputCompileResults(String line, String msg, int lineNum) {
		if ("Compilation successful".equals(msg)) {
			System.out.println(msg);
			consoleText.setText(consoleText.getText() + "\n" + msg);
			return true;
		} else if (lineNum == -1) {
			System.out.println(msg);
			consoleText.setText(consoleText.getText() + "\n" + msg);
			return false;
		} else {
			System.out.println("Compilation failed at line " + lineNum + ": \'" + line + "\'\nError: " + msg);
			consoleText.setText(consoleText.getText() + "\nCompilation failed at line " + lineNum + ": \'" + line
					+ "\'\nError: " + msg);
			return false;
		}
	}

	private static boolean assignLabelMemoryLocations(ArrayList<String> source) {
		programCounter = 0;
		int lineNum = 0;
		for (String line : source) {
			lineNum++;
			if (line.matches("^\\s*$"))
				continue; // skip any blank lines
			String[] tokens = line.split("\\s+");
			int memLoc = programCounter;
			int index = 0;
			if (tokens[index].startsWith("@")) {
				memLoc = getMemoryLocation(tokens[index].substring(1));
				if (memLoc == -1) {
					return outputCompileResults(line, "Invalid memory location (" + tokens[index] + ") specified",
							lineNum);
				}
				index++;
			} else {
				memLoc = programCounter;
			}
			if (tokens[index].contains(":")) {
				if (tokens[index].matches("^[a-z][a-z0-9]*:")) {
					String label = tokens[index].substring(0, tokens[index].length() - 1);
					if (labelMap.containsKey(label)) {
						return outputCompileResults(line, "Duplicate label (\'" + label + "\') specified", lineNum);
					}
					labelMap.put(label, memLoc);
				} else {
					return outputCompileResults(line, "Invalid label format (\'" + tokens[index] + "\') specified",
							lineNum);
				}
			}
			programCounter++;
		}
		return true;
	}

	private static int getMemoryLocation(String str) {
		try {
			int memLoc = Integer.parseInt(str);
			if (memLoc < 0 || memLoc > 99)
				return -1;
			return memLoc;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public static void main(String[] args) {
		isWindows = System.getProperty("os.name").contains("Win");
		dirPath = System.getProperty("user.dir");
		fileName = "";
		launch(args);
	}
}

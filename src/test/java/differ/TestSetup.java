import java.io.PrintWriter;
import java.nio.file.Paths;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.net.URL;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.lang.Thread;

import java.util.concurrent.TimeUnit;

import soot.G;
import differ.SemanticDiffer;

public class TestSetup {

    private static final String prefix = Paths.get("").toAbsolutePath().toString();
    private static final String resources = prefix+"/Mvntest/resources";
    private static final String output = resources +"/adapterOutput"; //for final output            
    private static final String renamedDir = resources + "/renamedOriginals"; //for first setup patch adapter output                                                                                       
    private static final String normalCp = prefix + "/target/classes/";
    private static final String redefDirBase = prefix +"/src/main/java/testexamplespatch/"; //for patch classes 
    private static final String redefDirSuffix = "/patch";
    private static String redefDir;
    
    public static void makeListFiles(String[] classes) throws Throwable {
	String mainClass = classes[0];
	File origList = new File(prefix + "/" + mainClass + ".originalclasses.out");
	PrintWriter writer = new PrintWriter(origList);
	for (String c: classes) {
	    writer.println(c);
	}
	writer.close();
    }
    
    public static String[] setup(String mainClass, String redefDirSpecificPath, boolean useMainClass) throws Throwable {
	//setup stuff

	redefDir = redefDirBase + redefDirSpecificPath + redefDirSuffix;
	
	File adapterOut = new File(output);
	adapterOut.mkdirs();
	File renameOut = new File(renamedDir);
        renameOut.mkdirs();
	
	System.out.println("java.home = " + System.getProperty("java.home"));
	String jce = System.getProperty("java.home") + "/lib/jce.jar";
	String rt = System.getProperty("java.home") + "/lib/rt.jar";
	String cp = normalCp + ":" + redefDir + ":" + rt + ":" + jce;

	String[] differArgs = { "-cp", cp, "-w", "-renameDestination", renamedDir, "-finalDestination", output, "-redefcp", redefDir, "-runRename", "true", "-useFullDir", "false", "-mainClass", mainClass, "-originalclasslist", prefix + "/" + mainClass + ".originalclasses.out", "Example"
};
	if(!useMainClass){
	    differArgs[12] = "true";
	}
	
	System.out.println("Command Line: " + Arrays.toString(differArgs));
	return differArgs;
	
    }

    public static void testSetupRefresh(String packagePrefix){
	//first the prev run adapter outputs 
	deleteFiles(output+ "/" +packagePrefix);
	//then the renamed temp outputs
	deleteFiles(renamedDir+ "/" +packagePrefix);
	//reset Soot, aka the Scene and loaded classes
	G.reset();
	try {
	    TimeUnit.SECONDS.sleep(5); //possibly not reset if next run starts at exact same second at prev
	} catch(Exception e) {
	    System.out.println("Could not delay refresh: " + e.getMessage());
	}
    }

    private static void deleteFiles(String path){
	//clears the temp directories used in prev test setups   
	for (File child : (new File(path)).listFiles()){
            System.out.println("Cleaning up: "+ child);
            try{
                Files.deleteIfExists(child.toPath());
            } catch(Exception e) {
                System.out.println("Error deleting file: "+ e.getMessage());
            }
        }
    }

    public static void runAdapter(List<String> classes, String[] differArgs){
	SemanticDiffer.setClasses(classes);
	try{
	    SemanticDiffer.main(differArgs);
	}catch(Exception e){
	    System.out.println("Patch Adapter Failure: " + e.getMessage());
	    e.printStackTrace(System.out);
	}
    }


    //https://github.com/Sable/soot/blob/master/src/test/java/soot/jimple/MethodHandleTest.java#L134
    public static Class<?> validateClassFile(String className) throws MalformedURLException, ClassNotFoundException {
	// Make sure the classfile is actually valid...
	URL adapterOut = new File(output).toURI().toURL();
	URL leftoverNotChangedPatchClasses = new File(redefDir).toURI().toURL();
	URLClassLoader classLoader = new URLClassLoader(new URL[] {  adapterOut, leftoverNotChangedPatchClasses }, null);	
	return classLoader.loadClass(className);
    }

}

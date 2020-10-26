import java.io.PrintWriter;
import java.nio.file.Paths;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.net.URL;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import java.lang.Thread;

import differ.SemanticDiffer;

public class TestSetup {

    private static final String prefix = Paths.get("").toAbsolutePath().toString();
    private static final String resources = prefix+"/Mvntest/resources";
    private static final String output = resources +"/adapterOutput"; //for final output            
    private static final String renamedDir = resources + "/renamedOriginals"; //for first setup patch adapter output                                                                                       
    private static final String normalCp = prefix + "/target/classes/";
    private static final String redefDir = prefix +"/src/main/java/patch"; //for patch classes 

    public static void makeListFiles(String[] classes) throws Throwable {
	String mainClass = classes[0];
	File origList = new File(prefix + "/" + mainClass + ".originalclasses.out");
	PrintWriter writer = new PrintWriter(origList);
	for (String c: classes) {
	    writer.println(c);
	}
	writer.close();
    }
    
    public static String[] setup(String mainClass) throws Throwable {
	//setup stuff
	File adapterOut = new File(output);
	adapterOut.mkdirs();
	File renameOut = new File(renamedDir);
        renameOut.mkdirs();
	
	System.out.println("java.home = " + System.getProperty("java.home"));
	String jce = System.getProperty("java.home") + "/lib/jce.jar";
	String rt = System.getProperty("java.home") + "/lib/rt.jar";
	String cp = normalCp + ":" + redefDir + ":" + rt + ":" + jce;
	
	String[] differArgs = {"-cp", cp, "-cp", cp, "-w", "-firstDest", renamedDir, "-altDest", output, "-redefcp", redefDir, "-runRename", "true", "-mainClass", mainClass, "-originalclasslist", prefix + "/" + mainClass + ".originalclasses.out", "Example"};
	System.out.println("Command Line: " + Arrays.toString(differArgs));
	return differArgs;
	
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
	URLClassLoader classLoader = new URLClassLoader(new URL[] { new File(output).toURI().toURL() }, null);
	
	return classLoader.loadClass(className);
    }

}

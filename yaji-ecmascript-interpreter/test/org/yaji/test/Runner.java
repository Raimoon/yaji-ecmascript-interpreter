package org.yaji.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.yaji.util.OS;

import FESI.Interpreter.Evaluator;
import FESI.Util.EvaluatorAccess;
import FESI.Util.IEvaluatorAccess;

public class Runner {

    private static class Logger implements ILogger, Runnable {
        private final Writer writer;
        private LinkedBlockingQueue<CharSequence> messagesToWrite = new LinkedBlockingQueue<CharSequence>();
        private volatile boolean closeRequested;
        private Thread thread;

        private Logger(String logFile) throws IOException {
            if (logFile != null) {
                writer = new PrintWriter(new FileWriter(logFile));
            } else {
                writer = new PrintWriter(System.out);
            }
            thread = new Thread(this,"Runner Logger");
            thread.start();
        }

        public Appendable append(CharSequence csq) throws IOException {
            messagesToWrite.add(csq);
            return this; 
        }

        public Appendable append(CharSequence csq, int start, int end)
        throws IOException {
            return append(csq.subSequence(start, end));
        }

        public Appendable append(char c) throws IOException {
            return append(Character.toString(c));
        }

        public void close() {
            closeRequested = true;
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void printStackTrace(Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();
            print(sw.toString());
        }

        public void println() {
            print(OS.EOL);
        }

        public void print(String string) {
            try {
                append(string);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void println(String string) {
            print(string+OS.EOL);
        }

        public void run() {
            while (!(closeRequested && messagesToWrite.isEmpty())) {
                try {
                    CharSequence toWrite = messagesToWrite.poll(1,TimeUnit.SECONDS);
                    if (toWrite != null) {
                        writer.append(toWrite);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
        }
    }

    private interface ILogger extends Appendable{

        void close();

        void printStackTrace(Throwable e);

        void println();

        void print(String string);

        void println(String string);
        
    }
    
    private static SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    
    private final Arguments arguments;
    private ILogger logger;
    private int testCount;
    private int testPassed;
    private HashSet<String> excludes;

    private File originalDirectory;

    private int threadCount;

    private File overrideDirectory;

    private static ThreadLocal<Evaluator> currentEvaluator = new ThreadLocal<Evaluator>();
    
    private static class Arguments extends HashMap<String,String> {

        private static final long serialVersionUID = -45926745254286446L;

        public Arguments(String[] args) {
            for (String arg : args) {
                if (arg.startsWith("--")) {
                    String[] components = arg.substring(2).split("=");
                    if (components.length == 2) {
                        put(components[0],components[1]);
                    } else {
                        throw new Error("Invalid argument "+arg);
                    }
                } else {
                    throw new Error("Invalid argument "+arg);
                }
            }
        }

    }

    public Runner(Arguments arguments) throws IOException {
        this.arguments = arguments;
        String logFile = arguments.get("logFile");
        this.logger = createLogger(logFile);
        String excludesFile = arguments.get("excludes");
        this.excludes = new HashSet<String>();
        if (excludesFile != null) {
            String[] excludesFileNames = excludesFile.split(File.pathSeparator);
            for (String excludesFileName : excludesFileNames) {
                readExcludesFile(excludesFileName,this.excludes);
            }
        }
        String originalDirectoryName = arguments.get("originals");
        if (originalDirectoryName != null) {
            originalDirectory = new File(originalDirectoryName);
        } else {
            originalDirectory = null;
        }
        String overrideDirectoryName = arguments.get("overrideDir");
        if (overrideDirectoryName != null) {
            overrideDirectory = new File(overrideDirectoryName);
        } else {
            overrideDirectory = null;
        }
        String threads = arguments.get("threads");
        if (threads == null) {
            threadCount = 1;
        } else {
            try {
                threadCount = Math.max(1, Integer.parseInt(threads,10));
            } catch (NumberFormatException e) {
                threadCount = 1;
            }
        }
        EvaluatorAccess.setAccessor(new IEvaluatorAccess() {
            
            public Evaluator getEvaluator() {
                return currentEvaluator.get();
            }
        });
    }

    private void readExcludesFile(String excludesFile, final HashSet<String> excludesSet) {
        try {
            SAXParser parser = saxParserFactory.newSAXParser();
            parser.parse(new File(excludesFile), new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {
                    if ("test".equals(qName)) {
                        String id = attributes.getValue("id");
                        if (id != null) {
                            excludesSet.add(id);
                        }
                    }
                }
            });
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ILogger createLogger(final String logFile) throws IOException {
        return new Logger(logFile);
    }
    
    public void run() {
        File rootDirectory = new File(arguments.get("testDir"));
        
        runTestsInDirectory(rootDirectory);
        
        logger.close();
    }

    private static class ThreadRunner extends Thread {

        private final Queue<TestFile> tests;

        public ThreadRunner(Queue<TestFile> tests) {
            this.tests = tests;
        }
        
        @Override
        public void run() {
            TestFile test = tests.poll();
            while (test != null) {
                test.execute();
                test = tests.poll();
            }
        }
    }
    
    private void runTestsInDirectory(File rootDirectory) {
        List<TestFile> tests = getListOfTests(rootDirectory);
        
        Queue<TestFile> testQueue = new ConcurrentLinkedQueue<TestFile>(tests);
        long start = System.currentTimeMillis();
        Thread[] threads = new Thread[threadCount];
        for(int i=0; i<threads.length; i++) {
            threads[i] = new ThreadRunner(testQueue);
            threads[i].start();
        }
        waitForThreadsToExit(threads);
        long duration = System.currentTimeMillis() - start;
        System.out.println();
        System.out.println("Run completed: "+testPassed+"/"+testCount+" ("+toReadableDuration(duration)+") "+(new Date()).toString());
    }

    private void waitForThreadsToExit(Thread[] threads) {
        for(int i=0; i<threads.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private List<TestFile> getListOfTests(File rootDirectory) {
        List<TestFile> tests = new ArrayList<TestFile>();
        List<File> directoriesToProcess = new LinkedList<File>();
        directoriesToProcess.add(rootDirectory);
        do {
            File directory = directoriesToProcess.remove(0);
            File [] files = directory.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    directoriesToProcess.add(file);
                } else if (file.getName().endsWith(".js")){
                    TestFile test = new TestFile(rootDirectory,file);
                    if (!excludes.contains(test.getTestId())) {
                        tests.add(test);
                    }
                }
            }
        } while (directoriesToProcess.size() > 0);
        return tests;
    }

    private String toReadableDuration(long duration) {
        StringBuilder sb = new StringBuilder();
        toReadableDuration(sb, duration);
        return sb.toString();
    }

    private void toReadableDuration(Appendable sb, long duration) {
        Formatter formatter = new Formatter(sb);
        long seconds = duration/1000L;
        formatter.format("%d:%02d.%03d", Long.valueOf(seconds/60),Long.valueOf(seconds%60),Long.valueOf(duration%1000L));
    }

    
    private class TestFile {
        private String testName;
        private String testId;
        private boolean negative;
        private final File testFile;

        public TestFile(File rootDirectory, File testFile) {
            testName = getTestName(rootDirectory, testFile);
            if (overrideDirectory != null) {
                File overrideFile = new File(overrideDirectory,testName+".js");
                if (overrideFile.exists()) {
                    this.testFile = overrideFile;
                } else {
                    this.testFile = testFile;
                }
            } else {
                this.testFile = testFile;
            }
            testId = testName.substring(testName.lastIndexOf(File.separatorChar)+1);
            negative = isNegative(testName);
        }
        
        public String getTestId() {
            return testId;
        }
    
        public void execute() {
            long start = System.currentTimeMillis();
            try {
                boolean passed = executeTest(testFile, testName, negative);
                if (negative) {
                    passed = !passed;
                }
                logTestResult(testName, passed, negative, System.currentTimeMillis()-start);
            } catch(Throwable t) {
                t.printStackTrace();
            }
        }
    }
    
    private boolean executeTest(File file, String testName, boolean negative) throws IOException {
        Evaluator evaluator = new Evaluator();
        currentEvaluator.set(evaluator);
        evaluator.setDefaultTimeZone(TimeZone.getTimeZone("UTC"));
        boolean passed = false;
        logStart(testName);
        try {
            evaluator.evaluate(file);
            passed = true;
        } catch (Throwable e) {
            if (!negative) {
                logException(e);
            }
        } finally {
            currentEvaluator.set(null);
        }
        return passed;
    }

    private synchronized void logException(Throwable e) {
        try {
            logger.printStackTrace(e);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private synchronized void logStart(String testName) throws IOException {
        logger.append("Executing ").append(testName);
        logger.println();
    }

    private String getTestName(File rootDirectory, File file) {
        int rootLength = rootDirectory.getAbsolutePath().length();
        String testPath = file.getAbsolutePath();
        return testPath.substring(rootLength, testPath.length()-3);
    }

    private synchronized void logTestResult(String testName, boolean passed, boolean negative, long duration) {
        testCount ++;
        logger.println();
        if (passed) {
            logger.print("Result " + testName + " SUCCESS");
            testPassed ++;
            System.out.print('.');
        } else {
            logger.print("Result " + testName + " FAIL");
            System.out.print('E');
        }
        if (testCount % 80 == 0) {
            System.out.println();
        }
        if (negative) {
            logger.print(" -ve");
        }
        logger.print(" (");
        toReadableDuration(logger, duration);
        logger.println(")");
        System.out.flush();
    }
    
    private boolean isNegative(String testName) {
        File originalFile = new File(originalDirectory,testName+".js");
        boolean isNegative = false;
        if (originalFile.isFile()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(originalFile));
                String line = reader.readLine();
                do {
                    if (line.contains("@negative")) {
                        isNegative = true;
                    }
                    line = reader.readLine();
                } while (line != null && !isNegative);
                reader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Could not locate file "+originalFile.getAbsolutePath());
        }
        return isNegative;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            Runner runner = new Runner(new Arguments(args));
            runner.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

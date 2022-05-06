package de.uni_mannheim.informatik.dws.gollum.extractLinks;

import de.uni_mannheim.informatik.dws.melt.matching_base.FileUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    
    
    public static void main(String[] args) throws Exception{
        //args = new String[]{"-i", "C:\\dev\\dbkwik_two\\files\\extracted_example", "-t", "./mytmp"};
        
        Options options = new Options();

        options.addOption(Option.builder("i")
                .longOpt("input")
                .hasArgs()
                .valueSeparator(' ')
                .required()
                .desc("Folder(s) to process as input. The folder should contain tar.gz files with wiki content. Multiple directories are possible (separated with space).")
                .build());
        
        options.addOption(Option.builder("t")
                .longOpt("tmpFolder")
                .hasArg()
                .desc("Pointing to the folder for temporary files like caches, TDB storage etc. If not given, point to the systems tmp folder.")
                .build());
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar ", options);
            System.exit(1);
        }
        
        if(cmd.hasOption("help")){
            formatter.printHelp("java -jar ", options);
            System.exit(1);
        }
        
        String tmpFolderText = cmd.getOptionValue("tmpFolder");
        if(tmpFolderText != null){
            FileUtil.setUserTmpFolder(new File(tmpFolderText));
        }
        
        List<File> filesToProcess = getFiles(cmd);
        LOGGER.info("Process {} files.", filesToProcess.size());        
        
        new AllWikisLinkExtractor(filesToProcess);
    }
    
    
    private static List<File> getFiles(CommandLine cmd){
        List<File> files = new ArrayList<>();
        for(String input : cmd.getOptionValues("input")){
            File inputDir = new File(input);
            if(inputDir.isDirectory() == false){
                LOGGER.warn("input is not a directory or does not exist: {}", inputDir);
                continue;
            }
            files.addAll(Arrays.asList(inputDir.listFiles((File dir, String name) -> name.endsWith("tar.gz"))));
        }
        //always keep a fixed order.
        Collections.sort(files, (File o1, File o2) -> o1.getName().compareTo(o2.getName()));
        return files;
    }
}

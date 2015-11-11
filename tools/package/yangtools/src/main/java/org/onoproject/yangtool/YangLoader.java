package org.onoproject.yangtool;

import com.google.common.io.Files;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.onlab.util.Tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class that takes in input two paths, one to the yang file and one where
 * to generate the interface, and a yang container name, generates the sources
 * through OpenDaylight code generator plugin from yang, and modifies
 * them accordingly to the needs of the ONOS behavior system.
 */
public class YangLoader {
    /**
     * Public method to take a yang file from the input folder and generate classes
     * to the output folder.
     * @param inputFolder forlder with the yang files
     * @param completeOuputFolder folder where to put the desired classes
     * @throws IOException
     * @throws ConfigurationException
     * @throws InterruptedException
     */
    public void generateBehaviourInterface(String inputFolder,
                                           String completeOuputFolder)
            throws IOException, ConfigurationException, InterruptedException {
        File projectDir = createTemporaryProject(inputFolder);
        List<String> containerNames = findContainerName(
                                new File(projectDir.getAbsolutePath() + "/src"));
        System.out.println("Containers " + containerNames);
        generateJava(projectDir);
        //modifyClasses(containerName, projectDir, completeInterfaceOuputFolder);
        copyFiles(new File(projectDir.getAbsolutePath() + "/dst"),
                  new File(completeOuputFolder));
        //System.out.println("Sources in " + completeOuputFolder);

    }

    private List<String> findContainerName(File scrDir) {
        List<String> allContainers = new ArrayList<>();
        Arrays.asList(scrDir.listFiles()).stream().forEach(f -> {
            try {
                FileUtils.readLines(f).stream()
                        .filter(line -> line.matches("(.*)container(.*)\\{"))
                        .collect(Collectors.toList()).forEach(s -> {
                    s = s.replace("container", "");
                    s = s.replace("{", "");
                    allContainers.add(s.trim());
                });

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return allContainers;
    }

    private File createTemporaryProject(String inputFolder) throws IOException,
            ConfigurationException {
        File tempDir = Files.createTempDir();
        File scrDir = new File(tempDir, "src");
        scrDir.mkdir();
        copyFiles(new File(inputFolder), scrDir);
        createPomFile(tempDir, scrDir);
        return tempDir;

    }

    private void copyFiles(File inputFolder, File scrDir) throws IOException {
        Tools.copyDirectory(inputFolder, scrDir);
    }

    private void createPomFile(File tempDir, File scrDir) throws ConfigurationException {
        File pom = new File(tempDir, "pom.xml");
        File dstDir = new File(tempDir, "dst");
        dstDir.mkdir();
        XMLConfiguration cfg = new XMLConfiguration();
        cfg.load(getClass().getResourceAsStream("/pom-template.xml"));
        cfg.setProperty("build.plugins.plugin.executions.execution." +
                                "configuration.yangFilesRootDir", scrDir.getPath());
        cfg.setProperty("build.plugins.plugin.executions." +
                                "execution.configuration.codeGenerators." +
                                "generator.outputBaseDir", dstDir.getPath());
        cfg.save(pom);
    }

    private void generateJava(File projectDir)
            throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec("mvn generate-sources", null, projectDir);
        String s = IOUtils.toString(pr.getInputStream(), "UTF-8");
        if (pr.waitFor() == 0) {
            if (s.contains("[WARNING]")) {
                System.out.println("Sources not generated, warning log: \n" + s);
            } else {
                System.out.println("Sources generated");
            }
        } else {
            System.out.println("Sources not generated. " + s +
                                       " \nError " + pr.getInputStream().read());
        }
    }

    //parsing classes part, for now is not used.
    private void modifyClasses(String containerName, File projectDir,
                               String pathToNewInterface) throws IOException {
        String odlInterfacepath = getPathWithFileName(containerName, projectDir.getPath());
        odlInterfacepath = odlInterfacepath + "/";
        parseClass(odlInterfacepath, pathToNewInterface, containerName);
        System.out.println("ONOS behaviour interface generated " +
                                   "correctly at " + pathToNewInterface);
    }

    private String getPathWithFileName(String filename, String pathToGenerated) {
        File[] directories = new File(pathToGenerated).listFiles(File::isDirectory);
        while (directories.length != 0) {
            pathToGenerated = pathToGenerated + "/" + directories[0].getName();
            directories = new File(pathToGenerated).listFiles(File::isDirectory);
        }
        File dir = new File(pathToGenerated);
        File behaviour = (File) Arrays.asList(dir.listFiles()).stream()
                .filter(f -> f.getName().equals(filename)).toArray()[0];
        return behaviour.getParentFile().getAbsolutePath();
    }

    private void parseClass(String filePath, String pathToNewInterface,
                            String filename) throws IOException {
        InputStream fis = null;
        String newFile = "package org.onosproject.net.behaviour;\n" +
                "import org.onosproject.net.driver.HandlerBehaviour;\n";
        fis = new FileInputStream(filePath + filename);
        InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.contains("org.opendaylight.")) {
                if (line.contains("ChildOf")) {
                    newFile = newFile + "HandlerBehaviour\n";
                } else {
                    newFile = newFile + line + "\n";
                }
            }
        }
        PrintWriter out = new PrintWriter(pathToNewInterface +
                                                  filename.replace(".java", "")
                                                  + "Interface.java");
        out.print(newFile);
        out.flush();
    }


}

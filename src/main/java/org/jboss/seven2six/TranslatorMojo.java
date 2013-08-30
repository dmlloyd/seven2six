package org.jboss.seven2six;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * A plugin that translates JDK 7 classes to JDK 6.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Mojo(name = "transform", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class TranslatorMojo extends AbstractMojo {

    /**
     * The output directory where resources should be processed
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File outputDirectory;

    /**
     * File patterns to include when processing
     */
    @Parameter
    private String[] excludes;

    /**
     * File patterns to exclude when processing
     */
    @Parameter(defaultValue = "**/*.class")
    private String[] includes;

    @Parameter(defaultValue = "false", property = "seven2six.transform.skip")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Log log = getLog();
        if (skip) {
            log.info("Skipping seven2six transform");
        } else {
            final Translator translator = new Translator();
            final File[] files = getFiles();

            if (log.isDebugEnabled()) {
                final String newLine = String.format("%n\t");
                final StringBuilder sb = new StringBuilder("Transforming Files:");
                sb.append(newLine);
                for (File file : files) {
                    sb.append(file.getAbsolutePath()).append(newLine);
                }
                log.debug(sb);
            }

            translator.transformRecursive(files);
        }
    }

    private File[] getFiles() {
        final List<File> result = new ArrayList<File>();
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(outputDirectory);
        scanner.setIncludes(includes);
        scanner.setExcludes(excludes);
        scanner.scan();
        for (String filename : scanner.getIncludedFiles()) {
            // Only class files
            final File targetFile = new File(outputDirectory, filename);
            if (targetFile.exists()) {
                result.add(targetFile.getAbsoluteFile());
            }
        }
        return result.toArray(new File[result.size()]);
    }
}

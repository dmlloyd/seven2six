seven2six
=========

A bytecode transformer for turning JDK 7 class files into JDK 6 class files.  This allows you to use new JDK 7 syntax constructs, including try-with-resources, the so-called "diamond operator", string switch, etc., while still being able to run without modification on Java 6.

Note that usage of try-with-resources will not add suppressed exceptions when running on JDK 6; this will however work OK when running transformed classes on JDK 7.  Note also that this is not a "code weaver" in the sense that usage of JDK 7 API methods will cause an error at runtime when run in JDK 6, so appropriate safeguards should be in place for this situation.

Note also that presence of the Java 7 "invokedynamic" instruction will raise a conversion error.

Usage: Maven
------------
Add a snippet like this to your pom.xml:

    <build>
        <plugins>
            <plugin>
                <groupId>org.jboss.seven2six</groupId>
                <artifactId>seven2six</artifactId>
                <version>1.1.Final</version>
                <executions>
                    <execution>
                        <id>weave</id>
                        <phase>process-classes</phase>
                        <goals>
                            <goal>transform</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>weave-tests</id>
                        <phase>process-test-classes</phase>
                        <goals>
                            <goal>transform</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>${project.build.testOutputDirectory}</outputDirectory>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

_Note that a separate execution is needed if you want your test classes to be transformed._

Make sure your compiler plugin is set to compile for 1.7.  Your final output will be 1.6.

Usage: Command Line
-------------------
Execute like this:

    java -jar seven2six.jar classes/

where "classes" is the name of a directory containing class files.  The class files will be transformed in place.

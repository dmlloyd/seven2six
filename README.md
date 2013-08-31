seven2six
=========

A bytecode transformer for turning JDK 7 class files into JDK 6 class files.  This allows you to use new JDK 7 syntax constructs, including try-with-resources, the so-called "diamond operator", string switch, etc., while still being able to run without modification on Java 6.  Any place where your project is using "-source 1.6 -target 1.6" but compiling with JDK 7 can be seamlessly replaced.

Note that usage of try-with-resources will not add suppressed exceptions when running on JDK 6; this will however work OK when running transformed classes on JDK 7.  Note also that this is not a "code weaver" in the sense that usage of JDK 7 API methods will cause an error at runtime when run in JDK 6, so appropriate safeguards should be in place for this situation (the same as if you were compiling with "-target 1.6" on JDK 7).

Note also that presence of the Java 7 "invokedynamic" instruction will raise a conversion error; however, "javac" will never produce this instruction in 1.7 (though 1.8 lambdas will do so; be sure to use "-target 1.7" on 1.8).

Usage: Maven
------------
Add a snippet like this to your pom.xml:

    <build>
        <plugins>
            <plugin>
                <groupId>org.jboss.seven2six</groupId>
                <artifactId>seven2six</artifactId>
                <version>1.2.Final</version>
                <executions>
                    <!-- run after "compile", lets your artifact work on 1.6 -->
                    <execution>
                        <id>weave</id>
                        <phase>process-classes</phase>
                        <goals>
                            <goal>transform</goal>
                        </goals>
                    </execution>
                    <!-- run after "test-compile", lets you run your tests on 1.6 -->
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

    java -classpath path/to/seven2six.jar:path/to/asm-4.1.jar org.jboss.seven2six.Translator path/of/class/files/

The class files will be transformed in place.

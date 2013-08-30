/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.seven2six;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Translate JDK 7 classes to JDK 6.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Translator implements ClassFileTransformer {

    public Translator() {
    }

    /**
     * Translate all {@code .class} files in the given list of files and directories.
     *
     * @param args the file and directory names
     */
    public static void main(String[] args) {
        new Translator().transformRecursive(args);
    }

    /**
     * Translate all {@code .class} files in the given list of files and directories.
     *
     * @param names the file and directory names
     */
    public void transformRecursive(String... names) {
        final File[] files = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            files[i] = new File(names[i]);
        }
        transformRecursive(files);
    }

    /**
     * Translate all {@code .class} files in the given list of files and directories.
     *
     * @param files the files and directories
     */
    public void transformRecursive(File... files) {
        for (File file : files) {
            if (file.isDirectory()) {
                transformRecursive(file.listFiles());
            } else if (file.getName().endsWith(".class")) {
                try {
                    transform(new RandomAccessFile(file, "rw"));
                } catch (IllegalClassFormatException e) {
                    System.err.println("Failed to transform " + file + ": " + e);
                } catch (IOException e) {
                    System.err.println("Failed to transform " + file + ": " + e);
                }
            }
            // else ignore
        }
    }

    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain, final byte[] classfileBuffer) throws IllegalClassFormatException {
        ClassWriter classWriter = new ClassWriter(0);
        final ClassReader classReader = new ClassReader(classfileBuffer);
        doAccept(classWriter, classReader);
        return classWriter.toByteArray();
    }

    public byte[] transform(final InputStream input) throws IllegalClassFormatException, IOException {
        ClassWriter classWriter = new ClassWriter(0);
        final ClassReader classReader = new ClassReader(input);
        doAccept(classWriter, classReader);
        return classWriter.toByteArray();
    }

    private void doAccept(final ClassWriter classWriter, final ClassReader classReader) throws IllegalClassFormatException {
        try {
            classReader.accept(new TranslatingClassVisitor(classWriter), 0);
        } catch (RuntimeException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IllegalClassFormatException) {
                throw (IllegalClassFormatException) cause;
            }
            throw e;
        }
    }

    public void transform(final InputStream input, final OutputStream output) throws IllegalClassFormatException, IOException {
        output.write(transform(input));
    }

    public void transform(final RandomAccessFile file) throws IllegalClassFormatException, IOException {
        try {
            file.seek(0);
            final InputStream is = new FileInputStream(file.getFD());
            try {
                final byte[] bytes = transform(is);
                file.seek(0);
                file.write(bytes);
                file.setLength(bytes.length);
                is.close();
            } finally {
                safeClose(is);
            }
            file.close();
        } finally {
            safeClose(file);
        }
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable ignored) {}
    }

    private static class TranslatingClassVisitor extends ClassVisitor {

        public TranslatingClassVisitor(final ClassWriter classWriter) {
            super(Opcodes.ASM4, classWriter);
        }

        public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
            super.visit(version == Opcodes.V1_7 ? Opcodes.V1_6 : version, access, name, signature, superName, interfaces);
        }

        public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
            final MethodVisitor defaultVisitor = super.visitMethod(access, name, desc, signature, exceptions);
            return new MethodVisitor(Opcodes.ASM4, defaultVisitor) {
                public void visitInvokeDynamicInsn(final String name, final String desc, final Handle bsm, final Object... bsmArgs) {
                    throw new RuntimeException(new IllegalClassFormatException("invokedynamic is unsupported"));
                }

                public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
                    if (name.equals("addSuppressed") && owner.equals("java/lang/Throwable")) {
                        final Label start = new Label();
                        final Label end = new Label();
                        final Label eh = new Label();
                        final Label cont = new Label();
                        super.visitTryCatchBlock(start, end, eh, "java/lang/NoSuchMethodError");
                        super.visitLabel(start);
                        super.visitMethodInsn(opcode, owner, name, desc);
                        super.visitLabel(end);
                        super.visitJumpInsn(Opcodes.GOTO, cont);
                        super.visitLabel(eh);
                        super.visitInsn(Opcodes.POP);
                        super.visitLabel(cont);
                    } else {
                        super.visitMethodInsn(opcode, owner, name, desc);
                    }
                }
            };
        }
    }
}

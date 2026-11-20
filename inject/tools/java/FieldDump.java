import org.objectweb.asm.*;
import java.util.zip.*;
import java.io.*;

/** Prints "owner name descriptor" for every field of every class in a jar (MCP's field lines carry no type). */
public class FieldDump {
    public static void main(String[] a) throws Exception {
        try (ZipFile zip = new ZipFile(a[0]); PrintStream out = new PrintStream(new FileOutputStream(a[1]), false, "UTF-8")) {
            java.util.Enumeration<? extends ZipEntry> e = zip.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = e.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                ClassReader reader = new ClassReader(zip.getInputStream(entry));
                final String owner = reader.getClassName();
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
                        out.println(owner + " " + name + " " + desc);
                        return null;
                    }
                }, ClassReader.SKIP_CODE);
            }
        }
    }
}

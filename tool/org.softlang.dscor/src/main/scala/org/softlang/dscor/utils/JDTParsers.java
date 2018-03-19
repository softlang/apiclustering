package org.softlang.dscor.utils;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.io.Files;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by Johannes on 26.06.2017.
 */
public class JDTParsers {

    public static Collection<String> parse(String content, String[] classes) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);

        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setSource(content.toCharArray());
        //TODO: We put dependencies here. Don't know what to put for unit name.
        parser.setUnitName("");
        parser.setEnvironment(classes, new String[]{}, new String[]{}, true);

        // Now parse that.
        CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
        Collection<String> results = new ArrayList<>();

        compilationUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation node) {
                if (node.resolveMethodBinding() != null && !node.resolveMethodBinding().getDeclaringClass().isFromSource()) {
                    IMethodBinding mb = node.resolveMethodBinding();
                    String qn = mb.getDeclaringClass().getQualifiedName();
                    int index = qn.indexOf("<");
                    if (index == -1) results.add(qn + "." + mb.getName());
                    if (index != -1) results.add(qn.substring(0, index) + "." + mb.getName());

                }
                return true;
            }

            //            @Override
//            public boolean visit(QualifiedName node) {
//                results.add(node.getParent().getClass().toString());
//                return true;
//            }
//
//            public boolean visit(MethodInvocation node) {
//                //System.out.println(node.resolveMethodBinding());
//                return true;
//            }
        });
        return results;
    }

    public static void main(String[] args) throws IOException {

        String path = "C:\\Data\\Workspaces\\Devprof\\testpoject\\src\\testpoject\\TestClass.java";
        // This is not nice code delete and think of better solution in java... maybe delega

        File[] files = new File("C:\\Data\\Corpus\\APIClustering\\class").listFiles();
        String[] classes = new String[files.length];
        for (int i = 0; i < classes.length; i++)
            classes[i] = files[i].getPath();

        System.out.println(parse(Files.asCharSource(new File(path), Charset.forName("UTF-8")).read(), classes));
    }

}

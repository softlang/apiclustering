package org.softlang.dscor.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.VariableDeclaratorId;
import japa.parser.ast.comments.BlockComment;
import japa.parser.ast.comments.Comment;
import japa.parser.ast.comments.JavadocComment;
import japa.parser.ast.comments.LineComment;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.visitor.GenericVisitorAdapter;
import org.apache.commons.io.IOUtils;

public class JavaParsers {

    public static void main(String[] args) throws ParseException, IOException {

        Collection<String> implementation = implementation(
                new File("E:/Corpus/Custom/EMF_PL/content/org/eclipse/emf/ecore/impl/BasicEObjectImpl.java"), "UTF-8");

        Collection<String> documentation = documentation(
                new File("E:/Corpus/Custom/EMF_PL/content/org/eclipse/emf/ecore/impl/BasicEObjectImpl.java"), "UTF-8");

        System.out.println(implementation);
        System.out.println(documentation);

    }

//	public static Collection<Collection<String>> extractMethods(File file) throws ParseException, IOException {
//		return extractMethods(new FileInputStream(file));
//	}
//
//	public static Collection<Collection<String>> extractMethods(InputStream file) throws ParseException, IOException {
//		CompilationUnit cu = JavaParser.parse(file);
//
//		Collection<Collection<String>> collections = new ArrayList<>();
//
//		cu.accept(new GenericVisitorAdapter<Void, Collection<Collection<String>>>() {
//			@Override
//			public Void visit(MethodDeclaration n, Collection<Collection<String>> arg) {
//
//				if (ModifierSet.isPublic(n.getModifiers())) {
//					Collection<String> collection = new ArrayList<String>();
//					collection.add(n.getName());
//
//					n.accept(new GenericVisitorAdapter<Void, Collection<String>>() {
//						@Override
//						public Void visit(ReferenceType n, Collection<String> arg) {
//							arg.add(n.getType().toString());
//							return super.visit(n, arg);
//						}
//
//						@Override
//						public Void visit(VariableDeclaratorId n, Collection<String> arg) {
//							arg.add(n.getName());
//							return super.visit(n, arg);
//						}
//
//					}, collection);
//
//					arg.add(collection);
//				}
//				return null;
//			}
//
//		}, collections);
//
//		return collections;
//	}

    public static Collection<String> documentation(File file, String encoding) throws ParseException, IOException {
        return documentation(new FileInputStream(file), encoding);
    }

    public static Collection<String> documentation(String content, String encoding) throws ParseException, IOException {
        return documentation(IOUtils.toInputStream(content), encoding);
    }

    public static Collection<String> documentation(InputStream file, String encoding) throws ParseException, IOException {
        CompilationUnit cu;
        try {
            cu = JavaParser.parse(file, encoding, true);
        } catch (ParseException e) {
            return Collections.emptyList();
        }
        Collection<String> collection = new ArrayList<>();

        for (Comment comment : cu.getComments())
            collection.add(comment.getContent());

        return collection;
    }

    public static Collection<String> implementation(File file, String encoding) throws ParseException, IOException {
        return implementation(new FileInputStream(file), encoding);
    }

    public static Collection<String> implementation(String content, String encoding) throws ParseException, IOException {
        return implementation(IOUtils.toInputStream(content), encoding);
    }

    public static Collection<String> implementation(InputStream file, String encoding) throws ParseException, IOException {
        CompilationUnit cu;
        try {
            cu = JavaParser.parse(file, encoding, false);
        } catch (ParseException e) {
            return Collections.emptyList();
        }
        Collection<String> collection = new ArrayList<>();
        cu.accept(new GenericVisitorAdapter<Void, Collection<String>>() {
            @Override
            public Void visit(MethodDeclaration n, Collection<String> arg) {
                arg.add(n.getName());
                return super.visit(n, arg);
            }

            @Override
            public Void visit(ClassOrInterfaceDeclaration n, Collection<String> arg) {
                arg.add(n.getName());
                return super.visit(n, arg);
            }

            @Override
            public Void visit(FieldDeclaration n, Collection<String> arg) {
                arg.addAll(n.getVariables().stream().map(x -> x.getId().getName()).collect(Collectors.toList()));

                return super.visit(n, arg);
            }

            @Override
            public Void visit(Parameter n, Collection<String> arg) {
                arg.add(n.getId().getName());
                return super.visit(n, arg);
            }

        }, collection);

        return collection;
    }
}

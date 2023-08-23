package org.gradlex.plugins.analyzer;

import com.google.common.collect.ImmutableList;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import org.gradlex.plugins.analyzer.analysis.TypeReferenceWalker;
import org.gradlex.plugins.analyzer.analysis.TypeReferenceWalker.ReferenceVisitor;
import org.gradlex.plugins.analyzer.analysis.TypeReferenceWalker.ReferenceVisitorFactory;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.stream.Stream;


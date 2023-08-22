package org.gradlex.plugins.analyzer;

public class TypeNotationConverter {
    public static String toFQCN(String internalType) {
        switch (internalType) {
            case "B": return "byte";
            case "C": return "char";
            case "D": return "double";
            case "F": return "float";
            case "I": return "int";
            case "J": return "long";
            case "S": return "short";
            case "Z": return "boolean";
            case "V": return "void";
        }

        if (!internalType.startsWith("L") || !internalType.endsWith(";")) {
            throw new IllegalArgumentException("Invalid internal type notation: " + internalType);
        }
        
        return internalType.substring(1, internalType.length() - 1).replace('/', '.');
    }
}

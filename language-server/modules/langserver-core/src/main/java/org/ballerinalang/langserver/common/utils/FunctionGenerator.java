/*
 * Copyright (c) 2019, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.langserver.common.utils;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.projects.Module;
import org.ballerinalang.langserver.codeaction.CodeActionModuleId;
import org.ballerinalang.langserver.common.ImportsAcceptor;
import org.ballerinalang.langserver.commons.BallerinaCompletionContext;
import org.ballerinalang.langserver.commons.DocumentServiceContext;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.tree.BLangFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.util.Name;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.ballerinalang.langserver.common.utils.CommonUtil.getModulePrefix;

/**
 * Function generator utilities.
 */
public class FunctionGenerator {

    public static final Pattern FULLY_QUALIFIED_MODULE_ID_PATTERN = Pattern.compile("([\\w]+)\\/([\\w.]+):([\\d.]+):");

    /**
     * Returns signature of the return type.
     *
     * @param importsAcceptor imports acceptor
     * @param typeDescriptor  {@link BLangNode}
     * @param context         {@link DocumentServiceContext}
     * @return return type signature
     */
    public static String generateTypeDefinition(ImportsAcceptor importsAcceptor,
                                                TypeSymbol typeDescriptor, DocumentServiceContext context) {
        return processModuleIDsInText(importsAcceptor, typeDescriptor.signature(), context);
    }

    /**
     * Returns imports processed of the type text.
     *
     * @param importsAcceptor imports acceptor
     * @param text            generated type text
     * @param context         {@link DocumentServiceContext}
     * @return return type signature
     */
    public static String processModuleIDsInText(ImportsAcceptor importsAcceptor, String text,
                                                DocumentServiceContext context) {
        Module module = context.workspace().module(context.filePath()).orElseThrow();
        String currentOrg = module.packageInstance().descriptor().org().value();
        String currentModule = module.descriptor().name().packageName().value();
        String currentVersion = module.packageInstance().descriptor().version().value().toString();

        StringBuilder newText = new StringBuilder();
        ModuleID currentModuleID = CodeActionModuleId.from(currentOrg, currentModule, currentVersion);
        Matcher matcher = FULLY_QUALIFIED_MODULE_ID_PATTERN.matcher(text);
        int nextStart = 0;
        // Matching Fully-Qualified-Module-IDs (eg.`abc/mod1:1.0.0`)
        // Purpose is to transform `int|abc/mod1:1.0.0:Person` into `int|mod1:Person` or `int|Person`
        // base on the current module id and identifying the potential imports required.
        while (matcher.find()) {
            // Append up-to start of the match
            newText.append(text, nextStart, matcher.start(1));
            // Append module prefix(empty when in same module) and identify imports
            ModuleID moduleID = CodeActionModuleId.from(matcher.group(1), matcher.group(2), matcher.group(3));
            newText.append(getModulePrefix(importsAcceptor, currentModuleID, moduleID, context));
            // Update next-start position
            nextStart = matcher.end(3) + 1;
        }
        // Append the remaining
        if (nextStart != 0) {
            newText.append(text.substring(nextStart));
        }
        return newText.length() > 0 ? newText.toString() : text;
    }

    /**
     * Get the function arguments from the function.
     *
     * @param importsAcceptor imports accepter
     * @param parent          Parent node
     * @param context         {@link BallerinaCompletionContext}
     * @return {@link List} List of arguments
     */
    @Deprecated
    public static List<String> getFuncArguments(ImportsAcceptor importsAcceptor, BLangNode parent,
                                                BallerinaCompletionContext context) {
        List<String> list = new ArrayList<>();
        if (parent instanceof BLangInvocation) {
            BLangInvocation bLangInvocation = (BLangInvocation) parent;
            if (bLangInvocation.argExprs.isEmpty()) {
                return null;
            }
            int argCounter = 1;
            for (BLangExpression bLangExpression : bLangInvocation.argExprs) {
                // TODO: Fix
//                Set<String> argNames = CommonUtil.getAllNameEntries(compilerContext);
                Set<String> argNames = new HashSet<>();
                if (bLangExpression instanceof BLangSimpleVarRef) {
                    BLangSimpleVarRef simpleVarRef = (BLangSimpleVarRef) bLangExpression;
                    String varName = simpleVarRef.variableName.value;
                    String argType = lookupVariableReturnType(importsAcceptor, varName, parent, context);
                    list.add(argType + " " + varName);
                    argNames.add(varName);
                } else if (bLangExpression instanceof BLangInvocation) {
//                    String argType = generateTypeDefinition(importsAcceptor, bLangExpression, context);
                    String argName = CommonUtil.generateVariableName(bLangExpression, argNames);
//                    list.add(argType + " " + argName);
                    argNames.add(argName);
                } else {
//                    String argType = generateTypeDefinition(importsAcceptor, bLangExpression, context);
                    String argName = CommonUtil.generateName(argCounter++, argNames);
//                    list.add(argType + " " + argName);
                    argNames.add(argName);
                }
            }
        }
        return (!list.isEmpty()) ? list : null;
    }

    private static String lookupVariableReturnType(ImportsAcceptor importsAcceptor,
                                                   String variableName, BLangNode parent,
                                                   DocumentServiceContext context) {
        // Recursively find BLangBlockStmt to get scope-entries
        if (parent instanceof BLangBlockStmt || parent instanceof BLangFunctionBody) {
            Scope scope = parent instanceof BLangBlockStmt ? ((BLangBlockStmt) parent).scope
                    : ((BLangFunctionBody) parent).scope;
            if (scope != null) {
                for (Map.Entry<Name, Scope.ScopeEntry> entry : scope.entries.entrySet()) {
                    String key = entry.getKey().getValue();
                    BSymbol symbol = entry.getValue().symbol;
                    if (variableName.equals(key) && symbol instanceof BVarSymbol) {
//                        return generateTypeDefinition(importsAcceptor, symbol.type, context);
                        //TODO Fix this
                        return null;
                    }
                }
            }
        }

        return (parent != null && parent.parent != null)
                ? lookupVariableReturnType(importsAcceptor, variableName, parent.parent, context)
                : "any";
    }
}

/*
 * (C) Copyright 2006-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     vpasquier
 *     slacoin
 */
package org.nuxeo.ecm.automation.core.impl;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.common.function.ThrowableFunction;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.OperationChainContribution;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.2 Operation Type Implementation for a chain
 */
public class ChainTypeImpl extends AbstractOperationType {

    protected static final Method runMethod;

    static {
        try {
            runMethod = OperationChainCompiler.CompiledChainImpl.class.getMethod("invoke", OperationContext.class);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new UnsupportedOperationException("Cannot use reflection for run method", e);
        }
    }

    protected OperationChain chain;

    /**
     * Invocable methods.
     */
    protected InvokableMethod[] methods = new InvokableMethod[] { new InvokableMethod(this, runMethod) };

    /**
     * The contribution fragment name.
     */
    protected String contributingComponent;

    /**
     * The operation chain XMAP contribution
     */
    protected OperationChainContribution contribution;

    /** @since 2025.0 */
    public ChainTypeImpl(OperationChain chain, OperationChainContribution contribution, String contributingComponent) {
        this.contribution = contribution;
        this.chain = chain;
        this.contributingComponent = contributingComponent;
    }

    @Override
    public String getId() {
        return chain.getId();
    }

    @Override
    public String[] getAliases() {
        return chain.getAliases();
    }

    @Override
    public boolean isEnabled() {
        return chain.isEnabled();
    }

    @Override
    public Class<?> getType() {
        return OperationChainCompiler.CompiledChainImpl.class;
    }

    @Override
    public OperationDocumentation getDocumentation() throws OperationException {
        OperationDocumentation doc = new OperationDocumentation(chain.getId());
        doc.label = chain.getId();
        doc.requires = contribution.getRequires();
        doc.category = contribution.getCategory();
        doc.setAliases(contribution.getAliases());
        OperationChainContribution.Operation[] operations = contribution.getOps();
        doc.operations = operations;
        doc.since = contribution.getSince();
        if (doc.requires.isEmpty()) {
            doc.requires = null;
        }
        if (doc.label.isEmpty()) {
            doc.label = doc.id;
        }
        doc.description = contribution.getDescription();
        doc.params = contribution.getParams();
        // load signature
        if (operations.length != 0) {
            // Fill signature with first inputs of the first operation and
            // related outputs of last operation
            // following the proper automation path
            List<String> result = getSignature(operations);
            doc.signature = result.toArray(String[]::new);
        } else {
            doc.signature = new String[] { "void", "void" };
        }
        return doc;
    }

    /**
     * @since 5.7.2
     * @param operations operations listing that chain contains.
     * @return the chain signature.
     */
    protected List<String> getSignature(OperationChainContribution.Operation[] operations) throws OperationException {
        OperationType operationType = Framework.getService(AutomationService.class).getOperation(operations[0].getId());
        return buildSignature(operationType.getMethods(),
                ThrowableFunction.asFunction(method -> getChainOutput(method.getInputType(), operations)));
    }

    /**
     * @since 5.7.2
     */
    protected Class<?> getChainOutput(Class<?> chainInput, OperationChainContribution.Operation[] operations)
            throws OperationException {
        for (OperationChainContribution.Operation operation : operations) {
            OperationType operationType = Framework.getService(AutomationService.class).getOperation(operation.getId());
            if (operationType instanceof ChainTypeImpl) {
                chainInput = getChainOutput(chainInput, operationType.getDocumentation().getOperations());
            } else {
                chainInput = getOperationOutput(chainInput, operationType);
            }
        }
        return chainInput;
    }

    /**
     * @since 5.7.2
     */
    protected Class<?> getOperationOutput(Class<?> input, OperationType operationType) {
        InvokableMethod[] methodsMatchingInput = operationType.getMethodsMatchingInput(input);
        if (methodsMatchingInput.length == 0) {
            return input;
        }
        // Choose the top priority method
        var topMethod = Stream.of(methodsMatchingInput)
                              .max(Comparator.comparingInt(InvokableMethod::getPriority))
                              .orElseThrow(); // can not happen since we're checking the length above
        Class<?> nextInput = topMethod.getOutputType();
        // If output is void, skip this method
        if (nextInput == Void.TYPE) {
            return input;
        }
        return nextInput;
    }

    @Override
    public String getContributingComponent() {
        return contributingComponent;
    }

    @Override
    public InvokableMethod[] getMethodsMatchingInput(Class<?> in) {
        return methods;
    }

    /**
     * @since 5.7.2
     */
    @Override
    public List<InvokableMethod> getMethods() {
        return List.of(methods);
    }

    @Override
    public Object newInstance(OperationContext ctx, Map<String, Object> args) throws OperationException {
        Object input = ctx.getInput();
        Class<?> inputType = input == null ? Void.TYPE : input.getClass();
        return Framework.getService(AutomationService.class).compileChain(inputType, chain);
    }

    // public internal APIs

    public OperationChain getChain() {
        return chain;
    }

    public Map<String, Object> getChainParameters() {
        return chain.getChainParameters();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((chain == null) ? 0 : chain.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChainTypeImpl other)) {
            return false;
        }
        return Objects.equals(chain, other.chain);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", getId()).toString();
    }
}

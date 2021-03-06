/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
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
package org.jboss.hal.dmr;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.shared.DateTimeFormat;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;
import org.jboss.hal.spi.EsParam;
import org.jboss.hal.spi.EsReturn;

import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static java.util.stream.Collectors.toList;
import static org.jboss.hal.dmr.ModelDescriptionConstants.HAL_INDEX;

/**
 * Static helper methods for dealing with {@link ModelNode}s and {@link NamedNode}s. Some methods accept a path
 * parameter separated by "/" to get a deeply nested data.
 */
@JsType
public class ModelNodeHelper {

    private static final String ENCODED_SLASH = "%2F";
    private static final DateTimeFormat ISO_8601 = GWT.isScript() ? DateTimeFormat.getFormat(
            DateTimeFormat.PredefinedFormat.ISO_8601) : null;

    @JsIgnore
    public static String encodeValue(String value) {
        return value.replace("/", ENCODED_SLASH);
    }

    @JsIgnore
    public static String decodeValue(String value) {
        return value.replace(ENCODED_SLASH, "/");
    }

    /**
     * Tries to get a deeply nested model node from the specified model node. Nested paths must be separated with "/".
     *
     * @param modelNode The model node to read from
     * @param path      A path separated with "/"
     *
     * @return The nested node or an empty / undefined model node
     */
    public static ModelNode failSafeGet(ModelNode modelNode, String path) {
        ModelNode undefined = new ModelNode();

        if (Strings.emptyToNull(path) != null) {
            Iterable<String> keys = Splitter.on('/').omitEmptyStrings().trimResults().split(path);
            if (!Iterables.isEmpty(keys)) {
                ModelNode context = modelNode;
                for (String key : keys) {
                    String safeKey = decodeValue(key);
                    if (context.hasDefined(safeKey)) {
                        context = context.get(safeKey);
                    } else {
                        context = undefined;
                        break;
                    }
                }
                return context;
            }
        }

        return undefined;
    }

    /**
     * Tries to get a deeply nested boolean value from the specified model node. Nested paths must be separated with
     * "/".
     *
     * @param modelNode The model node to read from
     * @param path      A path separated with "/"
     *
     * @return the boolean value or false.
     */
    public static boolean failSafeBoolean(ModelNode modelNode, String path) {
        ModelNode attribute = failSafeGet(modelNode, path);
        return attribute.isDefined() && attribute.asBoolean();
    }

    @JsIgnore
    public static Date failSafeDate(ModelNode modelNode, String path) {
        ModelNode attribute = failSafeGet(modelNode, path);
        if (attribute.isDefined()) {
            try {
                return ISO_8601.parse(attribute.asString());
            } catch (IllegalArgumentException ignore) { }
        }
        return null;
    }

    @JsIgnore
    public static List<ModelNode> failSafeList(ModelNode modelNode, String path) {
        ModelNode result = failSafeGet(modelNode, path);
        return result.isDefined() ? result.asList() : Collections.emptyList();
    }

    @JsIgnore
    public static List<Property> failSafePropertyList(ModelNode modelNode, String path) {
        ModelNode result = failSafeGet(modelNode, path);
        return result.isDefined() ? result.asPropertyList() : Collections.emptyList();
    }

    @JsIgnore
    public static <T> T getOrDefault(ModelNode modelNode, String attribute, Supplier<T> supplier, T defaultValue) {
        T result = defaultValue;
        if (modelNode != null && modelNode.hasDefined(attribute)) {
            try {
                result = supplier.get();
            } catch (Throwable ignored) {
                result = defaultValue;
            }
        }
        return result;
    }

    @JsIgnore
    public static void storeIndex(List<ModelNode> modelNodes) {
        int index = 0;
        for (ModelNode modelNode : modelNodes) {
            modelNode.get(HAL_INDEX).set(index);
            index++;
        }
    }

    /**
     * Turns a list of properties into a list of named model nodes which contains a {@link
     * ModelDescriptionConstants#NAME} key with the properties name.
     */
    @JsIgnore
    public static List<NamedNode> asNamedNodes(List<Property> properties) {
        return properties.stream().map(NamedNode::new).collect(toList());
    }

    /**
     * Looks for the specified attribute and tries to convert it to an enum constant using
     * {@code LOWER_HYPHEN.to(UPPER_UNDERSCORE, modelNode.get(attribute).asString())}.
     */
    @JsIgnore
    public static <E extends Enum<E>> E asEnumValue(ModelNode modelNode, String attribute, Function<String, E> valueOf,
            E defaultValue) {
        if (modelNode.hasDefined(attribute)) {
            return asEnumValue(modelNode.get(attribute), valueOf, defaultValue);
        }
        return defaultValue;
    }

    @JsIgnore
    public static <E extends Enum<E>> E asEnumValue(ModelNode modelNode, Function<String, E> valueOf, E defaultValue) {
        E value = defaultValue;
        String converted = LOWER_HYPHEN.to(UPPER_UNDERSCORE, modelNode.asString());
        try {
            value = valueOf.apply(converted);
        } catch (IllegalArgumentException ignored) {
        }
        return value;
    }

    /**
     * The reverse operation to {@link #asEnumValue(ModelNode, String, Function, Enum)}.
     */
    @JsIgnore
    public static <E extends Enum<E>> String asAttributeValue(E enumValue) {
        return UPPER_UNDERSCORE.to(LOWER_HYPHEN, enumValue.name());
    }

    /** Moves an attribute to another destination. Both source and destination can be a paths. */
    public static void move(ModelNode modelNode, String source, String destination) {
        if (modelNode != null && Strings.emptyToNull(source) != null && Strings.emptyToNull(destination) != null) {
            ModelNode value = null;
            ModelNode context = modelNode;
            List<String> sourceNames = Splitter.on('/')
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(source);
            if (!sourceNames.isEmpty()) {
                for (Iterator<String> iterator = sourceNames.iterator(); iterator.hasNext(); ) {
                    String name = iterator.next();
                    String safeName = decodeValue(name);
                    if (context.hasDefined(safeName)) {
                        if (iterator.hasNext()) {
                            context = context.get(safeName);
                        } else {
                            value = context.remove(safeName);
                            break;
                        }
                    }
                }
            }
            if (value != null) {
                context = modelNode;
                List<String> destinationNames = Splitter.on('/')
                        .omitEmptyStrings()
                        .trimResults()
                        .splitToList(destination);
                for (Iterator<String> iterator = destinationNames.iterator(); iterator.hasNext(); ) {
                    String name = iterator.next();
                    String safeName = decodeValue(name);
                    if (iterator.hasNext()) {
                        context = context.get(safeName);
                    } else {
                        context.get(safeName).set(value);
                        break;
                    }
                }
            }
        }
    }

    private ModelNodeHelper() {
    }


    // ------------------------------------------------------ JS methods

    /**
     * Tries to get a deeply nested node array from the specified model node. Nested paths must be separated with "/".
     *
     * @param modelNode The model node to read from
     * @param path      A path separated with "/"
     *
     * @return the model nodes or an empty array
     */
    @JsMethod(name = "failSafeList")
    @EsReturn("ModelNode[]")
    public static ModelNode[] jsFailSafeList(ModelNode modelNode, String path) {
        List<ModelNode> nodes = failSafeList(modelNode, path);
        return nodes.toArray(new ModelNode[nodes.size()]);
    }

    /**
     * Tries to get a deeply nested property array from the specified model node. Nested paths must be separated with
     * "/".
     *
     * @param modelNode The model node to read from
     * @param path      A path separated with "/"
     *
     * @return the properties or an empty array
     */
    @JsMethod(name = "failSafePropertyList")
    @EsReturn("Property[]")
    public static Property[] jsFailSafePropertyList(ModelNode modelNode, String path) {
        List<Property> properties = failSafePropertyList(modelNode, path);
        return properties.toArray(new Property[properties.size()]);
    }

    /**
     * Turns an properties array into an array of names nodes.
     *
     * @param properties The properties
     *
     * @return the array of named nodes
     */
    @JsMethod(name = "asNamedNodes")
    @EsReturn("NamedNode[]")
    public static NamedNode[] jsAsNamedNodes(@EsParam("Property[]") Property[] properties) {
        return Arrays.stream(properties).map(NamedNode::new).toArray(NamedNode[]::new);
    }
}

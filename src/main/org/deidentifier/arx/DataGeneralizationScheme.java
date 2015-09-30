/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
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

package org.deidentifier.arx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class encapsulates a generalization scheme
 * @author Fabian Prasser
 */
public class DataGeneralizationScheme {
    
    /**
     * A specific generalization degree
     * @author Fabian Prasser
     *
     */
    public static enum GeneralizationDegree {
        NONE(0d),
        LOW(0.2d),
        LOW_MEDIUM(0.4d),
        MEDIUM(0.5d),
        MEDIUM_HIGH(0.6d),
        HIGH(0.8d),
        COMPLETE(1d);
        
        /** Factor*/
        private final double factor;
        
        /**
         * Constructor
         * @param factor
         */
        private GeneralizationDegree(double factor) {
            this.factor = factor;
        }
        
        /**
         * Returns the factor
         * @return
         */
        protected double getFactor() {
            return this.factor;
        }
    }

    /**
     * Creates a new data generalization scheme
     * @param data
     * @return
     */
    public static DataGeneralizationScheme create(Data data) {
        return new DataGeneralizationScheme(data, null);
    }
    /**
     * Creates a new data generalization scheme
     * @param data
     * @return
     */
    public static DataGeneralizationScheme create(Data data, GeneralizationDegree degree) {
        return new DataGeneralizationScheme(data, degree);
    }
    /** Degrees */
    private Map<String, GeneralizationDegree> degrees = new HashMap<String, GeneralizationDegree>();
    /** Levels */
    private Map<String, Integer>              levels  = new HashMap<String, Integer>();
    
    /** Degree */
    private GeneralizationDegree              degree  = null;

    /** Data */
    private final Set<String>                 attributes;
    
    /**
     * Creates a new instance
     * @param data
     * @param degree 
     */
    private DataGeneralizationScheme(Data data, GeneralizationDegree degree) {
        this.attributes = new HashSet<String>();
        for (int i=0; i<data.getHandle().getNumColumns(); i++) {
            this.attributes.add(data.getHandle().getAttributeName(i));
        }
        this.degree = degree;
    }
    
    /**
     * Defines a specific generalization degree
     * @param degree
     * @return
     */
    public DataGeneralizationScheme generalize(GeneralizationDegree degree) {
        this.degree = degree;
        return this;
    }

    /**
     * Defines a specific generalization degree
     * @param attribute
     * @param degree
     * @return
     */
    public DataGeneralizationScheme generalize(String attribute, GeneralizationDegree degree) {
        check(attribute);
        this.degrees.put(attribute, degree);
        return this;
    }
    

    /**
     * Defines a specific generalization level
     * @param attribute
     * @param level
     * @return
     */
    public DataGeneralizationScheme generalize(String attribute, int level) {
        check(attribute);
        if (level < 0) {
            throw new IllegalArgumentException("Invalid generalization level: " + level);
        }
        this.levels.put(attribute, level);
        return this;
    }

    /**
     * Returns a generalization level as defined by this class
     * @param attribute
     * @param definition
     * @return
     */
    public int getGeneralizationLevel(String attribute, DataDefinition definition) {

        int result = 0;
        if (definition.isHierarchyAvailable(attribute)) {
            if (this.levels.containsKey(attribute)) {
                result = this.levels.get(attribute);
            } else if (this.degrees.containsKey(attribute)) {
                result = (int) Math.round(  (this.degrees.get(attribute).getFactor() * 
                                            (double) definition.getMaximumGeneralization(attribute)));
            } else if (this.degree != null) {
                result = (int) Math.round(this.degree.getFactor() *
                                          (double) definition.getMaximumGeneralization(attribute));
            }
        }
        result = Math.max(result, definition.getMinimumGeneralization(attribute));
        result = Math.min(result, definition.getMaximumGeneralization(attribute));
        return result;
    }
    
    /**
     * Checks the given attribute
     * @param attribute
     */
    private void check(String attribute) {
        if (!attributes.contains(attribute)) {
            throw new IllegalArgumentException("Unknown attribute: " + attribute);
        }
    }
}